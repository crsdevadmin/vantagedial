package com.vantage.dialer.worker.core;

import org.junit.jupiter.api.Test;

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
                "agent-1",
                "PJSIP/agent-00000002"
        );

        registry.register(session);
        registry.mapChannel("PJSIP/919876543210@vivphone-endpoint-00000001", session.getCallSessionId());

        CallSession resolved = registry.findByChannel("PJSIP/919876543210@vivphone-endpoint-00000001;2")
                .orElseThrow();

        assertSame(session, resolved);
    }
}
