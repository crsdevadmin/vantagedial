package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.campaign.CampaignStats;
import com.vantage.dialer.api.campaign.Lead;
import com.vantage.dialer.api.campaign.LeadStatus;
import com.vantage.dialer.api.campaign.LeadStore;
import com.vantage.dialer.api.dto.CallSessionResponse;
import com.vantage.dialer.api.dto.CampaignResponse;
import com.vantage.dialer.api.service.CallSessionService;
import com.vantage.dialer.api.service.CampaignCatalogService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CampaignQueryControllerTest {

    @Test
    void getCampaignReturnsCatalogEntry() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        CampaignCatalogService catalogService = mock(CampaignCatalogService.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CampaignQueryController(leadStore, catalogService, callSessionService));

        when(catalogService.getCampaign("camp-1"))
                .thenReturn(Optional.of(new CampaignResponse("camp-1", "Launch", "ASTERISK", "PROGRESSIVE", "RUNNING", null, 5, 2, 1.5)));

        mockMvc.perform(get("/campaigns/camp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-1"))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void getCampaignPropagatesUnknownCampaignError() {
        LeadStore leadStore = mock(LeadStore.class);
        CampaignCatalogService catalogService = mock(CampaignCatalogService.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CampaignQueryController(leadStore, catalogService, callSessionService));

        when(catalogService.getCampaign("missing")).thenReturn(Optional.empty());

        ServletException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ServletException.class,
                () -> mockMvc.perform(get("/campaigns/missing")).andReturn()
        );

        IllegalArgumentException cause = assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Unknown campaign: missing", cause.getMessage());
    }

    @Test
    void leadsStatsAndSessionsEndpointsReturnAggregatedQueryData() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        CampaignCatalogService catalogService = mock(CampaignCatalogService.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CampaignQueryController(leadStore, catalogService, callSessionService));

        Lead lead = new Lead("lead-1", "camp-2", "+15550001");
        lead.setStatus(LeadStatus.QUEUED);
        lead.setAttempts(1);
        CampaignStats stats = new CampaignStats();
        stats.setTotal(4);
        stats.setQueued(1);
        stats.setCompleted(2);
        when(leadStore.getLeads("camp-2")).thenReturn(List.of(lead));
        when(leadStore.getStats("camp-2")).thenReturn(stats);
        when(callSessionService.getCampaignSessions("camp-2")).thenReturn(List.of(
                new CallSessionResponse("session-1", "camp-2", "lead-1", "ASTERISK", "+15550001", "agent-1", "PJSIP/1001", "PROGRESSIVE", null, "QUEUED", "CALL_CREATED", Instant.parse("2026-03-22T10:05:00Z"), Instant.parse("2026-03-22T10:00:00Z"))
        ));

        mockMvc.perform(get("/campaigns/camp-2/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leadId").value("lead-1"))
                .andExpect(jsonPath("$[0].status").value("QUEUED"));

        mockMvc.perform(get("/campaigns/camp-2/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.completed").value(2));

        mockMvc.perform(get("/campaigns/camp-2/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].callSessionId").value("session-1"))
                .andExpect(jsonPath("$[0].provider").value("ASTERISK"));
    }
}
