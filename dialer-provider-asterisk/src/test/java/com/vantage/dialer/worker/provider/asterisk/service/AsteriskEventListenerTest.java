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
import org.asteriskjava.manager.event.OriginateResponseEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AsteriskEventListenerTest {

    @Test
    void publishesFailureAndRemovesSessionWhenOriginateFails() {
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

        CallSession session = session("session-failure", "9876543219", "PJSIP/agent-00000010");
        session.getActionOwners().put("session-failure:customer", "customer");
        sessionRegistry.register(session);
        sessionRegistry.mapAction("session-failure:customer", session.getCallSessionId());

        OriginateResponseEvent event = new OriginateResponseEvent(this);
        event.setActionId("session-failure:customer");
        event.setResponse("Failure");
        event.setReason(17);

        listener.onManagerEvent(event);

        verify(eventPublisher).publishFailure(eq(session), eq("AMI_ORIGINATE_FAILED"), eq(17));
        assertTrue(sessionRegistry.get(session.getCallSessionId()).isEmpty());
    }

    @Test
    void publishesAgentRingingWhenAgentOriginateSucceeds() {
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

        CallSession session = session("session-agent", "9876543218", "PJSIP/agent-00000011");
        session.getActionOwners().put("session-agent:agent", "agent");
        sessionRegistry.register(session);
        sessionRegistry.mapAction("session-agent:agent", session.getCallSessionId());

        OriginateResponseEvent event = new OriginateResponseEvent(this);
        event.setActionId("session-agent:agent");
        event.setResponse("Success");
        event.setChannel("PJSIP/agent-00000011;1");

        listener.onManagerEvent(event);

        assertEquals("PJSIP/agent-00000011;1", session.getAgentLiveChannel());
        verify(eventPublisher).publish(eq(session), eq(EventType.AGENT_RINGING));
    }

    @Test
    void publishesCustomerRingingWhenCustomerOriginateSucceeds() {
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

        CallSession session = session("session-customer", "9876543216", "PJSIP/agent-00000013");
        session.getActionOwners().put("session-customer:customer", "customer");
        sessionRegistry.register(session);
        sessionRegistry.mapAction("session-customer:customer", session.getCallSessionId());

        OriginateResponseEvent event = new OriginateResponseEvent(this);
        event.setActionId("session-customer:customer");
        event.setResponse("Success");
        event.setChannel("PJSIP/919876543216@vivphone-endpoint-00000011;1");

        listener.onManagerEvent(event);

        assertEquals("PJSIP/919876543216@vivphone-endpoint-00000011;1", session.getCustomerChannel());
        assertTrue(sessionRegistry.findByChannel("PJSIP/919876543216@vivphone-endpoint-00000011;2").isPresent());
        verify(eventPublisher).publish(eq(session), eq(EventType.CUSTOMER_RINGING));
    }

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
                "AGENT_ASSISTED",
                null,
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
    void ignoresDuplicateCustomerAnsweredEvents() {
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

        CallSession session = session("session-duplicate", "9876543215", "PJSIP/agent-00000014");
        session.setCustomerChannel("PJSIP/919876543215@vivphone-endpoint-00000012");
        session.setCustomerAnswered(true);
        sessionRegistry.register(session);
        sessionRegistry.mapChannel(session.getCustomerChannel(), session.getCallSessionId());

        NewStateEvent event = new NewStateEvent(this);
        event.setChannel("PJSIP/919876543215@vivphone-endpoint-00000012;3");
        event.setChannelStateDesc("Up");

        listener.onManagerEvent(event);

        verifyNoInteractions(eventPublisher);
        verify(provider, never()).startAgentLeg(session);
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
                "AGENT_ASSISTED",
                null,
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
                "AGENT_ASSISTED",
                null,
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
    void publishesAgentAnsweredWithoutBridgeWhenCustomerHasNotAnswered() {
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

        CallSession session = session("session-agent-first", "9876543214", "PJSIP/agent-00000015");
        session.setAgentLiveChannel("PJSIP/agent-00000015");
        sessionRegistry.register(session);
        sessionRegistry.mapChannel(session.getAgentLiveChannel(), session.getCallSessionId());

        NewStateEvent event = new NewStateEvent(this);
        event.setChannel("PJSIP/agent-00000015;4");
        event.setChannelStateDesc("Up");

        listener.onManagerEvent(event);

        verify(eventPublisher).publish(eq(session), eq(EventType.AGENT_ANSWERED));
        verify(provider, never()).bridge(session);
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
                "AGENT_ASSISTED",
                null,
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

    @Test
    void bridgesCallWhenAgentAnswersAfterCustomerAlreadyAnswered() {
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

        CallSession session = session("session-bridge", "9876543217", "PJSIP/agent-00000012");
        session.setCustomerChannel("PJSIP/919876543217@vivphone-endpoint-00000010");
        session.setAgentLiveChannel("PJSIP/agent-00000012");
        session.setCustomerAnswered(true);
        sessionRegistry.register(session);
        sessionRegistry.mapChannel(session.getAgentLiveChannel(), session.getCallSessionId());

        NewStateEvent event = new NewStateEvent(this);
        event.setChannel("PJSIP/agent-00000012;2");
        event.setChannelStateDesc("Up");

        listener.onManagerEvent(event);

        verify(eventPublisher).publish(eq(session), eq(EventType.AGENT_ANSWERED));
        verify(provider).bridge(session);
    }

    @Test
    void publishesAgentHangupBeforeAnswerFailure() {
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

        CallSession session = session("session-agent-hangup", "9876543213", "PJSIP/agent-00000016");
        session.setAgentLiveChannel("PJSIP/agent-00000016");
        sessionRegistry.register(session);
        sessionRegistry.mapChannel(session.getAgentLiveChannel(), session.getCallSessionId());

        HangupEvent event = new HangupEvent(this);
        event.setChannel("PJSIP/agent-00000016;5");
        event.setCause(17);
        event.setCauseTxt("User Busy");

        listener.onManagerEvent(event);

        verify(eventPublisher).publishFailure(
                eq(session),
                eq("AGENT_HANGUP_BEFORE_ANSWER"),
                argThat(error -> error instanceof Map<?, ?> payload
                        && "PJSIP/agent-00000016;5".equals(payload.get("channel"))
                        && Integer.valueOf(17).equals(payload.get("cause"))
                        && "User Busy".equals(payload.get("causeTxt")))
        );
    }

    @Test
    void publishesCallTerminatedBeforeBridgeWhenAnsweredCustomerHangsUp() {
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

        CallSession session = session("session-pre-bridge", "9876543212", "PJSIP/agent-00000017");
        session.setCustomerChannel("PJSIP/919876543212@vivphone-endpoint-00000013");
        session.setCustomerAnswered(true);
        sessionRegistry.register(session);
        sessionRegistry.mapChannel(session.getCustomerChannel(), session.getCallSessionId());

        HangupEvent event = new HangupEvent(this);
        event.setChannel("PJSIP/919876543212@vivphone-endpoint-00000013;6");
        event.setCause(18);
        event.setCauseTxt("No User Responding");

        listener.onManagerEvent(event);

        verify(eventPublisher).publishFailure(
                eq(session),
                eq("CALL_TERMINATED_BEFORE_BRIDGE"),
                argThat(error -> error instanceof Map<?, ?> payload
                        && "PJSIP/919876543212@vivphone-endpoint-00000013;6".equals(payload.get("channel"))
                        && Integer.valueOf(18).equals(payload.get("cause"))
                        && "No User Responding".equals(payload.get("causeTxt")))
        );
    }

    private CallSession session(String callSessionId, String customerNumber, String agentChannel) {
        return new CallSession(
                callSessionId,
                "ASTERISK",
                "campaign-1",
                "lead-" + callSessionId,
                customerNumber,
                "AGENT_ASSISTED",
                null,
                "agent-1",
                agentChannel
        );
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
