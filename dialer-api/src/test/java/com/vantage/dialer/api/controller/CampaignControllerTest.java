package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.campaign.CampaignEngine;
import com.vantage.dialer.api.campaign.DialMode;
import com.vantage.dialer.api.campaign.Lead;
import com.vantage.dialer.api.campaign.LeadStore;
import com.vantage.dialer.api.dto.CampaignRequest;
import com.vantage.dialer.api.dto.CampaignResponse;
import com.vantage.dialer.api.service.CampaignCatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CampaignControllerTest {

    @Test
    void createCampaignBindsRequestBodyAndReturnsCatalogResponse() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        CampaignEngine engine = mock(CampaignEngine.class);
        CampaignCatalogService catalogService = mock(CampaignCatalogService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CampaignController(leadStore, engine, catalogService));

        CampaignRequest request = new CampaignRequest();
        request.setName("Spring Launch");
        request.setProvider("ASTERISK");
        request.setDialMode("PROGRESSIVE");

        when(catalogService.createCampaign(any(CampaignRequest.class)))
                .thenReturn(new CampaignResponse("camp-1", "Spring Launch", "ASTERISK", "PROGRESSIVE", "DRAFT", null, 5, 2, 1.5));

        mockMvc.perform(post("/campaigns")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-1"))
                .andExpect(jsonPath("$.name").value("Spring Launch"));

        ArgumentCaptor<CampaignRequest> captor = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(catalogService).createCampaign(captor.capture());
        assertEquals("Spring Launch", captor.getValue().getName());
    }

    @Test
    void createCampaignUsesDefaultRequestWhenBodyIsOmitted() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        CampaignEngine engine = mock(CampaignEngine.class);
        CampaignCatalogService catalogService = mock(CampaignCatalogService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CampaignController(leadStore, engine, catalogService));

        when(catalogService.createCampaign(any(CampaignRequest.class)))
                .thenReturn(new CampaignResponse("camp-default", null, null, null, "DRAFT", null, 5, 2, 1.5));

        mockMvc.perform(post("/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-default"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        ArgumentCaptor<CampaignRequest> captor = ArgumentCaptor.forClass(CampaignRequest.class);
        verify(catalogService).createCampaign(captor.capture());
        CampaignRequest request = captor.getValue();
        assertEquals(null, request.getName());
        assertEquals(null, request.getProvider());
        assertEquals(null, request.getDialMode());
    }

    @Test
    void addLeadBindsCustomerNumberAndReturnsGeneratedLeadId() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        CampaignEngine engine = mock(CampaignEngine.class);
        CampaignCatalogService catalogService = mock(CampaignCatalogService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CampaignController(leadStore, engine, catalogService));

        mockMvc.perform(post("/campaigns/camp-2/leads")
                        .contentType(APPLICATION_JSON)
                        .content("{\"customerNumber\":\"+15551234567\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("\"leadId=")));

        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(leadStore).addLead(captor.capture());
        assertEquals("camp-2", captor.getValue().getCampaignId());
        assertEquals("+15551234567", captor.getValue().getCustomerNumber());
        assertTrue(captor.getValue().getLeadId() != null && !captor.getValue().getLeadId().isBlank());
    }

    @Test
    void startParsesParametersAndMarksCampaignRunning() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        CampaignEngine engine = mock(CampaignEngine.class);
        CampaignCatalogService catalogService = mock(CampaignCatalogService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CampaignController(leadStore, engine, catalogService));

        mockMvc.perform(post("/campaigns/camp-3/start")
                        .queryParam("maxConcurrentCalls", "7")
                        .queryParam("callsPerSecond", "3")
                        .queryParam("provider", "ASTERISK")
                        .queryParam("mode", "predictive")
                        .queryParam("predictiveRatio", "1.8"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"campaign started: camp-3\""));

        verify(engine).startCampaign("camp-3", 7, 3, "ASTERISK", DialMode.PREDICTIVE, 1.8);
        verify(catalogService).markRunning("camp-3", DialMode.PREDICTIVE, "ASTERISK", 7, 3, 1.8);
    }

    @Test
    void startUsesDefaultParametersWhenQueryParamsAreOmitted() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        CampaignEngine engine = mock(CampaignEngine.class);
        CampaignCatalogService catalogService = mock(CampaignCatalogService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CampaignController(leadStore, engine, catalogService));

        mockMvc.perform(post("/campaigns/camp-default/start"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"campaign started: camp-default\""));

        verify(engine).startCampaign("camp-default", 5, 2, "EXOTEL", DialMode.PROGRESSIVE, 1.5);
        verify(catalogService).markRunning("camp-default", DialMode.PROGRESSIVE, "EXOTEL", 5, 2, 1.5);
    }

    @Test
    void stopDelegatesToEngineAndCatalog() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        CampaignEngine engine = mock(CampaignEngine.class);
        CampaignCatalogService catalogService = mock(CampaignCatalogService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new CampaignController(leadStore, engine, catalogService));

        mockMvc.perform(post("/campaigns/camp-4/stop"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"campaign stopped: camp-4\""));

        verify(engine).stopCampaign("camp-4");
        verify(catalogService).markStopped("camp-4");
    }
}
