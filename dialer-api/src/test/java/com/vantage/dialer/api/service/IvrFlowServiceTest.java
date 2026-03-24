package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.IvrFlowRequest;
import com.vantage.dialer.api.dto.IvrFlowResponse;
import com.vantage.dialer.api.dto.IvrStepRequest;
import com.vantage.dialer.api.persistence.model.IvrFlowEntity;
import com.vantage.dialer.api.persistence.model.IvrStepEntity;
import com.vantage.dialer.api.persistence.model.IvrStepType;
import com.vantage.dialer.api.persistence.model.PromptSourceType;
import com.vantage.dialer.api.persistence.repository.IvrFlowRepository;
import com.vantage.dialer.api.persistence.repository.IvrStepRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IvrFlowServiceTest {

    @Test
    void createFlowPersistsStepsAndReturnsRoundTrippedMappings() {
        IvrFlowRepository flowRepository = mock(IvrFlowRepository.class);
        IvrStepRepository stepRepository = mock(IvrStepRepository.class);
        IvrFlowService service = new IvrFlowService(
                flowRepository,
                stepRepository,
                CustomerServiceTestFixtures.objectMapper()
        );

        AtomicReference<IvrFlowEntity> savedFlow = new AtomicReference<>();
        List<IvrStepEntity> savedSteps = new ArrayList<>();

        when(flowRepository.save(any(IvrFlowEntity.class))).thenAnswer(invocation -> {
            IvrFlowEntity entity = invocation.getArgument(0);
            savedFlow.set(entity);
            return entity;
        });
        when(stepRepository.save(any(IvrStepEntity.class))).thenAnswer(invocation -> {
            IvrStepEntity entity = invocation.getArgument(0);
            savedSteps.add(entity);
            return entity;
        });
        when(flowRepository.findById(any())).thenAnswer(invocation -> Optional.of(savedFlow.get()));
        when(stepRepository.findByIvrFlowIdOrderByStepOrderAsc(any())).thenAnswer(invocation -> savedSteps);

        IvrFlowRequest request = new IvrFlowRequest();
        request.setName("Lead Qualification");
        request.setDescription("Capture DTMF before transfer");
        request.setSteps(List.of(
                step(1, "play_prompt", "text_to_speech", "Welcome", Map.of("1", "sales"), null, "HANGUP"),
                step(2, "capture_dtmf", null, "Press 1", new LinkedHashMap<>(Map.of("1", "TRANSFER")), "PJSIP/1001", "REPEAT")
        ));

        IvrFlowResponse response = service.createFlow(request);

        assertEquals("Lead Qualification", response.name());
        assertEquals("Capture DTMF before transfer", response.description());
        assertEquals(2, response.steps().size());
        assertEquals("PLAY_PROMPT", response.steps().get(0).getStepType());
        assertEquals("TEXT_TO_SPEECH", response.steps().get(0).getPromptSourceType());
        assertEquals("sales", response.steps().get(0).getDtmfMappings().get("1"));
        assertEquals("CAPTURE_DTMF", response.steps().get(1).getStepType());
        assertEquals("PJSIP/1001", response.steps().get(1).getTargetAgentChannel());

        assertEquals("Lead Qualification", savedFlow.get().getName());
        assertEquals(2, savedSteps.size());
        assertEquals(IvrStepType.PLAY_PROMPT, savedSteps.get(0).getStepType());
        assertEquals(PromptSourceType.TEXT_TO_SPEECH, savedSteps.get(0).getPromptSourceType());
        assertTrue(savedSteps.get(0).getDtmfMappingsJson().contains("\"1\":\"sales\""));
        assertEquals(IvrStepType.CAPTURE_DTMF, savedSteps.get(1).getStepType());
    }

    @Test
    void getFlowReturnsEmptyMappingsWhenStoredJsonIsBlank() {
        IvrFlowRepository flowRepository = mock(IvrFlowRepository.class);
        IvrStepRepository stepRepository = mock(IvrStepRepository.class);
        IvrFlowService service = new IvrFlowService(
                flowRepository,
                stepRepository,
                CustomerServiceTestFixtures.objectMapper()
        );

        IvrFlowEntity flow = new IvrFlowEntity();
        flow.setIvrFlowId("ivr-1");
        flow.setName("Fallback Flow");
        flow.setDescription("Simple flow");

        IvrStepEntity step = new IvrStepEntity();
        step.setIvrStepId("step-1");
        step.setIvrFlowId("ivr-1");
        step.setStepOrder(1);
        step.setStepType(IvrStepType.HANGUP);
        step.setPromptValue("Goodbye");
        step.setDtmfMappingsJson("");
        step.setTargetAgentChannel(null);
        step.setFallbackAction("HANGUP");

        when(flowRepository.findById("ivr-1")).thenReturn(Optional.of(flow));
        when(stepRepository.findByIvrFlowIdOrderByStepOrderAsc("ivr-1")).thenReturn(List.of(step));

        IvrFlowResponse response = service.getFlow("ivr-1");

        assertEquals("Fallback Flow", response.name());
        assertEquals(1, response.steps().size());
        assertEquals("HANGUP", response.steps().get(0).getStepType());
        assertEquals(Map.of(), response.steps().get(0).getDtmfMappings());
        assertNull(response.steps().get(0).getPromptSourceType());
    }

    private IvrStepRequest step(int order,
                                String stepType,
                                String promptSourceType,
                                String promptValue,
                                Map<String, String> dtmfMappings,
                                String targetAgentChannel,
                                String fallbackAction) {
        IvrStepRequest request = new IvrStepRequest();
        request.setStepOrder(order);
        request.setStepType(stepType);
        request.setPromptSourceType(promptSourceType);
        request.setPromptValue(promptValue);
        request.setDtmfMappings(dtmfMappings);
        request.setTargetAgentChannel(targetAgentChannel);
        request.setFallbackAction(fallbackAction);
        return request;
    }
}
