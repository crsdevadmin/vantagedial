package com.vantage.dialer.api.kafka;

import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.campaign.Lead;
import com.vantage.dialer.api.campaign.LeadStatus;
import com.vantage.dialer.api.campaign.LeadStore;
import com.vantage.dialer.api.service.PredictiveDialerCoordinator;
import com.vantage.dialer.api.store.EventStore;
import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventConsumerTest {

    @Test
    void onEventWithNullPayloadOnlyUpsertsEvent() {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        PredictiveDialerCoordinator predictiveDialerCoordinator = mock(PredictiveDialerCoordinator.class);
        EventStore eventStore = mock(EventStore.class);
        EventConsumer consumer = new EventConsumer(leadStore, agentStore, predictiveDialerCoordinator, eventStore);

        StandardEvent event = event(EventType.CALL_CREATED, null);

        consumer.onEvent(event);

        verify(eventStore).upsert(event);
        verifyNoInteractions(leadStore, agentStore, predictiveDialerCoordinator);
    }

    @Test
    void onEventWithoutCampaignOrLeadOnlyUpsertsEvent() {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        PredictiveDialerCoordinator predictiveDialerCoordinator = mock(PredictiveDialerCoordinator.class);
        EventStore eventStore = mock(EventStore.class);
        EventConsumer consumer = new EventConsumer(leadStore, agentStore, predictiveDialerCoordinator, eventStore);

        StandardEvent event = event(EventType.CALL_CREATED, Map.of("campaignId", "campaign-1"));

        consumer.onEvent(event);

        verify(eventStore).upsert(event);
        verifyNoInteractions(leadStore, agentStore, predictiveDialerCoordinator);
    }

    @Test
    void callCreatedMarksLeadInProgress() {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        PredictiveDialerCoordinator predictiveDialerCoordinator = mock(PredictiveDialerCoordinator.class);
        EventStore eventStore = mock(EventStore.class);
        EventConsumer consumer = new EventConsumer(leadStore, agentStore, predictiveDialerCoordinator, eventStore);

        StandardEvent event = event(EventType.CALL_CREATED, payload("campaign-1", "lead-1", "agent-1"));

        consumer.onEvent(event);

        verify(eventStore).upsert(event);
        verify(leadStore).updateStatus("campaign-1", "lead-1", LeadStatus.IN_PROGRESS);
        verifyNoInteractions(agentStore);
        verifyNoInteractions(predictiveDialerCoordinator);
    }

    @Test
    void customerAnsweredQueuesPredictiveCall() {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        PredictiveDialerCoordinator predictiveDialerCoordinator = mock(PredictiveDialerCoordinator.class);
        EventStore eventStore = mock(EventStore.class);
        EventConsumer consumer = new EventConsumer(leadStore, agentStore, predictiveDialerCoordinator, eventStore);

        StandardEvent event = event(EventType.CUSTOMER_ANSWERED, payload("campaign-1", "lead-1", "agent-1"));

        consumer.onEvent(event);

        verify(eventStore).upsert(event);
        verify(predictiveDialerCoordinator).queueAnsweredCall(event);
        verifyNoInteractions(leadStore, agentStore);
    }

    @Test
    void callCompletedMarksLeadCompletedReleasesAgentAndDispatchesWaitingCalls() {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        PredictiveDialerCoordinator predictiveDialerCoordinator = mock(PredictiveDialerCoordinator.class);
        EventStore eventStore = mock(EventStore.class);
        EventConsumer consumer = new EventConsumer(leadStore, agentStore, predictiveDialerCoordinator, eventStore);

        StandardEvent event = event(EventType.CALL_COMPLETED, payload("campaign-1", "lead-1", "agent-1"));

        consumer.onEvent(event);

        verify(eventStore).upsert(event);
        verify(leadStore).updateStatus("campaign-1", "lead-1", LeadStatus.COMPLETED);
        verify(agentStore).releaseAgent("agent-1");
        verify(predictiveDialerCoordinator).dispatchWaitingCalls("campaign-1");
    }

    @Test
    void callFailedResetsLeadToNewWhenAttemptsAreBelowThree() {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        PredictiveDialerCoordinator predictiveDialerCoordinator = mock(PredictiveDialerCoordinator.class);
        EventStore eventStore = mock(EventStore.class);
        EventConsumer consumer = new EventConsumer(leadStore, agentStore, predictiveDialerCoordinator, eventStore);

        Lead lead = lead("campaign-1", "lead-1", 2);
        when(leadStore.getLeads("campaign-1")).thenReturn(List.of(lead));

        StandardEvent event = event(EventType.CALL_FAILED, payload("campaign-1", "lead-1", "agent-1"));

        consumer.onEvent(event);

        verify(eventStore).upsert(event);
        verify(leadStore).getLeads("campaign-1");
        verify(leadStore).updateStatus("campaign-1", "lead-1", LeadStatus.NEW);
        verify(agentStore).releaseAgent("agent-1");
        verify(predictiveDialerCoordinator).dispatchWaitingCalls("campaign-1");
    }

    @Test
    void callFailedMarksLeadFailedAfterThreeAttemptsAndSkipsBlankAgentRelease() {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        PredictiveDialerCoordinator predictiveDialerCoordinator = mock(PredictiveDialerCoordinator.class);
        EventStore eventStore = mock(EventStore.class);
        EventConsumer consumer = new EventConsumer(leadStore, agentStore, predictiveDialerCoordinator, eventStore);

        Lead lead = lead("campaign-1", "lead-1", 3);
        when(leadStore.getLeads("campaign-1")).thenReturn(List.of(lead));

        StandardEvent event = event(EventType.CALL_FAILED, payload("campaign-1", "lead-1", " "));

        consumer.onEvent(event);

        verify(eventStore).upsert(event);
        verify(leadStore).updateStatus("campaign-1", "lead-1", LeadStatus.FAILED);
        verify(agentStore, never()).releaseAgent(" ");
        verify(agentStore, never()).releaseAgent("");
        verify(predictiveDialerCoordinator).dispatchWaitingCalls("campaign-1");
    }

    private StandardEvent event(EventType eventType, Map<String, Object> payload) {
        StandardEvent event = new StandardEvent();
        event.setEventId("event-1");
        event.setCallSessionId("session-1");
        event.setEventType(eventType);
        event.setTimestamp(Instant.parse("2026-03-23T10:00:00Z"));
        event.setProvider("ASTERISK");
        event.setPayload(payload);
        return event;
    }

    private Map<String, Object> payload(String campaignId, String leadId, String agentId) {
        return Map.of(
                "campaignId", campaignId,
                "leadId", leadId,
                "agentId", agentId
        );
    }

    private Lead lead(String campaignId, String leadId, int attempts) {
        Lead lead = new Lead(leadId, campaignId, "+15550001");
        lead.setAttempts(attempts);
        return lead;
    }
}
