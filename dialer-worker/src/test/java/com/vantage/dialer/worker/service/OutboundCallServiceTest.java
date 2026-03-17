package com.vantage.dialer.worker.service;

import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.worker.core.CallSession;
import com.vantage.dialer.worker.core.CallSessionRegistry;
import com.vantage.dialer.worker.core.TelephonyProvider;
import com.vantage.dialer.worker.core.TelephonyProviderRouter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboundCallServiceTest {

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
                null,
                null
        );
        registry.register(session);

        service.dialAgent("session-1", "A1", "PJSIP/1001");

        verify(eventPublisher).publish(session, EventType.AGENT_DIALING);
        verify(provider).startAgentLeg(session);
    }
}
