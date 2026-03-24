package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.IvrFlowRequest;
import com.vantage.dialer.api.dto.IvrFlowResponse;
import com.vantage.dialer.api.dto.IvrStepRequest;
import com.vantage.dialer.api.service.IvrFlowService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

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

class IvrControllerTest {

    @Test
    void createAndGetFlowEndpointsDelegateToService() throws Exception {
        IvrFlowService ivrFlowService = mock(IvrFlowService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new IvrController(ivrFlowService));
        IvrFlowResponse response = response();

        when(ivrFlowService.createFlow(any(IvrFlowRequest.class))).thenReturn(response);
        when(ivrFlowService.getFlow("ivr-1")).thenReturn(response);

        mockMvc.perform(post("/ivr/flows")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Welcome","description":"Main flow","steps":[{"stepOrder":1,"stepType":"PLAY_PROMPT","promptSourceType":"TEXT","promptValue":"Hello","dtmfMappings":{"1":"sales"},"fallbackAction":"RETRY"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ivrFlowId").value("ivr-1"))
                .andExpect(jsonPath("$.steps[0].stepType").value("PLAY_PROMPT"));

        mockMvc.perform(get("/ivr/flows/ivr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Welcome"))
                .andExpect(jsonPath("$.steps[0].promptValue").value("Hello"));

        ArgumentCaptor<IvrFlowRequest> captor = ArgumentCaptor.forClass(IvrFlowRequest.class);
        verify(ivrFlowService).createFlow(captor.capture());
        assertEquals("Welcome", captor.getValue().getName());
        assertEquals(1, captor.getValue().getSteps().size());
        assertEquals("PLAY_PROMPT", captor.getValue().getSteps().get(0).getStepType());
        assertEquals("Hello", captor.getValue().getSteps().get(0).getPromptValue());
        verify(ivrFlowService).getFlow("ivr-1");
    }

    private IvrFlowResponse response() {
        IvrStepRequest step = new IvrStepRequest();
        step.setStepOrder(1);
        step.setStepType("PLAY_PROMPT");
        step.setPromptSourceType("TEXT");
        step.setPromptValue("Hello");
        step.setDtmfMappings(Map.of("1", "sales"));
        step.setFallbackAction("RETRY");
        return new IvrFlowResponse("ivr-1", "Welcome", "Main flow", List.of(step));
    }
}
