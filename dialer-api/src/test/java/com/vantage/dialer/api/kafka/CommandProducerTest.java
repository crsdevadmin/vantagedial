package com.vantage.dialer.api.kafka;

import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.kafka.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandProducerTest {

    @Test
    void sendCommandPublishesToCommandsTopicUsingCallSessionIdAsKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, CommandMessage> kafkaTemplate = mock(KafkaTemplate.class);
        CommandProducer producer = new CommandProducer(kafkaTemplate);

        CommandMessage command = new CommandMessage();
        command.setCommandId("cmd-1");
        command.setCommandType(CommandType.START_CUSTOMER_CALL);
        command.setCallSessionId("session-1");
        command.setProvider("ASTERISK");
        command.setTimestamp(Instant.parse("2026-03-23T10:00:00Z"));
        command.setPayload(Map.of("customerNumber", "+15550001"));

        producer.sendCommand(command);

        verify(kafkaTemplate).send(Topics.COMMANDS, "session-1", command);
    }

    @Test
    void sendCommandCanTreatKafkaAsOptional() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, CommandMessage> kafkaTemplate = mock(KafkaTemplate.class);
        CommandProducer producer = new CommandProducer(kafkaTemplate, false);
        CommandMessage command = new CommandMessage();
        command.setCallSessionId("session-2");

        when(kafkaTemplate.send(Topics.COMMANDS, "session-2", command))
                .thenThrow(new IllegalStateException("broker unavailable"));

        assertDoesNotThrow(() -> producer.sendCommand(command));
    }
}
