package com.vantage.dialer.api.campaign;

import com.vantage.dialer.api.persistence.model.LeadEntity;
import com.vantage.dialer.api.persistence.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
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

class LeadStoreTest {

    @Test
    void addLeadAndLookupEndpointsMapBetweenDomainAndRepositoryModels() {
        LeadRepository leadRepository = mock(LeadRepository.class);
        LeadStore store = new LeadStore(leadRepository);
        Lead lead = new Lead("lead-0", "campaign-0", "+155500");
        LeadEntity stored = entity("lead-0", "campaign-0", "+155500", LeadStatus.QUEUED, 1);

        when(leadRepository.save(any(LeadEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadRepository.findById("lead-0")).thenReturn(Optional.of(stored));
        when(leadRepository.findByCampaignIdOrderByCreatedAtAsc("campaign-0")).thenReturn(List.of(stored));

        store.addLead(lead);

        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository).save(captor.capture());
        LeadEntity saved = captor.getValue();
        assertEquals("lead-0", saved.getLeadId());
        assertEquals("campaign-0", saved.getCampaignId());
        assertEquals("+155500", saved.getCustomerNumber());
        assertEquals(LeadStatus.NEW, saved.getStatus());
        assertEquals(0, saved.getAttempts());

        Lead loaded = store.getLead("lead-0").orElseThrow();
        assertEquals("lead-0", loaded.getLeadId());
        assertEquals(LeadStatus.QUEUED, loaded.getStatus());
        assertEquals(1, loaded.getAttempts());

        List<Lead> leads = store.getLeads("campaign-0");
        assertEquals(1, leads.size());
        assertEquals("lead-0", leads.get(0).getLeadId());
        assertEquals(LeadStatus.QUEUED, leads.get(0).getStatus());
    }

    @Test
    void getNextNewLeadsFiltersAttemptLimitAndRespectsRequestedLimit() {
        LeadRepository leadRepository = mock(LeadRepository.class);
        LeadStore store = new LeadStore(leadRepository);

        when(leadRepository.findByCampaignIdAndStatusOrderByCreatedAtAsc("campaign-1", LeadStatus.NEW)).thenReturn(List.of(
                entity("lead-1", "campaign-1", "+155501", LeadStatus.NEW, 0),
                entity("lead-2", "campaign-1", "+155502", LeadStatus.NEW, 2),
                entity("lead-3", "campaign-1", "+155503", LeadStatus.NEW, 3)
        ));

        List<Lead> leads = store.getNextNewLeads("campaign-1", 2);

        assertEquals(2, leads.size());
        assertEquals(List.of("lead-1", "lead-2"), leads.stream().map(Lead::getLeadId).toList());
        assertTrue(leads.stream().allMatch(lead -> lead.getAttempts() < 3));
    }

    @Test
    void updateStatusAndIncrementAttemptsOnlyMutateMatchingCampaign() {
        LeadRepository leadRepository = mock(LeadRepository.class);
        LeadStore store = new LeadStore(leadRepository);
        LeadEntity matching = entity("lead-1", "campaign-1", "+155501", LeadStatus.NEW, 1);
        LeadEntity otherCampaign = entity("lead-2", "campaign-2", "+155502", LeadStatus.NEW, 1);

        when(leadRepository.findById("lead-1")).thenReturn(Optional.of(matching));
        when(leadRepository.findById("lead-2")).thenReturn(Optional.of(otherCampaign));
        when(leadRepository.save(any(LeadEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        store.updateStatus("campaign-1", "lead-1", LeadStatus.COMPLETED);
        store.incrementAttempts("campaign-1", "lead-1");
        store.updateStatus("campaign-1", "lead-2", LeadStatus.FAILED);
        store.incrementAttempts("campaign-1", "lead-2");

        assertEquals(LeadStatus.COMPLETED, matching.getStatus());
        assertEquals(2, matching.getAttempts());
        assertEquals(LeadStatus.NEW, otherCampaign.getStatus());
        assertEquals(1, otherCampaign.getAttempts());
        verify(leadRepository, times(2)).save(matching);
        verify(leadRepository, never()).save(otherCampaign);
    }

    @Test
    void getStatsAndCountActiveReflectRepositoryState() {
        LeadRepository leadRepository = mock(LeadRepository.class);
        LeadStore store = new LeadStore(leadRepository);

        when(leadRepository.findByCampaignIdOrderByCreatedAtAsc("campaign-1")).thenReturn(List.of(
                entity("lead-1", "campaign-1", "+155501", LeadStatus.NEW, 0),
                entity("lead-2", "campaign-1", "+155502", LeadStatus.QUEUED, 1),
                entity("lead-3", "campaign-1", "+155503", LeadStatus.IN_PROGRESS, 2),
                entity("lead-4", "campaign-1", "+155504", LeadStatus.COMPLETED, 1),
                entity("lead-5", "campaign-1", "+155505", LeadStatus.FAILED, 3)
        ));
        when(leadRepository.countByCampaignIdAndStatusIn(any(), any())).thenReturn(2L);

        CampaignStats stats = store.getStats("campaign-1");

        assertEquals(5, stats.getTotal());
        assertEquals(1, stats.getNewCount());
        assertEquals(1, stats.getQueued());
        assertEquals(1, stats.getInProgress());
        assertEquals(1, stats.getCompleted());
        assertEquals(1, stats.getFailed());
        assertEquals(2L, store.countActive("campaign-1"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void countActiveUsesQueuedAndInProgressStatusesOnly() {
        LeadRepository leadRepository = mock(LeadRepository.class);
        LeadStore store = new LeadStore(leadRepository);
        ArgumentCaptor<Collection> statusCaptor = ArgumentCaptor.forClass(Collection.class);

        when(leadRepository.countByCampaignIdAndStatusIn(any(), any())).thenReturn(3L);

        assertEquals(3L, store.countActive("campaign-2"));

        verify(leadRepository).countByCampaignIdAndStatusIn(org.mockito.ArgumentMatchers.eq("campaign-2"), statusCaptor.capture());
        assertEquals(java.util.Set.of(LeadStatus.QUEUED, LeadStatus.IN_PROGRESS), java.util.Set.copyOf(statusCaptor.getValue()));
    }

    private LeadEntity entity(String leadId,
                              String campaignId,
                              String customerNumber,
                              LeadStatus status,
                              int attempts) {
        LeadEntity entity = new LeadEntity();
        entity.setLeadId(leadId);
        entity.setCampaignId(campaignId);
        entity.setCustomerNumber(customerNumber);
        entity.setStatus(status);
        entity.setAttempts(attempts);
        return entity;
    }
}
