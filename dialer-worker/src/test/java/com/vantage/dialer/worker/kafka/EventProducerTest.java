package com.vantage.dialer.worker.kafka;

import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import com.vantage.dialer.common.kafka.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventProducerTest {

    @Test
    void publishUsesEventsTopicAndCallSessionIdKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, StandardEvent> kafkaTemplate = mock(KafkaTemplate.class);
        EventProducer producer = new EventProducer(kafkaTemplate);

        StandardEvent event = new StandardEvent();
        event.setEventId("event-1");
        event.setCallSessionId("session-1");
        event.setEventType(EventType.CALL_CREATED);
        event.setTimestamp(Instant.parse("2026-03-23T10:00:00Z"));
        event.setProvider("ASTERISK");
        event.setPayload(Map.of("campaignId", "campaign-1"));

        producer.publish(event);

        verify(kafkaTemplate).send(Topics.EVENTS, "session-1", event);
    }
}
