package com.vantage.dialer.worker.provider.asterisk.service;

import com.vantage.dialer.worker.core.CallSession;
import com.vantage.dialer.worker.core.CallSessionRegistry;
import com.vantage.dialer.worker.core.TelephonyProvider;
import org.springframework.stereotype.Service;

@Service
public class AsteriskTelephonyProvider implements TelephonyProvider {

    private static final String PROVIDER = "ASTERISK";

    private final AsteriskService asteriskService;
    private final CallSessionRegistry sessionRegistry;

    public AsteriskTelephonyProvider(AsteriskService asteriskService, CallSessionRegistry sessionRegistry) {
        this.asteriskService = asteriskService;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public boolean supports(String provider) {
        return provider != null && PROVIDER.equalsIgnoreCase(provider);
    }

    @Override
    public void startCustomerLeg(CallSession session) {
        String actionId = session.getCallSessionId() + ":customer";
        session.getActionOwners().put(actionId, "customer");
        sessionRegistry.mapAction(actionId, session.getCallSessionId());
        asteriskService.originateCall(session.getCustomerNumber(), actionId);
    }

    @Override
    public void startAgentLeg(CallSession session) {
        if (session.getAgentChannel() == null || session.getAgentChannel().isBlank()) {
            return;
        }

        String actionId = session.getCallSessionId() + ":agent";
        session.setAgentDialRequested(true);
        session.getActionOwners().put(actionId, "agent");
        sessionRegistry.mapAction(actionId, session.getCallSessionId());
        asteriskService.originateChannel(session.getAgentChannel(), actionId);
    }

    @Override
    public void bridge(CallSession session) {
        asteriskService.bridgeChannels(session.getCustomerChannel(), session.getAgentLiveChannel());
    }
}
