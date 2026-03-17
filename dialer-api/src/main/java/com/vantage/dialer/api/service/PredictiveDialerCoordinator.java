package com.vantage.dialer.api.service;

import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.campaign.CampaignEngine;
import com.vantage.dialer.api.campaign.DialMode;
import com.vantage.dialer.api.kafka.CommandProducer;
import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.events.StandardEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PredictiveDialerCoordinator {

    private final CampaignEngine campaignEngine;
    private final AgentStore agentStore;
    private final CommandProducer commandProducer;
    private final ConcurrentHashMap<String, Queue<PendingAgentAssignment>> waitingByCampaign = new ConcurrentHashMap<>();

    public PredictiveDialerCoordinator(CampaignEngine campaignEngine,
                                       AgentStore agentStore,
                                       CommandProducer commandProducer) {
        this.campaignEngine = campaignEngine;
        this.agentStore = agentStore;
        this.commandProducer = commandProducer;
    }

    public void queueAnsweredCall(StandardEvent event) {
        String campaignId = String.valueOf(event.getPayload().get("campaignId"));
        if (!isPredictive(campaignId)) {
            return;
        }

        waitingByCampaign
                .computeIfAbsent(campaignId, ignored -> new ArrayDeque<>())
                .add(new PendingAgentAssignment(
                        event.getCallSessionId(),
                        campaignId,
                        String.valueOf(event.getPayload().get("leadId")),
                        event.getProvider()
                ));
        dispatchWaitingCalls(campaignId);
    }

    public void dispatchWaitingCalls(String campaignId) {
        if (!isPredictive(campaignId)) {
            return;
        }

        Queue<PendingAgentAssignment> queue = waitingByCampaign.get(campaignId);
        if (queue == null) {
            return;
        }

        while (!queue.isEmpty()) {
            var agent = agentStore.acquireAvailableAgent().orElse(null);
            if (agent == null) {
                return;
            }

            PendingAgentAssignment pending = queue.poll();
            CommandMessage command = new CommandMessage();
            command.setCommandId(UUID.randomUUID().toString());
            command.setCommandType(CommandType.DIAL_AGENT);
            command.setCallSessionId(pending.callSessionId());
            command.setProvider(pending.provider());
            command.setTimestamp(Instant.now());

            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("campaignId", pending.campaignId());
            payload.put("leadId", pending.leadId());
            payload.put("agentId", agent.getAgentId());
            payload.put("agentChannel", agent.getChannel());
            command.setPayload(payload);

            commandProducer.sendCommand(command);
        }
    }

    private boolean isPredictive(String campaignId) {
        return campaignEngine.getExecution(campaignId)
                .map(execution -> execution.dialMode() == DialMode.PREDICTIVE)
                .orElse(false);
    }

    private record PendingAgentAssignment(
            String callSessionId,
            String campaignId,
            String leadId,
            String provider) {
    }
}
