package com.vantage.dialer.worker.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

class CallSessionRegistryTest {

    @Test
    void findsSessionWhenAsteriskChannelVariantUsesLegSuffix() {
        CallSessionRegistry registry = new CallSessionRegistry();
        CallSession session = new CallSession(
                "session-1",
                "ASTERISK",
                "campaign-1",
                "lead-1",
                "9876543210",
                "AGENT_ASSISTED",
                null,
                "agent-1",
                "PJSIP/agent-00000002"
        );

        registry.register(session);
        registry.mapChannel("PJSIP/919876543210@vivphone-endpoint-00000001", session.getCallSessionId());

        CallSession resolved = registry.findByChannel("PJSIP/919876543210@vivphone-endpoint-00000001;2")
                .orElseThrow();

        assertSame(session, resolved);
    }

    @Test
    void mapsActionsAndRemoveClearsAllIndexes() {
        CallSessionRegistry registry = new CallSessionRegistry();
        CallSession session = new CallSession(
                "session-2",
                "ASTERISK",
                "campaign-1",
                "lead-2",
                "9876543211",
                "AGENT_ASSISTED",
                null,
                "agent-1",
                "PJSIP/agent-00000003"
        );

        registry.register(session);
        registry.mapAction("session-2:customer", session.getCallSessionId());
        registry.mapChannel("PJSIP/919876543211@vivphone-endpoint-00000005", session.getCallSessionId());

        assertSame(session, registry.findByAction("session-2:customer").orElseThrow());
        assertSame(session, registry.findByChannel("PJSIP/919876543211@vivphone-endpoint-00000005").orElseThrow());

        registry.remove("session-2");

        assertTrue(registry.get("session-2").isEmpty());
        assertTrue(registry.findByAction("session-2:customer").isEmpty());
        assertTrue(registry.findByChannel("PJSIP/919876543211@vivphone-endpoint-00000005").isEmpty());
    }

    @Test
    void ignoresNullAndBlankChannels() {
        CallSessionRegistry registry = new CallSessionRegistry();
        CallSession session = new CallSession(
                "session-3",
                "ASTERISK",
                "campaign-1",
                "lead-3",
                "9876543212",
                "AGENT_ASSISTED",
                null,
                "agent-1",
                "PJSIP/agent-00000004"
        );

        registry.register(session);
        registry.mapChannel(null, session.getCallSessionId());
        registry.mapChannel("   ", session.getCallSessionId());

        assertTrue(registry.findByChannel(null).isEmpty());
        assertTrue(registry.findByChannel("   ").isEmpty());
    }
}
