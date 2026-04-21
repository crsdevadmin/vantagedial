package com.vantage.dialer.api.kafka;

import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.kafka.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CommandProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandProducer.class);

    private final KafkaTemplate<String, CommandMessage> kafkaTemplate;
    private final boolean commandPublishRequired;

    @Autowired
    public CommandProducer(KafkaTemplate<String, CommandMessage> kafkaTemplate,
                           @Value("${app.kafka.command-publish-required:true}") boolean commandPublishRequired) {
        this.kafkaTemplate = kafkaTemplate;
        this.commandPublishRequired = commandPublishRequired;
    }

    CommandProducer(KafkaTemplate<String, CommandMessage> kafkaTemplate) {
        this(kafkaTemplate, true);
    }

    public void sendCommand(CommandMessage command) {
        try {
            kafkaTemplate.send(
                    Topics.COMMANDS,
                    command.getCallSessionId(),
                    command
            );
        } catch (RuntimeException e) {
            if (commandPublishRequired) {
                throw e;
            }
            LOGGER.warn(
                    "Skipping command publish for callSessionId={} because Kafka is unavailable and command publishing is optional",
                    command.getCallSessionId(),
                    e
            );
        }
    }
}
