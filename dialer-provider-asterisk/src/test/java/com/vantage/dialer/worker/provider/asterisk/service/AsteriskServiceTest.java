package com.vantage.dialer.worker.provider.asterisk.service;

import com.vantage.dialer.worker.provider.asterisk.config.AsteriskProperties;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionState;
import org.asteriskjava.manager.ManagerEventListener;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsteriskServiceTest {

    @Test
    void registersExistingListenersAfterReconnect() throws Exception {
        AsteriskProperties properties = new AsteriskProperties();
        ManagerConnection firstConnection = mock(ManagerConnection.class);
        ManagerConnection secondConnection = mock(ManagerConnection.class);
        when(firstConnection.getState()).thenReturn(ManagerConnectionState.DISCONNECTED);
        when(secondConnection.getState()).thenReturn(ManagerConnectionState.CONNECTED);

        AtomicInteger connectionAttempts = new AtomicInteger();
        AsteriskService service = new AsteriskService(properties, ignored -> {
            if (connectionAttempts.getAndIncrement() == 0) {
                return firstConnection;
            }
            return secondConnection;
        });

        ManagerEventListener listener = event -> { };
        service.addEventListener(listener);

        service.initializeOnStartup();
        service.reconnectIfDisconnected();

        verify(firstConnection).login();
        verify(firstConnection).logoff();
        verify(secondConnection).login();
        verify(secondConnection).addEventListener(listener);
        assertEquals(2, connectionAttempts.get());
    }

    @Test
    void buildsChannelUsingTypedProperties() {
        AsteriskProperties properties = new AsteriskProperties();
        properties.setDialPrefix("1");
        properties.setEndpoint("voice-endpoint");

        AsteriskService service = new AsteriskService(properties, ignored -> mock(ManagerConnection.class));

        assertEquals("PJSIP/19876543210@voice-endpoint", service.buildChannel("(987) 654-3210"));
    }
}
