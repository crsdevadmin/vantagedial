package com.vantage.dialer.worker.provider.asterisk.service;

import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.worker.core.CallEventPublisher;
import com.vantage.dialer.worker.core.CallSession;
import com.vantage.dialer.worker.core.CallSessionRegistry;
import com.vantage.dialer.worker.core.TelephonyProvider;
import com.vantage.dialer.worker.core.TelephonyProviderRouter;
import com.vantage.dialer.worker.provider.asterisk.config.AsteriskProperties;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionState;
import org.asteriskjava.manager.event.BridgeEvent;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.NewStateEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsteriskEventListenerTest {

    @Test
    void handlesAnsweredEventWhenAsteriskReportsChannelLegSuffix() {
        CallSessionRegistry sessionRegistry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("ASTERISK")).thenReturn(true);
        TelephonyProviderRouter providerRouter = new TelephonyProviderRouter(List.of(provider));
        CallEventPublisher eventPublisher = mock(CallEventPublisher.class);
        ManagerConnection managerConnection = mock(ManagerConnection.class);
        when(managerConnection.getState()).thenReturn(ManagerConnectionState.CONNECTED);

        AsteriskService asteriskService = new AsteriskService(buildProperties(), ignored -> managerConnection);
        injectManagerConnection(asteriskService, managerConnection);

        AsteriskEventListener listener = new AsteriskEventListener(
                sessionRegistry,
                providerRouter,
                eventPublisher,
                asteriskService
        );

        CallSession session = new CallSession(
                "session-1",
                "ASTERISK",
                "campaign-1",
                "lead-1",
                "9876543210",
                "agent-1",
                "PJSIP/agent-00000002"
        );
        session.setCustomerChannel("PJSIP/919876543210@vivphone-endpoint-00000001");
        sessionRegistry.register(session);
        sessionRegistry.mapChannel(session.getCustomerChannel(), session.getCallSessionId());

        NewStateEvent event = new NewStateEvent(this);
        event.setChannel("PJSIP/919876543210@vivphone-endpoint-00000001;2");
        event.setChannelStateDesc("Up");

        listener.onManagerEvent(event);

        verify(eventPublisher).publish(eq(session), eq(EventType.CUSTOMER_ANSWERED));
        verify(eventPublisher).publish(eq(session), eq(EventType.AGENT_DIALING));
        verify(provider).startAgentLeg(session);
    }

    @Test
    void publishesFailureWhenCustomerLegHangsUpBeforeAnswer() {
        CallSessionRegistry sessionRegistry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("ASTERISK")).thenReturn(true);
        TelephonyProviderRouter providerRouter = new TelephonyProviderRouter(List.of(provider));
        CallEventPublisher eventPublisher = mock(CallEventPublisher.class);
        ManagerConnection managerConnection = mock(ManagerConnection.class);
        when(managerConnection.getState()).thenReturn(ManagerConnectionState.CONNECTED);

        AsteriskService asteriskService = new AsteriskService(buildProperties(), ignored -> managerConnection);
        injectManagerConnection(asteriskService, managerConnection);

        AsteriskEventListener listener = new AsteriskEventListener(
                sessionRegistry,
                providerRouter,
                eventPublisher,
                asteriskService
        );

        CallSession session = new CallSession(
                "session-2",
                "ASTERISK",
                "campaign-1",
                "lead-2",
                "9876543211",
                "agent-1",
                "PJSIP/agent-00000002"
        );
        session.setCustomerChannel("PJSIP/919876543211@vivphone-endpoint-00000005");
        sessionRegistry.register(session);
        sessionRegistry.mapChannel(session.getCustomerChannel(), session.getCallSessionId());

        HangupEvent event = new HangupEvent(this);
        event.setChannel("PJSIP/919876543211@vivphone-endpoint-00000005;2");
        event.setCause(16);
        event.setCauseTxt("Normal Clearing");

        listener.onManagerEvent(event);

        verify(eventPublisher).publishFailure(
                eq(session),
                eq("CUSTOMER_HANGUP_BEFORE_ANSWER"),
                argThat(error -> error instanceof Map<?, ?> payload
                        && "PJSIP/919876543211@vivphone-endpoint-00000005;2".equals(payload.get("channel"))
                        && Integer.valueOf(16).equals(payload.get("cause"))
                        && "Normal Clearing".equals(payload.get("causeTxt")))
        );
        verify(eventPublisher, never()).publish(eq(session), eq(EventType.CALL_COMPLETED));
    }

    @Test
    void publishesCompletedWhenBridgedCallHangsUp() {
        CallSessionRegistry sessionRegistry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("ASTERISK")).thenReturn(true);
        TelephonyProviderRouter providerRouter = new TelephonyProviderRouter(List.of(provider));
        CallEventPublisher eventPublisher = mock(CallEventPublisher.class);
        ManagerConnection managerConnection = mock(ManagerConnection.class);
        when(managerConnection.getState()).thenReturn(ManagerConnectionState.CONNECTED);

        AsteriskService asteriskService = new AsteriskService(buildProperties(), ignored -> managerConnection);
        injectManagerConnection(asteriskService, managerConnection);

        AsteriskEventListener listener = new AsteriskEventListener(
                sessionRegistry,
                providerRouter,
                eventPublisher,
                asteriskService
        );

        CallSession session = new CallSession(
                "session-3",
                "ASTERISK",
                "campaign-1",
                "lead-3",
                "9876543212",
                "agent-1",
                "PJSIP/agent-00000002"
        );
        session.setCustomerChannel("PJSIP/919876543212@vivphone-endpoint-00000008");
        session.setAgentLiveChannel("PJSIP/agent-00000002");
        session.setCustomerAnswered(true);
        session.setAgentAnswered(true);
        session.setBridged(true);
        sessionRegistry.register(session);
        sessionRegistry.mapChannel(session.getCustomerChannel(), session.getCallSessionId());
        sessionRegistry.mapChannel(session.getAgentLiveChannel(), session.getCallSessionId());

        HangupEvent event = new HangupEvent(this);
        event.setChannel("PJSIP/agent-00000002;1");

        listener.onManagerEvent(event);

        verify(eventPublisher).publish(eq(session), eq(EventType.CALL_COMPLETED));
        verify(eventPublisher, never()).publishFailure(eq(session), eq("CALL_TERMINATED_BEFORE_BRIDGE"), argThat(arg -> true));
    }

    @Test
    void publishesCallBridgedWhenBridgeEventUsesChannelLegSuffixes() {
        CallSessionRegistry sessionRegistry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("ASTERISK")).thenReturn(true);
        TelephonyProviderRouter providerRouter = new TelephonyProviderRouter(List.of(provider));
        CallEventPublisher eventPublisher = mock(CallEventPublisher.class);
        ManagerConnection managerConnection = mock(ManagerConnection.class);
        when(managerConnection.getState()).thenReturn(ManagerConnectionState.CONNECTED);

        AsteriskService asteriskService = new AsteriskService(buildProperties(), ignored -> managerConnection);
        injectManagerConnection(asteriskService, managerConnection);

        AsteriskEventListener listener = new AsteriskEventListener(
                sessionRegistry,
                providerRouter,
                eventPublisher,
                asteriskService
        );

        CallSession session = new CallSession(
                "session-4",
                "ASTERISK",
                "campaign-1",
                "lead-4",
                "9876543213",
                "agent-1",
                "PJSIP/agent-00000003"
        );
        session.setCustomerChannel("PJSIP/919876543213@vivphone-endpoint-00000009");
        session.setAgentLiveChannel("PJSIP/agent-00000003");
        session.setCustomerAnswered(true);
        session.setAgentAnswered(true);
        sessionRegistry.register(session);
        sessionRegistry.mapChannel(session.getCustomerChannel(), session.getCallSessionId());
        sessionRegistry.mapChannel(session.getAgentLiveChannel(), session.getCallSessionId());

        BridgeEvent event = new BridgeEvent(this);
        event.setChannel1("PJSIP/919876543213@vivphone-endpoint-00000009;2");
        event.setChannel2("PJSIP/agent-00000003;1");

        listener.onManagerEvent(event);

        verify(eventPublisher).publish(eq(session), eq(EventType.CALL_BRIDGED));
    }

    private AsteriskProperties buildProperties() {
        AsteriskProperties properties = new AsteriskProperties();
        properties.setHost("localhost");
        properties.setPort(5038);
        properties.setUsername("admin");
        properties.setPassword("secret");
        properties.setEndpoint("vivphone-endpoint");
        properties.setDialPrefix("91");
        properties.setOriginateContext("from-internal");
        properties.setOriginateExtension("s");
        properties.setOriginatePriority(1);
        properties.setOriginateTimeoutMs(30000L);
        return properties;
    }

    private void injectManagerConnection(AsteriskService asteriskService, ManagerConnection managerConnection) {
        try {
            var field = AsteriskService.class.getDeclaredField("managerConnection");
            field.setAccessible(true);
            field.set(asteriskService, managerConnection);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
