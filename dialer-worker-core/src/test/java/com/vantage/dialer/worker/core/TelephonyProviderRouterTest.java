package com.vantage.dialer.worker.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelephonyProviderRouterTest {

    @Test
    void resolveReturnsFirstProviderThatSupportsRequestedName() {
        TelephonyProvider first = mock(TelephonyProvider.class);
        TelephonyProvider second = mock(TelephonyProvider.class);
        when(first.supports("ASTERISK")).thenReturn(false);
        when(second.supports("ASTERISK")).thenReturn(true);

        TelephonyProviderRouter router = new TelephonyProviderRouter(List.of(first, second));

        TelephonyProvider resolved = router.resolve("ASTERISK");

        assertSame(second, resolved);
    }

    @Test
    void resolveRejectsUnsupportedProvider() {
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("TWILIO")).thenReturn(false);
        TelephonyProviderRouter router = new TelephonyProviderRouter(List.of(provider));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> router.resolve("TWILIO"));

        assertEquals("Unsupported provider: TWILIO", error.getMessage());
    }
}
