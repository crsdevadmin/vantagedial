package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.CallSessionResponse;
import com.vantage.dialer.api.persistence.model.CallSessionEntity;
import com.vantage.dialer.api.persistence.repository.CallSessionRepository;
import com.vantage.dialer.common.model.CallMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallSessionServiceTest {

    @Test
    void createQueuedSessionPersistsQueuedCallState() {
        CallSessionRepository repository = mock(CallSessionRepository.class);
        CallSessionService service = new CallSessionService(repository);

        when(repository.save(any(CallSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createQueuedSession(
                "session-1",
                "campaign-1",
                "lead-1",
                "ASTERISK",
                "+15551234567",
                "agent-1",
                "PJSIP/1001",
                CallMode.AGENT_ASSISTED,
                "ivr-1"
        );

        ArgumentCaptor<CallSessionEntity> captor = ArgumentCaptor.forClass(CallSessionEntity.class);
        verify(repository).save(captor.capture());
        CallSessionEntity saved = captor.getValue();

        assertEquals("session-1", saved.getCallSessionId());
        assertEquals("campaign-1", saved.getCampaignId());
        assertEquals("lead-1", saved.getLeadId());
        assertEquals("ASTERISK", saved.getProvider());
        assertEquals("+15551234567", saved.getCustomerNumber());
        assertEquals("agent-1", saved.getAgentId());
        assertEquals("PJSIP/1001", saved.getAgentChannel());
        assertEquals("AGENT_ASSISTED", saved.getCallMode());
        assertEquals("ivr-1", saved.getIvrFlowId());
        assertEquals("QUEUED", saved.getStatus());
        assertEquals("QUEUED", saved.getLastEventType());
        assertNotNull(saved.getLastEventAt());
    }

    @Test
    void getCallSessionAndCampaignSessionsMapEntitiesToResponses() throws Exception {
        CallSessionRepository repository = mock(CallSessionRepository.class);
        CallSessionService service = new CallSessionService(repository);

        CallSessionEntity latest = entity(
                "session-2",
                "campaign-1",
                "lead-2",
                "OUTBOUND_IVR",
                "FLOWING",
                "CALL_COMPLETED",
                Instant.parse("2026-03-22T12:10:00Z"),
                Instant.parse("2026-03-22T12:00:00Z")
        );
        CallSessionEntity earlier = entity(
                "session-1",
                "campaign-1",
                "lead-1",
                "AGENT_ASSISTED",
                "CONNECTED",
                "CALL_BRIDGED",
                Instant.parse("2026-03-22T11:10:00Z"),
                Instant.parse("2026-03-22T11:00:00Z")
        );

        when(repository.findById("session-2")).thenReturn(Optional.of(latest));
        when(repository.findByCampaignIdOrderByCreatedAtDesc("campaign-1")).thenReturn(List.of(latest, earlier));

        CallSessionResponse response = service.getCallSession("session-2").orElseThrow();
        List<CallSessionResponse> campaignSessions = service.getCampaignSessions("campaign-1");

        assertEquals("session-2", response.callSessionId());
        assertEquals("OUTBOUND_IVR", response.callMode());
        assertEquals("CALL_COMPLETED", response.lastEventType());
        assertEquals(2, campaignSessions.size());
        assertEquals("session-2", campaignSessions.get(0).callSessionId());
        assertEquals("lead-1", campaignSessions.get(1).leadId());
    }

    private CallSessionEntity entity(String callSessionId,
                                     String campaignId,
                                     String leadId,
                                     String callMode,
                                     String status,
                                     String lastEventType,
                                     Instant lastEventAt,
                                     Instant createdAt) throws Exception {
        CallSessionEntity entity = new CallSessionEntity();
        entity.setCallSessionId(callSessionId);
        entity.setCampaignId(campaignId);
        entity.setLeadId(leadId);
        entity.setProvider("ASTERISK");
        entity.setCustomerNumber("+1555000" + leadId.charAt(leadId.length() - 1));
        entity.setAgentId("agent-" + leadId.charAt(leadId.length() - 1));
        entity.setAgentChannel("PJSIP/100" + leadId.charAt(leadId.length() - 1));
        entity.setIvrFlowId("ivr-1");
        entity.setCallMode(callMode);
        entity.setStatus(status);
        entity.setLastEventType(lastEventType);
        entity.setLastEventAt(lastEventAt);
        setField(entity, "createdAt", createdAt);
        return entity;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
