package com.vantage.dialer.worker.service;

import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.worker.core.CallSession;
import com.vantage.dialer.worker.core.CallSessionRegistry;
import com.vantage.dialer.worker.core.TelephonyProviderRouter;
import org.springframework.stereotype.Service;

@Service
public class OutboundCallService {

    private final CallSessionRegistry sessionRegistry;
    private final TelephonyProviderRouter providerRouter;
    private final EventPublisherService eventPublisherService;

    public OutboundCallService(CallSessionRegistry sessionRegistry,
                               TelephonyProviderRouter providerRouter,
                               EventPublisherService eventPublisherService) {
        this.sessionRegistry = sessionRegistry;
        this.providerRouter = providerRouter;
        this.eventPublisherService = eventPublisherService;
    }

    public void start(String callSessionId,
                      String customerNumber,
                      String provider,
                      String campaignId,
                      String leadId,
                      String agentId,
                      String agentChannel) {
        CallSession session = new CallSession(
                callSessionId,
                provider,
                campaignId,
                leadId,
                customerNumber,
                agentId,
                agentChannel
        );
        sessionRegistry.register(session);

        try {
            eventPublisherService.publish(session, com.vantage.dialer.common.events.EventType.CALL_CREATED);
            providerRouter.resolve(provider).startCustomerLeg(session);
        } catch (RuntimeException ex) {
            eventPublisherService.publishFailure(session, "AMI_ORIGINATE_FAILED", ex.getMessage());
            sessionRegistry.remove(callSessionId);
            throw ex;
        }
    }

    public void dialAgent(String callSessionId, String agentId, String agentChannel) {
        CallSession session = sessionRegistry.get(callSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown call session: " + callSessionId));

        session.setAgentId(agentId);
        session.setAgentChannel(agentChannel);
        eventPublisherService.publish(session, EventType.AGENT_DIALING);
        providerRouter.resolve(session.getProvider()).startAgentLeg(session);
    }
}
