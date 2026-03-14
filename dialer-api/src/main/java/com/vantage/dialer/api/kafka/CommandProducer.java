package com.vantage.dialer.api.kafka;

import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.kafka.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CommandProducer {

    private final KafkaTemplate<String, CommandMessage> kafkaTemplate;

    public CommandProducer(KafkaTemplate<String, CommandMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCommand(CommandMessage command) {

        kafkaTemplate.send(
                Topics.COMMANDS,
                command.getCallSessionId(),
                command
        );
    }
}