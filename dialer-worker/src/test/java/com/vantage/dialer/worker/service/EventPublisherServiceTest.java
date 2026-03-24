package com.vantage.dialer.worker.service;

import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import com.vantage.dialer.worker.core.CallSession;
import com.vantage.dialer.worker.kafka.EventProducer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventPublisherServiceTest {

    @Test
    void publishBuildsStandardEventFromSessionStateAndExtraPayload() {
        EventProducer eventProducer = mock(EventProducer.class);
        EventPublisherService service = new EventPublisherService(eventProducer);
        CallSession session = session();
        session.setCustomerChannel("customer-channel");
        session.setAgentLiveChannel("agent-live-channel");

        service.publish(session, EventType.CALL_BRIDGED, java.util.Map.of("bridgeResult", "ok"));

        ArgumentCaptor<StandardEvent> captor = ArgumentCaptor.forClass(StandardEvent.class);
        verify(eventProducer).publish(captor.capture());
        StandardEvent event = captor.getValue();

        assertNotNull(event.getEventId());
        assertEquals("session-1", event.getCallSessionId());
        assertSame(EventType.CALL_BRIDGED, event.getEventType());
        assertEquals("ASTERISK", event.getProvider());
        assertEquals("campaign-1", event.getPayload().get("campaignId"));
        assertEquals("lead-1", event.getPayload().get("leadId"));
        assertEquals("+15550001", event.getPayload().get("customerNumber"));
        assertEquals("AGENT_ASSISTED", event.getPayload().get("callMode"));
        assertEquals("ivr-1", event.getPayload().get("ivrFlowId"));
        assertEquals("agent-1", event.getPayload().get("agentId"));
        assertEquals("PJSIP/1001", event.getPayload().get("agentChannel"));
        assertEquals("customer-channel", event.getPayload().get("customerChannel"));
        assertEquals("agent-live-channel", event.getPayload().get("agentLiveChannel"));
        assertEquals("ok", event.getPayload().get("bridgeResult"));
        assertNotNull(event.getTimestamp());
    }

    @Test
    void publishFailureUsesCallFailedEventWithResultAndErrorPayload() {
        EventProducer eventProducer = mock(EventProducer.class);
        EventPublisherService service = new EventPublisherService(eventProducer);
        CallSession session = session();

        service.publishFailure(session, "AMI_ORIGINATE_FAILED", "boom");

        ArgumentCaptor<StandardEvent> captor = ArgumentCaptor.forClass(StandardEvent.class);
        verify(eventProducer).publish(captor.capture());
        StandardEvent event = captor.getValue();

        assertSame(EventType.CALL_FAILED, event.getEventType());
        assertEquals("AMI_ORIGINATE_FAILED", event.getPayload().get("result"));
        assertEquals("boom", event.getPayload().get("error"));
    }

    @Test
    void publishFailurePreservesStructuredErrorPayload() {
        EventProducer eventProducer = mock(EventProducer.class);
        EventPublisherService service = new EventPublisherService(eventProducer);
        CallSession session = session();
        Map<String, Object> error = Map.of("code", 503, "reason", "AMI unavailable");

        service.publishFailure(session, "AMI_ORIGINATE_FAILED", error);

        ArgumentCaptor<StandardEvent> captor = ArgumentCaptor.forClass(StandardEvent.class);
        verify(eventProducer).publish(captor.capture());
        StandardEvent event = captor.getValue();

        assertSame(EventType.CALL_FAILED, event.getEventType());
        assertEquals("AMI_ORIGINATE_FAILED", event.getPayload().get("result"));
        assertSame(error, event.getPayload().get("error"));
    }

    @Test
    void publishWithoutExtraPayloadStillBuildsEvent() {
        EventProducer eventProducer = mock(EventProducer.class);
        EventPublisherService service = new EventPublisherService(eventProducer);
        CallSession session = session();

        service.publish(session, EventType.CALL_CREATED);

        ArgumentCaptor<StandardEvent> captor = ArgumentCaptor.forClass(StandardEvent.class);
        verify(eventProducer).publish(captor.capture());
        assertSame(EventType.CALL_CREATED, captor.getValue().getEventType());
    }

    private CallSession session() {
        return new CallSession(
                "session-1",
                "ASTERISK",
                "campaign-1",
                "lead-1",
                "+15550001",
                "AGENT_ASSISTED",
                "ivr-1",
                "agent-1",
                "PJSIP/1001"
        );
    }
}
