package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.service.DirectOutboundService;
import com.vantage.dialer.common.model.CallMode;
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

class OutboundControllerTest {

    @Test
    void startEndpointDelegatesCallRequestAsIs() throws Exception {
        DirectOutboundService directOutboundService = mock(DirectOutboundService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new OutboundController(directOutboundService));

        when(directOutboundService.queueCall(any(CallRequest.class)))
                .thenReturn(Map.of("status", "QUEUED", "callSessionId", "session-1"));

        mockMvc.perform(post("/outbound/start")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"customerNumber":"+15550001","campaignId":"camp-1","provider":"ASTERISK"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callSessionId").value("session-1"));

        ArgumentCaptor<CallRequest> captor = ArgumentCaptor.forClass(CallRequest.class);
        verify(directOutboundService).queueCall(captor.capture());
        assertEquals("+15550001", captor.getValue().getCustomerNumber());
        assertEquals("camp-1", captor.getValue().getCampaignId());
    }

    @Test
    void testCallEndpointBuildsDefaultCampaignAndProvider() throws Exception {
        DirectOutboundService directOutboundService = mock(DirectOutboundService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new OutboundController(directOutboundService));

        when(directOutboundService.queueCall(any(CallRequest.class)))
                .thenReturn(Map.of("status", "QUEUED", "callSessionId", "session-2"));

        mockMvc.perform(post("/outbound/test-call")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"customerNumber":"+15550002","campaignId":"custom","provider":"MANUAL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callSessionId").value("session-2"));

        ArgumentCaptor<CallRequest> captor = ArgumentCaptor.forClass(CallRequest.class);
        verify(directOutboundService).queueCall(captor.capture());
        assertEquals("+15550002", captor.getValue().getCustomerNumber());
        assertEquals("test-call", captor.getValue().getCampaignId());
        assertEquals("ASTERISK", captor.getValue().getProvider());
    }

    @Test
    void ivrStartEndpointForcesOutboundIvrMode() throws Exception {
        DirectOutboundService directOutboundService = mock(DirectOutboundService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new OutboundController(directOutboundService));

        when(directOutboundService.queueCall(any(CallRequest.class)))
                .thenReturn(Map.of("status", "QUEUED", "callSessionId", "session-3"));

        mockMvc.perform(post("/outbound/ivr/start")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"customerNumber":"+15550003","campaignId":"camp-3","ivrFlowId":"ivr-1","callMode":"AGENT_ASSISTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callSessionId").value("session-3"));

        ArgumentCaptor<CallRequest> captor = ArgumentCaptor.forClass(CallRequest.class);
        verify(directOutboundService).queueCall(captor.capture());
        assertEquals(CallMode.OUTBOUND_IVR.name(), captor.getValue().getCallMode());
        assertEquals("ivr-1", captor.getValue().getIvrFlowId());
    }
}
