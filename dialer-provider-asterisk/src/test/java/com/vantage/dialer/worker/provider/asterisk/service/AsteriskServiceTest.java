package com.vantage.dialer.worker.provider.asterisk.service;

import com.vantage.dialer.worker.provider.asterisk.config.AsteriskProperties;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionState;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.action.BridgeAction;
import org.asteriskjava.manager.action.ManagerAction;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.response.ManagerResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsteriskServiceTest {

    @Test
    void registersExistingListenersAfterReconnect() throws Exception {
        AsteriskProperties properties = buildProperties();
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
    void connectThrowsWhenManualConnectionFails() {
        AsteriskProperties properties = buildProperties();
        IOException failure = new IOException("AMI unavailable");
        AsteriskService service = new AsteriskService(properties, ignored -> {
            throw failure;
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, service::connect);

        assertEquals("Failed to connect to Asterisk AMI at localhost:5038", thrown.getMessage());
        assertSame(failure, thrown.getCause());
    }

    @Test
    void disconnectLogsOffAndClearsConnection() throws Exception {
        AsteriskProperties properties = buildProperties();
        ManagerConnection managerConnection = connectedConnection();
        AsteriskService service = new AsteriskService(properties, ignored -> managerConnection);
        injectManagerConnection(service, managerConnection);

        service.disconnect();

        verify(managerConnection).logoff();
        assertNull(currentManagerConnection(service));
    }

    @Test
    void addEventListenerRegistersImmediatelyWhenConnected() throws Exception {
        AsteriskProperties properties = buildProperties();
        ManagerConnection managerConnection = connectedConnection();
        AsteriskService service = new AsteriskService(properties, ignored -> managerConnection);
        injectManagerConnection(service, managerConnection);
        ManagerEventListener listener = event -> { };

        service.addEventListener(listener);

        verify(managerConnection).addEventListener(listener);
    }

    @Test
    void originateCallBuildsOriginateActionAndReturnsResponse() throws Exception {
        AsteriskProperties properties = buildProperties();
        properties.setCallerId("Support Desk");
        properties.setOriginateTimeoutMs(15000L);
        ManagerConnection managerConnection = connectedConnection();
        ManagerResponse response = mock(ManagerResponse.class);
        when(response.getResponse()).thenReturn("Success");
        when(managerConnection.sendAction(any(ManagerAction.class), eq(15000L))).thenReturn(response);

        AsteriskService service = new AsteriskService(properties, ignored -> managerConnection);
        injectManagerConnection(service, managerConnection);

        ManagerResponse actual = service.originateCall("(987) 654-3210", "call-1");

        assertSame(response, actual);

        ArgumentCaptor<ManagerAction> actionCaptor = ArgumentCaptor.forClass(ManagerAction.class);
        verify(managerConnection).sendAction(actionCaptor.capture(), eq(15000L));
        OriginateAction action = (OriginateAction) actionCaptor.getValue();
        assertEquals("call-1", action.getActionId());
        assertEquals(Boolean.TRUE, action.getAsync());
        assertEquals("PJSIP/19876543210@voice-endpoint", action.getChannel());
        assertEquals("from-internal", action.getContext());
        assertEquals("s", action.getExten());
        assertEquals(Integer.valueOf(1), action.getPriority());
        assertEquals(Long.valueOf(15000L), action.getTimeout());
        assertEquals("Support Desk", action.getCallerId());
    }

    @Test
    void originateChannelOmitsBlankCallerId() throws Exception {
        AsteriskProperties properties = buildProperties();
        properties.setCallerId(" ");
        properties.setOriginateTimeoutMs(7000L);
        ManagerConnection managerConnection = connectedConnection();
        ManagerResponse response = mock(ManagerResponse.class);
        when(response.getResponse()).thenReturn("Success");
        when(managerConnection.sendAction(any(ManagerAction.class), eq(7000L))).thenReturn(response);

        AsteriskService service = new AsteriskService(properties, ignored -> managerConnection);
        injectManagerConnection(service, managerConnection);

        service.originateChannel("PJSIP/agent-1001", "agent-1");

        ArgumentCaptor<ManagerAction> actionCaptor = ArgumentCaptor.forClass(ManagerAction.class);
        verify(managerConnection).sendAction(actionCaptor.capture(), eq(7000L));
        OriginateAction action = (OriginateAction) actionCaptor.getValue();
        assertEquals("agent-1", action.getActionId());
        assertEquals("PJSIP/agent-1001", action.getChannel());
        assertNull(action.getCallerId());
    }

    @Test
    void originateCallWrapsSendActionFailures() throws Exception {
        AsteriskProperties properties = buildProperties();
        ManagerConnection managerConnection = connectedConnection();
        IOException failure = new IOException("socket closed");
        when(managerConnection.sendAction(any(ManagerAction.class), anyLong())).thenThrow(failure);

        AsteriskService service = new AsteriskService(properties, ignored -> managerConnection);
        injectManagerConnection(service, managerConnection);

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> service.originateCall("+1 (555) 000-1", "call-2")
        );

        assertEquals(
                "Failed to originate call for +1 (555) 000-1 via Asterisk AMI",
                thrown.getMessage()
        );
        assertSame(failure, thrown.getCause());
    }

    @Test
    void bridgeChannelsBuildsBridgeActionAndReturnsResponse() throws Exception {
        AsteriskProperties properties = buildProperties();
        properties.setOriginateTimeoutMs(9000L);
        ManagerConnection managerConnection = connectedConnection();
        ManagerResponse response = mock(ManagerResponse.class);
        when(response.getResponse()).thenReturn("Success");
        when(managerConnection.sendAction(any(ManagerAction.class), eq(9000L))).thenReturn(response);

        AsteriskService service = new AsteriskService(properties, ignored -> managerConnection);
        injectManagerConnection(service, managerConnection);

        ManagerResponse actual = service.bridgeChannels("customer-channel", "agent-channel");

        assertSame(response, actual);

        ArgumentCaptor<ManagerAction> actionCaptor = ArgumentCaptor.forClass(ManagerAction.class);
        verify(managerConnection).sendAction(actionCaptor.capture(), eq(9000L));
        BridgeAction action = (BridgeAction) actionCaptor.getValue();
        assertEquals("customer-channel", action.getChannel1());
        assertEquals("agent-channel", action.getChannel2());
    }

    @Test
    void buildChannelUsingTypedProperties() {
        AsteriskProperties properties = buildProperties();
        properties.setDialPrefix("1");
        properties.setEndpoint("voice-endpoint");

        AsteriskService service = new AsteriskService(properties, ignored -> mock(ManagerConnection.class));

        assertEquals("PJSIP/19876543210@voice-endpoint", service.buildChannel("(987) 654-3210"));
    }

    @Test
    void buildChannelValidatesPhoneNumber() {
        AsteriskService service = new AsteriskService(buildProperties(), ignored -> mock(ManagerConnection.class));

        IllegalArgumentException missingNumber = assertThrows(
                IllegalArgumentException.class,
                () -> service.buildChannel(null)
        );
        IllegalArgumentException noDigits = assertThrows(
                IllegalArgumentException.class,
                () -> service.buildChannel("dial-this")
        );

        assertEquals("phoneNumber is required", missingNumber.getMessage());
        assertEquals("phoneNumber must contain digits", noDigits.getMessage());
    }

    private AsteriskProperties buildProperties() {
        AsteriskProperties properties = new AsteriskProperties();
        properties.setHost("localhost");
        properties.setPort(5038);
        properties.setUsername("admin");
        properties.setPassword("secret");
        properties.setEndpoint("voice-endpoint");
        properties.setDialPrefix("1");
        properties.setCallerId("Caller");
        properties.setOriginateContext("from-internal");
        properties.setOriginateExtension("s");
        properties.setOriginatePriority(1);
        properties.setOriginateTimeoutMs(30000L);
        return properties;
    }

    private ManagerConnection connectedConnection() {
        ManagerConnection managerConnection = mock(ManagerConnection.class);
        when(managerConnection.getState()).thenReturn(ManagerConnectionState.CONNECTED);
        return managerConnection;
    }

    private void injectManagerConnection(AsteriskService service, ManagerConnection managerConnection)
            throws ReflectiveOperationException {
        Field field = AsteriskService.class.getDeclaredField("managerConnection");
        field.setAccessible(true);
        field.set(service, managerConnection);
    }

    private ManagerConnection currentManagerConnection(AsteriskService service)
            throws ReflectiveOperationException {
        Field field = AsteriskService.class.getDeclaredField("managerConnection");
        field.setAccessible(true);
        return (ManagerConnection) field.get(service);
    }
}
