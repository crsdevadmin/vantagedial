package com.vantage.dialer.api.agent;

import com.vantage.dialer.api.dto.AgentProvisionRequest;
import com.vantage.dialer.api.persistence.model.AgentEntity;
import com.vantage.dialer.api.persistence.repository.AgentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentStoreTest {

    @Test
    void initSeedsOnlyMissingDefaultAgents() {
        AgentRepository agentRepository = mock(AgentRepository.class);
        AgentStore store = new AgentStore(agentRepository);

        when(agentRepository.existsById("A1")).thenReturn(false);
        when(agentRepository.existsById("A2")).thenReturn(true);
        when(agentRepository.existsById("A3")).thenReturn(false);
        when(agentRepository.save(any(AgentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        store.init();

        ArgumentCaptor<AgentEntity> captor = ArgumentCaptor.forClass(AgentEntity.class);
        verify(agentRepository, times(2)).save(captor.capture());

        List<AgentEntity> saved = captor.getAllValues();
        assertEquals(List.of("A1", "A3"), saved.stream().map(AgentEntity::getAgentId).toList());
        assertTrue(saved.stream().allMatch(agent -> agent.getStatus() == AgentStatus.AVAILABLE));
        assertEquals("PJSIP/1001", saved.get(0).getChannel());
        assertEquals("PJSIP/1003", saved.get(1).getChannel());
    }

    @Test
    void acquireAvailableAgentMarksFirstAvailableAgentBusy() {
        AgentRepository agentRepository = mock(AgentRepository.class);
        AgentStore store = new AgentStore(agentRepository);
        AgentEntity available = entity("A1", "Agent 1", "PJSIP/1001", "1001", "1001", "pw-1", AgentStatus.AVAILABLE);

        when(agentRepository.findByStatusOrderByAgentIdAsc(AgentStatus.AVAILABLE)).thenReturn(List.of(available));
        when(agentRepository.save(any(AgentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Agent agent = store.acquireAvailableAgent().orElseThrow();

        assertEquals("A1", agent.getAgentId());
        assertEquals(AgentStatus.BUSY, agent.getStatus());
        assertEquals(AgentStatus.BUSY, available.getStatus());
        verify(agentRepository).save(available);
    }

    @Test
    void acquireAgentOnlySucceedsForExplicitAvailableAgent() {
        AgentRepository agentRepository = mock(AgentRepository.class);
        AgentStore store = new AgentStore(agentRepository);
        AgentEntity available = entity("A4", "Agent 4", "PJSIP/1004", "1004", "1004", "pw-4", AgentStatus.AVAILABLE);
        AgentEntity busy = entity("A5", "Agent 5", "PJSIP/1005", "1005", "1005", "pw-5", AgentStatus.BUSY);

        when(agentRepository.findById("A4")).thenReturn(Optional.of(available));
        when(agentRepository.findById("A5")).thenReturn(Optional.of(busy));
        when(agentRepository.save(any(AgentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Agent acquired = store.acquireAgent("A4").orElseThrow();

        assertEquals("A4", acquired.getAgentId());
        assertEquals(AgentStatus.BUSY, acquired.getStatus());
        assertTrue(store.acquireAgent("A5").isEmpty());
        verify(agentRepository).save(available);
        verify(agentRepository, never()).save(busy);
    }

    @Test
    void createOrUpdateNormalizesValuesAndAppliesDefaults() {
        AgentRepository agentRepository = mock(AgentRepository.class);
        AgentStore store = new AgentStore(agentRepository);

        when(agentRepository.existsById(" A9 ")).thenReturn(false);
        when(agentRepository.save(any(AgentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentProvisionRequest request = new AgentProvisionRequest();
        request.setAgentId(" A9 ");
        request.setAgentName(" Alice ");
        request.setExtensionNumber(" 2001 ");
        request.setSipUsername(" ");
        request.setSipPassword(" secret ");
        request.setChannel(" ");

        Agent agent = store.createOrUpdate(request);

        assertEquals("A9", agent.getAgentId());
        assertEquals("Alice", agent.getAgentName());
        assertEquals("2001", agent.getExtensionNumber());
        assertEquals("2001", agent.getSipUsername());
        assertEquals("secret", agent.getSipPassword());
        assertEquals("PJSIP/2001", agent.getChannel());
        assertEquals(AgentStatus.AVAILABLE, agent.getStatus());
    }

    @Test
    void createOrUpdateExistingAgentPreservesExistingIdAndStatus() {
        AgentRepository agentRepository = mock(AgentRepository.class);
        AgentStore store = new AgentStore(agentRepository);
        AgentEntity existing = entity("A8", "Old Name", "PJSIP/old", "1008", "1008", "old-secret", AgentStatus.BUSY);

        when(agentRepository.existsById("A8")).thenReturn(true);
        when(agentRepository.findById("A8")).thenReturn(Optional.of(existing));
        when(agentRepository.save(any(AgentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentProvisionRequest request = new AgentProvisionRequest();
        request.setAgentId("A8");
        request.setAgentName(" Updated ");
        request.setExtensionNumber(" 3008 ");
        request.setSipUsername(" ");
        request.setSipPassword(" new-secret ");
        request.setChannel(" ");

        Agent updated = store.createOrUpdate(request);

        assertEquals("A8", updated.getAgentId());
        assertEquals("Updated", updated.getAgentName());
        assertEquals("3008", updated.getExtensionNumber());
        assertEquals("3008", updated.getSipUsername());
        assertEquals("new-secret", updated.getSipPassword());
        assertEquals("PJSIP/3008", updated.getChannel());
        assertEquals(AgentStatus.BUSY, updated.getStatus());
    }

    @Test
    void releaseAgentRestoresAvailabilityWhenAgentExists() {
        AgentRepository agentRepository = mock(AgentRepository.class);
        AgentStore store = new AgentStore(agentRepository);
        AgentEntity busy = entity("A2", "Agent 2", "PJSIP/1002", "1002", "1002", "pw-2", AgentStatus.BUSY);

        when(agentRepository.findById("A2")).thenReturn(Optional.of(busy));
        when(agentRepository.save(any(AgentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        store.releaseAgent("A2");

        assertEquals(AgentStatus.AVAILABLE, busy.getStatus());
        verify(agentRepository).save(busy);
    }

    @Test
    void markBusyFindAgentAndCountAvailableUseRepositoryState() {
        AgentRepository agentRepository = mock(AgentRepository.class);
        AgentStore store = new AgentStore(agentRepository);
        AgentEntity available = entity("A6", "Agent 6", "PJSIP/1006", "1006", "1006", "pw-6", AgentStatus.AVAILABLE);

        when(agentRepository.findById("A6")).thenReturn(Optional.of(available));
        when(agentRepository.findByStatusOrderByAgentIdAsc(AgentStatus.AVAILABLE)).thenReturn(List.of());
        when(agentRepository.findAll()).thenReturn(List.of(available));
        when(agentRepository.save(any(AgentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        store.markBusy("A6");

        assertEquals(AgentStatus.BUSY, available.getStatus());
        assertEquals("A6", store.findAgent("A6").orElseThrow().getAgentId());
        assertEquals(0L, store.countAvailable());
        assertEquals(1, store.getAgents().size());
        verify(agentRepository).save(available);
    }

    private AgentEntity entity(String agentId,
                               String agentName,
                               String channel,
                               String extensionNumber,
                               String sipUsername,
                               String sipPassword,
                               AgentStatus status) {
        AgentEntity entity = new AgentEntity();
        entity.setAgentId(agentId);
        entity.setAgentName(agentName);
        entity.setChannel(channel);
        entity.setExtensionNumber(extensionNumber);
        entity.setSipUsername(sipUsername);
        entity.setSipPassword(sipPassword);
        entity.setStatus(status);
        return entity;
    }
}
