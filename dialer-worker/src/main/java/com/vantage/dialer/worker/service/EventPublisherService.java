package com.vantage.dialer.worker.service;

import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import com.vantage.dialer.worker.core.CallEventPublisher;
import com.vantage.dialer.worker.core.CallSession;
import com.vantage.dialer.worker.kafka.EventProducer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class EventPublisherService implements CallEventPublisher {

    private final EventProducer eventProducer;

    public EventPublisherService(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    @Override
    public void publish(CallSession session, EventType eventType) {
        publish(session, eventType, Map.of());
    }

    @Override
    public void publishFailure(CallSession session, String result, Object error) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("result", result);
        extra.put("error", error);
        publish(session, EventType.CALL_FAILED, extra);
    }

    @Override
    public void publish(CallSession session, EventType eventType, Map<String, Object> extraPayload) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("campaignId", session.getCampaignId());
        payload.put("leadId", session.getLeadId());
        payload.put("customerNumber", session.getCustomerNumber());
        payload.put("agentId", session.getAgentId());
        payload.put("agentChannel", session.getAgentChannel());
        payload.put("customerChannel", session.getCustomerChannel());
        payload.put("agentLiveChannel", session.getAgentLiveChannel());
        payload.putAll(extraPayload);

        StandardEvent event = new StandardEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setCallSessionId(session.getCallSessionId());
        event.setEventType(eventType);
        event.setTimestamp(Instant.now());
        event.setProvider(session.getProvider());
        event.setPayload(payload);

        eventProducer.publish(event);
    }
}
