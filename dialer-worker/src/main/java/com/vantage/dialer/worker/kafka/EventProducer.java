package com.vantage.dialer.worker.kafka;

import com.vantage.dialer.common.events.StandardEvent;
import com.vantage.dialer.common.kafka.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventProducer {

    private final KafkaTemplate<String, StandardEvent> kafkaTemplate;

    public EventProducer(KafkaTemplate<String, StandardEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(StandardEvent event) {
        kafkaTemplate.send(Topics.EVENTS, event.getCallSessionId(), event);
    }
}