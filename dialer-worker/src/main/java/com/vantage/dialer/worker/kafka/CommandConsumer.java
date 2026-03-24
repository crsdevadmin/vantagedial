package com.vantage.dialer.worker.kafka;

import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.kafka.Topics;
import com.vantage.dialer.worker.service.OutboundCallService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CommandConsumer {

    private final OutboundCallService outboundCallService;

    public CommandConsumer(OutboundCallService outboundCallService) {
        this.outboundCallService = outboundCallService;
    }

    @KafkaListener(topics = Topics.COMMANDS, groupId = "dialer-worker")
    public void onCommand(CommandMessage command) {

        if (command.getCommandType() == CommandType.START_CUSTOMER_CALL
                || command.getCommandType() == CommandType.START_IVR_CALL) {

            String customerNumber = command.getPayload().get("customerNumber");
            String campaignId = command.getPayload().get("campaignId");
            String leadId = command.getPayload().get("leadId");
            String callMode = command.getPayload().get("callMode");
            String ivrFlowId = command.getPayload().get("ivrFlowId");
            String agentId = command.getPayload().get("agentId");
            String agentChannel = command.getPayload().get("agentChannel");

            System.out.println("[WORKER] " + command.getCommandType() + " session=" + command.getCallSessionId()
                    + " campaignId=" + campaignId
                    + " leadId=" + leadId
                    + " callMode=" + callMode
                    + " ivrFlowId=" + ivrFlowId
                    + " agentId=" + agentId
                    + " agentChannel=" + agentChannel
                    + " number=" + customerNumber);

            outboundCallService.start(
                    command.getCallSessionId(),
                    customerNumber,
                    command.getProvider(),
                    campaignId,
                    leadId,
                    callMode,
                    ivrFlowId,
                    agentId,
                    agentChannel
            );
            return;
        }

        if (command.getCommandType() == CommandType.DIAL_AGENT) {
            String agentId = command.getPayload().get("agentId");
            String agentChannel = command.getPayload().get("agentChannel");

            System.out.println("[WORKER] DIAL_AGENT session=" + command.getCallSessionId()
                    + " agentId=" + agentId
                    + " agentChannel=" + agentChannel);

            outboundCallService.dialAgent(
                    command.getCallSessionId(),
                    agentId,
                    agentChannel
            );
        }
    }
}
