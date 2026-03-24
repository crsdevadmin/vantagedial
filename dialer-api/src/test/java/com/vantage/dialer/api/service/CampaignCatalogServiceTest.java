package com.vantage.dialer.api.service;

import com.vantage.dialer.api.campaign.DialMode;
import com.vantage.dialer.api.dto.CampaignRequest;
import com.vantage.dialer.api.dto.CampaignResponse;
import com.vantage.dialer.api.persistence.model.CampaignEntity;
import com.vantage.dialer.api.persistence.model.CampaignStatus;
import com.vantage.dialer.api.persistence.repository.CampaignRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignCatalogServiceTest {

    @Test
    void createCampaignAppliesDefaultsAndTrimsOptionalFields() {
        CampaignRepository repository = mock(CampaignRepository.class);
        CampaignCatalogService service = new CampaignCatalogService(repository);

        CampaignRequest request = new CampaignRequest();
        request.setName("   ");
        request.setProvider(" ");
        request.setDialMode(null);
        request.setIvrFlowId("   ");

        when(repository.save(any(CampaignEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignResponse response = service.createCampaign(request);

        ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(repository).save(captor.capture());
        CampaignEntity saved = captor.getValue();

        assertTrue(response.name().startsWith("Campaign "));
        assertEquals("ASTERISK", response.provider());
        assertEquals("PROGRESSIVE", response.dialMode());
        assertEquals("DRAFT", response.status());
        assertEquals(5, response.maxConcurrentCalls());
        assertEquals(2, response.callsPerSecond());
        assertEquals(1.5, response.predictiveRatio());
        assertNull(response.ivrFlowId());

        assertEquals(CampaignStatus.DRAFT, saved.getStatus());
        assertEquals(DialMode.PROGRESSIVE, saved.getDialMode());
        assertEquals("ASTERISK", saved.getProvider());
        assertNull(saved.getIvrFlowId());
    }

    @Test
    void markRunningCreatesMissingCampaignAndMarkStoppedUpdatesExisting() {
        CampaignRepository repository = mock(CampaignRepository.class);
        CampaignCatalogService service = new CampaignCatalogService(repository);

        when(repository.findById("campaign-1")).thenReturn(Optional.empty());
        when(repository.save(any(CampaignEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markRunning("campaign-1", DialMode.PREDICTIVE, "ASTERISK", 20, 4, 2.0);

        ArgumentCaptor<CampaignEntity> runningCaptor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(repository).save(runningCaptor.capture());
        CampaignEntity running = runningCaptor.getValue();

        assertEquals("campaign-1", running.getCampaignId());
        assertEquals("Campaign campaign-1", running.getName());
        assertEquals(CampaignStatus.RUNNING, running.getStatus());
        assertEquals(DialMode.PREDICTIVE, running.getDialMode());
        assertEquals(20, running.getMaxConcurrentCalls());
        assertEquals(4, running.getCallsPerSecond());
        assertEquals(2.0, running.getPredictiveRatio());

        CampaignEntity existing = new CampaignEntity();
        existing.setCampaignId("campaign-2");
        existing.setName("Campaign Two");
        existing.setProvider("ASTERISK");
        existing.setDialMode(DialMode.PROGRESSIVE);
        existing.setStatus(CampaignStatus.RUNNING);
        existing.setMaxConcurrentCalls(5);
        existing.setCallsPerSecond(2);
        existing.setPredictiveRatio(1.5);

        when(repository.findById("campaign-2")).thenReturn(Optional.of(existing));

        service.markStopped("campaign-2");

        assertEquals(CampaignStatus.STOPPED, existing.getStatus());
        verify(repository).save(existing);
    }
}
