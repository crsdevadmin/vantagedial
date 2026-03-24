package com.vantage.dialer.api.service;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.kafka.CommandProducer;
import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.model.CallMode;
import com.vantage.dialer.common.model.CallRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class DirectOutboundService {

    private static final String DEFAULT_PROVIDER = "ASTERISK";
    private static final String DEFAULT_CAMPAIGN_ID = "direct-outbound";

    private final AgentStore agentStore;
    private final CommandProducer producer;
    private final CallSessionService callSessionService;

    public DirectOutboundService(AgentStore agentStore,
                                 CommandProducer producer,
                                 CallSessionService callSessionService) {
        this.agentStore = agentStore;
        this.producer = producer;
        this.callSessionService = callSessionService;
    }

    public Map<String, String> queueCall(CallRequest request) {
        String customerNumber = trimToNull(request.getCustomerNumber());
        if (customerNumber == null) {
            throw new ResponseStatusException(BAD_REQUEST, "customerNumber is required");
        }

        String provider = trimToNull(request.getProvider());
        if (provider == null) {
            provider = DEFAULT_PROVIDER;
        }

        CallMode callMode = request.getCallMode() == null ? CallMode.AGENT_ASSISTED : CallMode.from(request.getCallMode());
        String ivrFlowId = trimToNull(request.getIvrFlowId());
        String agentId = trimToNull(request.getAgentId());
        String agentChannel = trimToNull(request.getAgentChannel());

        if (callMode == CallMode.AGENT_ASSISTED && agentChannel == null) {
            Agent agent = reserveAgent(agentId);
            agentId = agent.getAgentId();
            agentChannel = agent.getChannel();
        }

        String callSessionId = UUID.randomUUID().toString();
        String campaignId = trimToNull(request.getCampaignId());
        if (campaignId == null) {
            campaignId = DEFAULT_CAMPAIGN_ID;
        }
        CommandMessage command = new CommandMessage();
        command.setCommandId(UUID.randomUUID().toString());
        command.setCommandType(callMode == CallMode.OUTBOUND_IVR ? CommandType.START_IVR_CALL : CommandType.START_CUSTOMER_CALL);
        command.setCallSessionId(callSessionId);
        command.setProvider(provider);
        command.setTimestamp(Instant.now());
        command.setPayload(buildPayload(customerNumber, campaignId, agentId, agentChannel, callSessionId, callMode, ivrFlowId));

        callSessionService.createQueuedSession(
                callSessionId,
                campaignId,
                callSessionId,
                provider,
                customerNumber,
                agentId,
                agentChannel,
                callMode,
                ivrFlowId
        );

        producer.sendCommand(command);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "queued");
        response.put("callSessionId", callSessionId);
        response.put("provider", provider);
        response.put("campaignId", campaignId);
        response.put("customerNumber", customerNumber);
        response.put("agentId", agentId);
        response.put("agentChannel", agentChannel);
        response.put("callMode", callMode.name());
        response.put("ivrFlowId", ivrFlowId);
        return response;
    }

    private Agent reserveAgent(String requestedAgentId) {
        if (requestedAgentId != null) {
            return agentStore.acquireAgent(requestedAgentId)
                    .orElseThrow(() -> new ResponseStatusException(
                            CONFLICT, "Requested agent is not available: " + requestedAgentId));
        }

        return agentStore.acquireAvailableAgent()
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "No available agents"));
    }

    private Map<String, String> buildPayload(String customerNumber,
                                             String campaignId,
                                             String agentId,
                                             String agentChannel,
                                             String callSessionId,
                                             CallMode callMode,
                                             String ivrFlowId) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("customerNumber", customerNumber);
        payload.put("campaignId", campaignId);
        payload.put("leadId", callSessionId);
        payload.put("callMode", callMode.name());
        if (ivrFlowId != null) {
            payload.put("ivrFlowId", ivrFlowId);
        }
        if (agentId != null) {
            payload.put("agentId", agentId);
        }
        if (agentChannel != null) {
            payload.put("agentChannel", agentChannel);
        }
        return payload;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
