package com.vantage.dialer.api.campaign;

import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.kafka.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class CampaignEngine {

    private final LeadStore leadStore;
    private final AgentStore agentStore;
    private final KafkaTemplate<String, CommandMessage> kafkaTemplate;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> running = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CampaignExecution> executions = new ConcurrentHashMap<>();
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

    public void startCampaign(String campaignId,
                              int maxConcurrentCalls,
                              int callsPerSecond,
                              String provider,
                              DialMode dialMode,
                              double predictiveRatio) {
        CampaignExecution execution = new CampaignExecution(
                campaignId,
                maxConcurrentCalls,
                callsPerSecond,
                provider,
                dialMode,
                predictiveRatio
        );
        executions.put(campaignId, execution);
        running.computeIfAbsent(campaignId, id ->
                scheduler.scheduleWithFixedDelay(
                        () -> tick(execution),
                        0,
                        200,
                        TimeUnit.MILLISECONDS
                )
        );
    }

    public void stopCampaign(String campaignId) {
        ScheduledFuture<?> future = running.remove(campaignId);
        executions.remove(campaignId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public Optional<CampaignExecution> getExecution(String campaignId) {
        return Optional.ofNullable(executions.get(campaignId));
    }

    private void tick(CampaignExecution execution) {
        String campaignId = execution.campaignId();
        long active = leadStore.countActive(campaignId);
        int freeSlots = (int) Math.max(0, execution.maxConcurrentCalls() - active);
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
        int cpsRemaining = Math.max(0, execution.callsPerSecond() - sentThisSecond);
        if (cpsRemaining == 0) {
            return;
        }

        long availableAgents = agentStore.countAvailable();
        if (availableAgents == 0) {
            System.out.println("[API] No available agents for campaign=" + campaignId);
            return;
        }

        int allowed = allowedCalls(execution, freeSlots, cpsRemaining, availableAgents);
        if (allowed <= 0) {
            return;
        }

        var leads = leadStore.getNextNewLeads(campaignId, allowed);
        for (Lead lead : leads) {
            String agentId = null;
            String agentChannel = null;
            if (execution.dialMode() == DialMode.PROGRESSIVE) {
                var agent = agentStore.acquireAvailableAgent().orElse(null);
                if (agent == null) {
                    System.out.println("[API] No available agent could be reserved for leadId=" + lead.getLeadId());
                    break;
                }
                agentId = agent.getAgentId();
                agentChannel = agent.getChannel();
            }

            leadStore.incrementAttempts(campaignId, lead.getLeadId());
            leadStore.updateStatus(campaignId, lead.getLeadId(), LeadStatus.QUEUED);

            CommandMessage cmd = new CommandMessage();
            cmd.setCommandId(UUID.randomUUID().toString());
            cmd.setCommandType(CommandType.START_CUSTOMER_CALL);
            cmd.setCallSessionId(UUID.randomUUID().toString());
            cmd.setProvider(execution.provider());
            cmd.setTimestamp(Instant.now());
            cmd.setPayload(buildPayload(execution, lead, agentId, agentChannel));

            kafkaTemplate.send(Topics.COMMANDS, cmd.getCallSessionId(), cmd);
            callsThisSecond.put(campaignId, callsThisSecond.get(campaignId) + 1);

            System.out.println("[API] queued leadId=" + lead.getLeadId()
                    + " mode=" + execution.dialMode()
                    + " agentId=" + agentId
                    + " attempts=" + lead.getAttempts()
                    + " session=" + cmd.getCallSessionId());
        }
    }

    private int allowedCalls(CampaignExecution execution, int freeSlots, int cpsRemaining, long availableAgents) {
        if (execution.dialMode() == DialMode.PROGRESSIVE) {
            return Math.min(Math.min(freeSlots, cpsRemaining), (int) availableAgents);
        }

        int predictiveCapacity = (int) Math.ceil(availableAgents * execution.predictiveRatio());
        predictiveCapacity = Math.max((int) availableAgents, predictiveCapacity);
        return Math.min(Math.min(freeSlots, cpsRemaining), predictiveCapacity);
    }

    private Map<String, String> buildPayload(CampaignExecution execution,
                                             Lead lead,
                                             String agentId,
                                             String agentChannel) {
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        payload.put("campaignId", execution.campaignId());
        payload.put("leadId", lead.getLeadId());
        payload.put("customerNumber", lead.getCustomerNumber());
        payload.put("dialMode", execution.dialMode().name());
        if (agentId != null) {
            payload.put("agentId", agentId);
        }
        if (agentChannel != null) {
            payload.put("agentChannel", agentChannel);
        }
        return payload;
    }
}
