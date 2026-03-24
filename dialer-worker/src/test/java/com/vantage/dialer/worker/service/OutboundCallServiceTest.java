package com.vantage.dialer.worker.service;

import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.worker.core.CallSession;
import com.vantage.dialer.worker.core.CallSessionRegistry;
import com.vantage.dialer.worker.core.TelephonyProvider;
import com.vantage.dialer.worker.core.TelephonyProviderRouter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboundCallServiceTest {

    @Test
    void startRegistersSessionPublishesCreatedEventAndStartsCustomerLeg() {
        CallSessionRegistry registry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("ASTERISK")).thenReturn(true);
        TelephonyProviderRouter router = new TelephonyProviderRouter(List.of(provider));
        EventPublisherService eventPublisher = mock(EventPublisherService.class);
        OutboundCallService service = new OutboundCallService(registry, router, eventPublisher);

        service.start("session-1", "919876543210", "ASTERISK", "campaign-1", "lead-1", "AGENT_ASSISTED", null, "agent-1", "PJSIP/1001");

        CallSession session = registry.get("session-1").orElseThrow();
        assertEquals("session-1", session.getCallSessionId());
        assertEquals("campaign-1", session.getCampaignId());
        assertEquals("lead-1", session.getLeadId());
        assertEquals("919876543210", session.getCustomerNumber());
        assertEquals("agent-1", session.getAgentId());
        assertEquals("PJSIP/1001", session.getAgentChannel());
        verify(eventPublisher).publish(same(session), same(EventType.CALL_CREATED));
        verify(provider).startCustomerLeg(same(session));
    }

    @Test
    void startPublishesFailureAndRemovesSessionWhenProviderStartFails() {
        CallSessionRegistry registry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("ASTERISK")).thenReturn(true);
        RuntimeException failure = new RuntimeException("originate failed");
        org.mockito.Mockito.doThrow(failure).when(provider).startCustomerLeg(org.mockito.ArgumentMatchers.any(CallSession.class));
        TelephonyProviderRouter router = new TelephonyProviderRouter(List.of(provider));
        EventPublisherService eventPublisher = mock(EventPublisherService.class);
        OutboundCallService service = new OutboundCallService(registry, router, eventPublisher);

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                service.start("session-1", "919876543210", "ASTERISK", "campaign-1", "lead-1", "AGENT_ASSISTED", null, "agent-1", "PJSIP/1001"));

        assertSame(failure, error);
        assertTrue(registry.get("session-1").isEmpty());
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.any(CallSession.class), same(EventType.CALL_CREATED));
        verify(eventPublisher).publishFailure(org.mockito.ArgumentMatchers.any(CallSession.class), same("AMI_ORIGINATE_FAILED"), same("originate failed"));
    }

    @Test
    void startPublishesFailureAndRemovesSessionWhenProviderIsUnsupported() {
        CallSessionRegistry registry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("ASTERISK")).thenReturn(false);
        TelephonyProviderRouter router = new TelephonyProviderRouter(List.of(provider));
        EventPublisherService eventPublisher = mock(EventPublisherService.class);
        OutboundCallService service = new OutboundCallService(registry, router, eventPublisher);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                service.start("session-2", "919876543211", "ASTERISK", "campaign-2", "lead-2", "AGENT_ASSISTED", null, "agent-2", "PJSIP/1002"));

        assertEquals("Unsupported provider: ASTERISK", error.getMessage());
        assertTrue(registry.get("session-2").isEmpty());
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.any(CallSession.class), same(EventType.CALL_CREATED));
        verify(eventPublisher).publishFailure(
                org.mockito.ArgumentMatchers.any(CallSession.class),
                same("AMI_ORIGINATE_FAILED"),
                org.mockito.ArgumentMatchers.eq("Unsupported provider: ASTERISK")
        );
    }

    @Test
    void dialsAssignedAgentForExistingSession() {
        CallSessionRegistry registry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("ASTERISK")).thenReturn(true);
        TelephonyProviderRouter router = new TelephonyProviderRouter(List.of(provider));
        EventPublisherService eventPublisher = mock(EventPublisherService.class);
        OutboundCallService service = new OutboundCallService(registry, router, eventPublisher);

        CallSession session = new CallSession(
                "session-1",
                "ASTERISK",
                "campaign-1",
                "lead-1",
                "919876543210",
                "AGENT_ASSISTED",
                null,
                null,
                null
        );
        registry.register(session);

        service.dialAgent("session-1", "A1", "PJSIP/1001");

        verify(eventPublisher).publish(session, EventType.AGENT_DIALING);
        verify(provider).startAgentLeg(session);
    }

    @Test
    void dialAgentRejectsUnknownSession() {
        CallSessionRegistry registry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        TelephonyProviderRouter router = new TelephonyProviderRouter(List.of(provider));
        EventPublisherService eventPublisher = mock(EventPublisherService.class);
        OutboundCallService service = new OutboundCallService(registry, router, eventPublisher);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                service.dialAgent("missing-session", "A1", "PJSIP/1001"));

        assertEquals("Unknown call session: missing-session", error.getMessage());
    }

    @Test
    void dialAgentPublishesAndPropagatesUnsupportedProviderAfterUpdatingSession() {
        CallSessionRegistry registry = new CallSessionRegistry();
        TelephonyProvider provider = mock(TelephonyProvider.class);
        when(provider.supports("ASTERISK")).thenReturn(false);
        TelephonyProviderRouter router = new TelephonyProviderRouter(List.of(provider));
        EventPublisherService eventPublisher = mock(EventPublisherService.class);
        OutboundCallService service = new OutboundCallService(registry, router, eventPublisher);

        CallSession session = new CallSession(
                "session-3",
                "ASTERISK",
                "campaign-3",
                "lead-3",
                "919876543212",
                "AGENT_ASSISTED",
                null,
                null,
                null
        );
        registry.register(session);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                service.dialAgent("session-3", "A3", "PJSIP/1003"));

        assertEquals("Unsupported provider: ASTERISK", error.getMessage());
        assertEquals("A3", session.getAgentId());
        assertEquals("PJSIP/1003", session.getAgentChannel());
        verify(eventPublisher).publish(session, EventType.AGENT_DIALING);
    }
}
