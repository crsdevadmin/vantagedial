package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.service.DirectOutboundService;
import com.vantage.dialer.common.model.CallRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DialControllerTest {

    @Test
    void callEndpointBindsRequestAndReturnsQueuedResponse() throws Exception {
        DirectOutboundService directOutboundService = mock(DirectOutboundService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new DialController(directOutboundService));

        when(directOutboundService.queueCall(any(CallRequest.class)))
                .thenReturn(Map.of("status", "QUEUED", "callSessionId", "session-1"));

        mockMvc.perform(post("/dialer/call")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"customerNumber":"+15551234567","campaignId":"camp-1","provider":"ASTERISK"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.callSessionId").value("session-1"));

        ArgumentCaptor<CallRequest> captor = ArgumentCaptor.forClass(CallRequest.class);
        verify(directOutboundService).queueCall(captor.capture());
        assertEquals("+15551234567", captor.getValue().getCustomerNumber());
        assertEquals("camp-1", captor.getValue().getCampaignId());
        assertEquals("ASTERISK", captor.getValue().getProvider());
    }

    @Test
    void callEndpointFallsBackCustomerNumberFromPhoneNumber() throws Exception {
        DirectOutboundService directOutboundService = mock(DirectOutboundService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new DialController(directOutboundService));

        when(directOutboundService.queueCall(any(CallRequest.class)))
                .thenReturn(Map.of("status", "QUEUED", "callSessionId", "session-9"));

        mockMvc.perform(post("/dialer/call")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"+15559876543","campaignId":"camp-9","provider":"ASTERISK"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callSessionId").value("session-9"));

        ArgumentCaptor<CallRequest> captor = ArgumentCaptor.forClass(CallRequest.class);
        verify(directOutboundService).queueCall(captor.capture());
        assertEquals("+15559876543", captor.getValue().getPhoneNumber());
        assertEquals("+15559876543", captor.getValue().getCustomerNumber());
    }
}
