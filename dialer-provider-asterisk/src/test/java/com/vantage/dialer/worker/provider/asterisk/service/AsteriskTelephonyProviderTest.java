package com.vantage.dialer.worker.provider.asterisk.service;

import com.vantage.dialer.worker.core.CallSession;
import com.vantage.dialer.worker.core.CallSessionRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AsteriskTelephonyProviderTest {

    @Test
    void supportsMatchesAsteriskCaseInsensitively() {
        AsteriskService asteriskService = mock(AsteriskService.class);
        CallSessionRegistry sessionRegistry = new CallSessionRegistry();
        AsteriskTelephonyProvider provider = new AsteriskTelephonyProvider(asteriskService, sessionRegistry);

        assertTrue(provider.supports("ASTERISK"));
        assertTrue(provider.supports("asterisk"));
        assertFalse(provider.supports(null));
        assertFalse(provider.supports("TWILIO"));
    }

    @Test
    void startCustomerLegMapsActionOwnerAndOriginatesCall() {
        AsteriskService asteriskService = mock(AsteriskService.class);
        CallSessionRegistry sessionRegistry = new CallSessionRegistry();
        AsteriskTelephonyProvider provider = new AsteriskTelephonyProvider(asteriskService, sessionRegistry);
        CallSession session = session("session-1", "PJSIP/1001");

        sessionRegistry.register(session);
        provider.startCustomerLeg(session);

        assertEquals("customer", session.getActionOwners().get("session-1:customer"));
        assertSame(session, sessionRegistry.findByAction("session-1:customer").orElseThrow());
        verify(asteriskService).originateCall("+15550001", "session-1:customer");
    }

    @Test
    void startAgentLegSkipsWhenAgentChannelIsBlank() {
        AsteriskService asteriskService = mock(AsteriskService.class);
        CallSessionRegistry sessionRegistry = new CallSessionRegistry();
        AsteriskTelephonyProvider provider = new AsteriskTelephonyProvider(asteriskService, sessionRegistry);
        CallSession session = session("session-2", " ");

        sessionRegistry.register(session);
        provider.startAgentLeg(session);

        assertFalse(session.isAgentDialRequested());
        assertTrue(sessionRegistry.findByAction("session-2:agent").isEmpty());
        verifyNoInteractions(asteriskService);
    }

    @Test
    void startAgentLegMapsActionOwnerAndOriginatesAgentChannel() {
        AsteriskService asteriskService = mock(AsteriskService.class);
        CallSessionRegistry sessionRegistry = new CallSessionRegistry();
        AsteriskTelephonyProvider provider = new AsteriskTelephonyProvider(asteriskService, sessionRegistry);
        CallSession session = session("session-3", "PJSIP/1001");

        sessionRegistry.register(session);
        provider.startAgentLeg(session);

        assertTrue(session.isAgentDialRequested());
        assertEquals("agent", session.getActionOwners().get("session-3:agent"));
        assertSame(session, sessionRegistry.findByAction("session-3:agent").orElseThrow());
        verify(asteriskService).originateChannel("PJSIP/1001", "session-3:agent");
    }

    @Test
    void bridgeDelegatesToAsteriskService() {
        AsteriskService asteriskService = mock(AsteriskService.class);
        CallSessionRegistry sessionRegistry = new CallSessionRegistry();
        AsteriskTelephonyProvider provider = new AsteriskTelephonyProvider(asteriskService, sessionRegistry);
        CallSession session = session("session-4", "PJSIP/1001");
        session.setCustomerChannel("customer-channel");
        session.setAgentLiveChannel("agent-live-channel");

        provider.bridge(session);

        verify(asteriskService).bridgeChannels("customer-channel", "agent-live-channel");
    }

    private CallSession session(String callSessionId, String agentChannel) {
        return new CallSession(
                callSessionId,
                "ASTERISK",
                "campaign-1",
                "lead-1",
                "+15550001",
                "AGENT_ASSISTED",
                null,
                "agent-1",
                agentChannel
        );
    }
}
