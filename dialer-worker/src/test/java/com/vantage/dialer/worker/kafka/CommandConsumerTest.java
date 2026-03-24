package com.vantage.dialer.worker.kafka;

import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.worker.service.OutboundCallService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CommandConsumerTest {

    @Test
    void startCustomerAndIvrCommandsDelegateToOutboundStart() {
        OutboundCallService outboundCallService = mock(OutboundCallService.class);
        CommandConsumer consumer = new CommandConsumer(outboundCallService);

        consumer.onCommand(command(CommandType.START_CUSTOMER_CALL, Map.of(
                "customerNumber", "+15550001",
                "campaignId", "campaign-1",
                "leadId", "lead-1",
                "callMode", "AGENT_ASSISTED",
                "ivrFlowId", "",
                "agentId", "agent-1",
                "agentChannel", "PJSIP/1001"
        )));

        consumer.onCommand(command(CommandType.START_IVR_CALL, Map.of(
                "customerNumber", "+15550002",
                "campaignId", "campaign-2",
                "leadId", "lead-2",
                "callMode", "OUTBOUND_IVR",
                "ivrFlowId", "ivr-1",
                "agentId", "",
                "agentChannel", ""
        )));

        verify(outboundCallService).start("session-1", "+15550001", "ASTERISK", "campaign-1", "lead-1", "AGENT_ASSISTED", "", "agent-1", "PJSIP/1001");
        verify(outboundCallService).start("session-1", "+15550002", "ASTERISK", "campaign-2", "lead-2", "OUTBOUND_IVR", "ivr-1", "", "");
    }

    @Test
    void dialAgentCommandDelegatesToDialAgent() {
        OutboundCallService outboundCallService = mock(OutboundCallService.class);
        CommandConsumer consumer = new CommandConsumer(outboundCallService);

        consumer.onCommand(command(CommandType.DIAL_AGENT, Map.of(
                "agentId", "agent-1",
                "agentChannel", "PJSIP/1001"
        )));

        verify(outboundCallService).dialAgent("session-1", "agent-1", "PJSIP/1001");
    }

    @Test
    void unsupportedCommandsDoNothing() {
        OutboundCallService outboundCallService = mock(OutboundCallService.class);
        CommandConsumer consumer = new CommandConsumer(outboundCallService);

        consumer.onCommand(command(CommandType.BRIDGE_CALL, Map.of()));

        verifyNoInteractions(outboundCallService);
    }

    private CommandMessage command(CommandType type, Map<String, String> payload) {
        CommandMessage command = new CommandMessage();
        command.setCommandId("cmd-1");
        command.setCommandType(type);
        command.setCallSessionId("session-1");
        command.setProvider("ASTERISK");
        command.setTimestamp(Instant.parse("2026-03-23T10:00:00Z"));
        command.setPayload(payload);
        return command;
    }
}
