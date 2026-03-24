package com.vantage.dialer.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CallModeTest {

    @Test
    void fromDefaultsToAgentAssistedForNullOrBlankValues() {
        assertEquals(CallMode.AGENT_ASSISTED, CallMode.from(null));
        assertEquals(CallMode.AGENT_ASSISTED, CallMode.from(""));
        assertEquals(CallMode.AGENT_ASSISTED, CallMode.from("   "));
    }

    @Test
    void fromNormalizesCaseAndWhitespace() {
        assertEquals(CallMode.AGENT_ASSISTED, CallMode.from("agent_assisted"));
        assertEquals(CallMode.OUTBOUND_IVR, CallMode.from(" outbound_ivr "));
    }

    @Test
    void fromRejectsUnknownModes() {
        assertThrows(IllegalArgumentException.class, () -> CallMode.from("predictive"));
    }
}
