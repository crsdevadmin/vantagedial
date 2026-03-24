package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.IvrFlowRequest;
import com.vantage.dialer.api.dto.IvrFlowResponse;
import com.vantage.dialer.api.dto.IvrStepRequest;
import com.vantage.dialer.api.persistence.model.IvrFlowEntity;
import com.vantage.dialer.api.persistence.model.IvrStepEntity;
import com.vantage.dialer.api.persistence.model.IvrStepType;
import com.vantage.dialer.api.persistence.model.PromptSourceType;
import com.vantage.dialer.api.persistence.repository.IvrFlowRepository;
import com.vantage.dialer.api.persistence.repository.IvrStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class IvrFlowService {

    private final IvrFlowRepository ivrFlowRepository;
    private final IvrStepRepository ivrStepRepository;
    private final ObjectMapper objectMapper;

    public IvrFlowService(IvrFlowRepository ivrFlowRepository,
                          IvrStepRepository ivrStepRepository,
                          ObjectMapper objectMapper) {
        this.ivrFlowRepository = ivrFlowRepository;
        this.ivrStepRepository = ivrStepRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IvrFlowResponse createFlow(IvrFlowRequest request) {
        IvrFlowEntity flow = new IvrFlowEntity();
        flow.setIvrFlowId(UUID.randomUUID().toString());
        flow.setName(request.getName());
        flow.setDescription(request.getDescription());
        ivrFlowRepository.save(flow);

        if (request.getSteps() != null) {
            for (IvrStepRequest stepRequest : request.getSteps()) {
                IvrStepEntity step = new IvrStepEntity();
                step.setIvrStepId(UUID.randomUUID().toString());
                step.setIvrFlowId(flow.getIvrFlowId());
                step.setStepOrder(stepRequest.getStepOrder());
                step.setStepType(IvrStepType.valueOf(stepRequest.getStepType().trim().toUpperCase()));
                if (stepRequest.getPromptSourceType() != null && !stepRequest.getPromptSourceType().isBlank()) {
                    step.setPromptSourceType(PromptSourceType.valueOf(stepRequest.getPromptSourceType().trim().toUpperCase()));
                }
                step.setPromptValue(stepRequest.getPromptValue());
                step.setDtmfMappingsJson(writeJson(stepRequest.getDtmfMappings()));
                step.setTargetAgentChannel(stepRequest.getTargetAgentChannel());
                step.setFallbackAction(stepRequest.getFallbackAction());
                ivrStepRepository.save(step);
            }
        }
        return getFlow(flow.getIvrFlowId());
    }

    @Transactional(readOnly = true)
    public IvrFlowResponse getFlow(String ivrFlowId) {
        IvrFlowEntity flow = ivrFlowRepository.findById(ivrFlowId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown IVR flow: " + ivrFlowId));
        List<IvrStepRequest> steps = ivrStepRepository.findByIvrFlowIdOrderByStepOrderAsc(ivrFlowId).stream()
                .map(step -> {
                    IvrStepRequest request = new IvrStepRequest();
                    request.setStepOrder(step.getStepOrder());
                    request.setStepType(step.getStepType().name());
                    request.setPromptSourceType(step.getPromptSourceType() == null ? null : step.getPromptSourceType().name());
                    request.setPromptValue(step.getPromptValue());
                    request.setDtmfMappings(readMap(step.getDtmfMappingsJson()));
                    request.setTargetAgentChannel(step.getTargetAgentChannel());
                    request.setFallbackAction(step.getFallbackAction());
                    return request;
                })
                .toList();
        return new IvrFlowResponse(flow.getIvrFlowId(), flow.getName(), flow.getDescription(), steps);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? java.util.Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize IVR mappings", e);
        }
    }

    private java.util.Map<String, String> readMap(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<java.util.LinkedHashMap<String, String>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize IVR mappings", e);
        }
    }
}
