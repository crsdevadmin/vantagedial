package com.vantage.dialer.api.kafka;

import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.kafka.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}
