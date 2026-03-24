package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.CostConfigurationRequest;
import com.vantage.dialer.api.dto.CostConfigurationResponse;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CostEstimateResponse;
import com.vantage.dialer.api.service.PricingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PricingControllerTest {

    @Test
    void estimateBindsUsageRequestAndReturnsEstimate() throws Exception {
        PricingService service = mock(PricingService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new PricingController(service));
        CostEstimateRequest request = new CostEstimateRequest();
        request.setCustomerId("cust-1");
        request.setMonthlyCallMinutes(1200L);
        request.setDesiredMarginPercent(35.0);

        when(service.estimate(any(CostEstimateRequest.class)))
                .thenReturn(new CostEstimateResponse("cust-1", "config-1", 40.0, 72.0, 112.0, 151.2, 35.0));

        mockMvc.perform(post("/pricing/estimate")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-1"))
                .andExpect(jsonPath("$.totalEstimatedCost").value(112.0))
                .andExpect(jsonPath("$.suggestedSellPrice").value(151.2));

        ArgumentCaptor<CostEstimateRequest> captor = ArgumentCaptor.forClass(CostEstimateRequest.class);
        verify(service).estimate(captor.capture());
        assertEquals("cust-1", captor.getValue().getCustomerId());
        assertEquals(1200L, captor.getValue().getMonthlyCallMinutes());
    }

    @Test
    void configUpsertBindsRequestAndReturnsPersistedConfiguration() throws Exception {
        PricingService service = mock(PricingService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new PricingController(service));
        CostConfigurationRequest request = new CostConfigurationRequest();
        request.setCustomerId("cust-1");
        request.setAsteriskServerMonthlyCost(20.0);
        request.setVoiceMinuteCost(0.5);

        when(service.upsertCustomerConfiguration(any(CostConfigurationRequest.class)))
                .thenReturn(new CostConfigurationResponse("config-1", "cust-1", 20.0, 25.0, 5.0, 2.0, 0.5, 0.2, 0.3, 0.4));

        mockMvc.perform(post("/pricing/config")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurationId").value("config-1"))
                .andExpect(jsonPath("$.customerId").value("cust-1"))
                .andExpect(jsonPath("$.voiceMinuteCost").value(0.5));

        ArgumentCaptor<CostConfigurationRequest> captor = ArgumentCaptor.forClass(CostConfigurationRequest.class);
        verify(service).upsertCustomerConfiguration(captor.capture());
        assertEquals("cust-1", captor.getValue().getCustomerId());
        assertEquals(20.0, captor.getValue().getAsteriskServerMonthlyCost());
        assertEquals(0.5, captor.getValue().getVoiceMinuteCost());
    }

    @Test
    void configLookupEndpointsReturnDefaultAndCustomerConfiguration() throws Exception {
        PricingService service = mock(PricingService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new PricingController(service));

        when(service.getDefaultConfiguration())
                .thenReturn(new CostConfigurationResponse("default", null, 10.0, 11.0, 12.0, 13.0, 0.1, 0.2, 0.3, 0.4));
        when(service.getCustomerConfiguration("cust-9"))
                .thenReturn(new CostConfigurationResponse("config-9", "cust-9", 20.0, 21.0, 22.0, 23.0, 0.5, 0.6, 0.7, 0.8));

        mockMvc.perform(get("/pricing/config/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurationId").value("default"));

        mockMvc.perform(get("/pricing/config/cust-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-9"))
                .andExpect(jsonPath("$.recordingGbCost").value(0.8));

        verify(service).getDefaultConfiguration();
        verify(service).getCustomerConfiguration("cust-9");
    }
}
