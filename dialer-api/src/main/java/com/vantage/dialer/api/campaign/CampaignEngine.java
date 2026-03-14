package com.vantage.dialer.api.campaign;

import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.kafka.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class CampaignEngine {

    private final LeadStore leadStore;
    private final AgentStore agentStore;
    private final KafkaTemplate<String, CommandMessage> kafkaTemplate;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> running = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final ConcurrentHashMap<String, Long> lastSecond = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> callsThisSecond = new ConcurrentHashMap<>();

    public CampaignEngine(LeadStore leadStore,
                          AgentStore agentStore,
                          KafkaTemplate<String, CommandMessage> kafkaTemplate) {
        this.leadStore = leadStore;
        this.agentStore = agentStore;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void startCampaign(String campaignId, int maxConcurrentCalls, int callsPerSecond, String provider) {
        running.computeIfAbsent(campaignId, id ->
                scheduler.scheduleWithFixedDelay(
                        () -> tick(id, maxConcurrentCalls, callsPerSecond, provider),
                        0,
                        200,
                        TimeUnit.MILLISECONDS
                )
        );
    }

    public void stopCampaign(String campaignId) {
        ScheduledFuture<?> future = running.remove(campaignId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void tick(String campaignId, int maxConcurrentCalls, int callsPerSecond, String provider) {

        long active = leadStore.countActive(campaignId);
        int freeSlots = (int) Math.max(0, maxConcurrentCalls - active);

        if (freeSlots == 0) {
            return;
        }

        long nowSecond = System.currentTimeMillis() / 1000;

        lastSecond.putIfAbsent(campaignId, nowSecond);
        callsThisSecond.putIfAbsent(campaignId, 0);

        if (!lastSecond.get(campaignId).equals(nowSecond)) {
            lastSecond.put(campaignId, nowSecond);
            callsThisSecond.put(campaignId, 0);
        }

        int sentThisSecond = callsThisSecond.get(campaignId);
        int cpsRemaining = Math.max(0, callsPerSecond - sentThisSecond);

        if (cpsRemaining == 0) {
            return;
        }

        if (agentStore.countAvailable() == 0) {
            System.out.println("[API] No available agents for campaign=" + campaignId);
            return;
        }

        int allowed = Math.min(Math.min(freeSlots, cpsRemaining), (int) agentStore.countAvailable());

        if (allowed <= 0) {
            return;
        }

        var leads = leadStore.getNextNewLeads(campaignId, allowed);

        for (Lead lead : leads) {
            var agent = agentStore.acquireAvailableAgent().orElse(null);
            if (agent == null) {
                System.out.println("[API] No available agent could be reserved for leadId=" + lead.getLeadId());
                break;
            }

            leadStore.incrementAttempts(campaignId, lead.getLeadId());
            leadStore.updateStatus(campaignId, lead.getLeadId(), LeadStatus.QUEUED);

            CommandMessage cmd = new CommandMessage();
            cmd.setCommandId(UUID.randomUUID().toString());
            cmd.setCommandType(CommandType.START_CUSTOMER_CALL);
            cmd.setCallSessionId(UUID.randomUUID().toString());
            cmd.setProvider(provider);
            cmd.setTimestamp(Instant.now());
            cmd.setPayload(Map.of(
                    "campaignId", campaignId,
                    "leadId", lead.getLeadId(),
                    "customerNumber", lead.getCustomerNumber(),
                    "agentId", agent.getAgentId(),
                    "agentChannel", agent.getChannel()
            ));

            kafkaTemplate.send(Topics.COMMANDS, cmd.getCallSessionId(), cmd);
            callsThisSecond.put(campaignId, callsThisSecond.get(campaignId) + 1);

            System.out.println("[API] queued leadId=" + lead.getLeadId()
                    + " agentId=" + agent.getAgentId()
                    + " attempts=" + lead.getAttempts()
                    + " session=" + cmd.getCallSessionId());
        }
    }
}
