package com.vantage.dialer.api.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.persistence.model.CallEventEntity;
import com.vantage.dialer.api.persistence.model.CallSessionEntity;
import com.vantage.dialer.api.persistence.repository.CallEventRepository;
import com.vantage.dialer.api.persistence.repository.CallSessionRepository;
import com.vantage.dialer.common.events.StandardEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class EventStore {

    private final CallEventRepository callEventRepository;
    private final CallSessionRepository callSessionRepository;
    private final ObjectMapper objectMapper;

    public EventStore(CallEventRepository callEventRepository,
                      CallSessionRepository callSessionRepository,
                      ObjectMapper objectMapper) {
        this.callEventRepository = callEventRepository;
        this.callSessionRepository = callSessionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void upsert(StandardEvent event) {
        CallEventEntity entity = new CallEventEntity();
        entity.setEventId(event.getEventId());
        entity.setCallSessionId(event.getCallSessionId());
        entity.setCallLegId(event.getCallLegId());
        entity.setEventType(event.getEventType().name());
        entity.setEventTimestamp(event.getTimestamp());
        entity.setProvider(event.getProvider());
        entity.setProviderCallId(event.getProviderCallId());
        entity.setLegType(event.getLegType());
        entity.setCampaignId(readPayload(event, "campaignId"));
        entity.setLeadId(readPayload(event, "leadId"));
        entity.setAgentId(readPayload(event, "agentId"));
        entity.setPayloadJson(writePayload(event.getPayload()));
        callEventRepository.save(entity);

        CallSessionEntity session = callSessionRepository.findById(event.getCallSessionId())
                .orElseGet(CallSessionEntity::new);
        session.setCallSessionId(event.getCallSessionId());
        session.setCampaignId(readPayload(event, "campaignId"));
        session.setLeadId(readPayload(event, "leadId"));
        session.setProvider(event.getProvider());
        session.setCustomerNumber(readPayload(event, "customerNumber"));
        session.setAgentId(readPayload(event, "agentId"));
        session.setAgentChannel(readPayload(event, "agentChannel"));
        session.setIvrFlowId(readPayload(event, "ivrFlowId"));
        session.setCallMode(readPayload(event, "callMode") == null ? "AGENT_ASSISTED" : readPayload(event, "callMode"));
        session.setStatus(event.getEventType().name());
        session.setLastEventType(event.getEventType().name());
        session.setLastEventAt(event.getTimestamp());
        callSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<StandardEvent> getLatest(String callSessionId) {
        List<CallEventEntity> events = callEventRepository.findByCallSessionIdOrderByEventTimestampAsc(callSessionId);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toEvent(events.get(events.size() - 1)));
    }

    @Transactional(readOnly = true)
    public List<StandardEvent> getTimeline(String callSessionId) {
        return callEventRepository.findByCallSessionIdOrderByEventTimestampAsc(callSessionId).stream()
                .map(this::toEvent)
                .toList();
    }

    private StandardEvent toEvent(CallEventEntity entity) {
        StandardEvent event = new StandardEvent();
        event.setEventId(entity.getEventId());
        event.setCallSessionId(entity.getCallSessionId());
        event.setCallLegId(entity.getCallLegId());
        event.setEventType(com.vantage.dialer.common.events.EventType.valueOf(entity.getEventType()));
        event.setTimestamp(entity.getEventTimestamp());
        event.setProvider(entity.getProvider());
        event.setProviderCallId(entity.getProviderCallId());
        event.setLegType(entity.getLegType());
        event.setPayload(readPayloadMap(entity.getPayloadJson()));
        return event;
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }

    private Map<String, Object> readPayloadMap(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize event payload", e);
        }
    }

    private String readPayload(StandardEvent event, String key) {
        if (event.getPayload() == null) {
            return null;
        }
        Object value = event.getPayload().get(key);
        return value == null ? null : String.valueOf(value);
    }

}
