package com.vantage.dialer.api.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.persistence.model.CallEventEntity;
import com.vantage.dialer.api.persistence.model.CallSessionEntity;
import com.vantage.dialer.api.persistence.repository.CallEventRepository;
import com.vantage.dialer.api.persistence.repository.CallSessionRepository;
import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventStoreTest {

    @Test
    void upsertPersistsEventAndCreatesSessionWithDefaultCallMode() {
        CallEventRepository callEventRepository = mock(CallEventRepository.class);
        CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
        EventStore store = new EventStore(callEventRepository, callSessionRepository, new ObjectMapper());

        when(callSessionRepository.findById("session-1")).thenReturn(Optional.empty());
        when(callEventRepository.save(any(CallEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(callSessionRepository.save(any(CallSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        store.upsert(event(
                "event-1",
                "session-1",
                EventType.CALL_CREATED,
                Instant.parse("2026-03-22T10:15:30Z"),
                Map.of(
                        "campaignId", "campaign-1",
                        "leadId", "lead-1",
                        "agentId", "agent-1",
                        "customerNumber", "+15551234567",
                        "agentChannel", "PJSIP/1001",
                        "ivrFlowId", "ivr-1"
                )
        ));

        ArgumentCaptor<CallEventEntity> eventCaptor = ArgumentCaptor.forClass(CallEventEntity.class);
        ArgumentCaptor<CallSessionEntity> sessionCaptor = ArgumentCaptor.forClass(CallSessionEntity.class);
        verify(callEventRepository).save(eventCaptor.capture());
        verify(callSessionRepository).save(sessionCaptor.capture());

        CallEventEntity savedEvent = eventCaptor.getValue();
        assertEquals("event-1", savedEvent.getEventId());
        assertEquals("session-1", savedEvent.getCallSessionId());
        assertEquals("CALL_CREATED", savedEvent.getEventType());
        assertEquals("campaign-1", savedEvent.getCampaignId());
        assertEquals("lead-1", savedEvent.getLeadId());
        assertEquals("agent-1", savedEvent.getAgentId());
        assertTrue(savedEvent.getPayloadJson().contains("\"customerNumber\":\"+15551234567\""));

        CallSessionEntity savedSession = sessionCaptor.getValue();
        assertEquals("session-1", savedSession.getCallSessionId());
        assertEquals("campaign-1", savedSession.getCampaignId());
        assertEquals("lead-1", savedSession.getLeadId());
        assertEquals("ASTERISK", savedSession.getProvider());
        assertEquals("+15551234567", savedSession.getCustomerNumber());
        assertEquals("agent-1", savedSession.getAgentId());
        assertEquals("PJSIP/1001", savedSession.getAgentChannel());
        assertEquals("ivr-1", savedSession.getIvrFlowId());
        assertEquals("AGENT_ASSISTED", savedSession.getCallMode());
        assertEquals("CALL_CREATED", savedSession.getStatus());
        assertEquals("CALL_CREATED", savedSession.getLastEventType());
        assertEquals(Instant.parse("2026-03-22T10:15:30Z"), savedSession.getLastEventAt());
    }

    @Test
    void upsertUpdatesExistingSessionAndUsesExplicitCallMode() {
        CallEventRepository callEventRepository = mock(CallEventRepository.class);
        CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
        EventStore store = new EventStore(callEventRepository, callSessionRepository, new ObjectMapper());

        CallSessionEntity existing = new CallSessionEntity();
        existing.setCallSessionId("session-2");
        existing.setProvider("ASTERISK");
        existing.setCustomerNumber("+15550000000");
        existing.setCallMode("AGENT_ASSISTED");
        existing.setStatus("CALL_CREATED");

        when(callSessionRepository.findById("session-2")).thenReturn(Optional.of(existing));
        when(callEventRepository.save(any(CallEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(callSessionRepository.save(any(CallSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        store.upsert(event(
                "event-2",
                "session-2",
                EventType.CALL_BRIDGED,
                Instant.parse("2026-03-22T10:16:00Z"),
                Map.of(
                        "campaignId", "campaign-2",
                        "leadId", "lead-2",
                        "agentId", "agent-2",
                        "customerNumber", "+15557654321",
                        "agentChannel", "PJSIP/1002",
                        "ivrFlowId", "ivr-2",
                        "callMode", "OUTBOUND_IVR"
                )
        ));

        ArgumentCaptor<CallSessionEntity> sessionCaptor = ArgumentCaptor.forClass(CallSessionEntity.class);
        verify(callSessionRepository).save(sessionCaptor.capture());

        CallSessionEntity savedSession = sessionCaptor.getValue();
        assertSame(existing, savedSession);
        assertEquals("campaign-2", savedSession.getCampaignId());
        assertEquals("lead-2", savedSession.getLeadId());
        assertEquals("+15557654321", savedSession.getCustomerNumber());
        assertEquals("agent-2", savedSession.getAgentId());
        assertEquals("PJSIP/1002", savedSession.getAgentChannel());
        assertEquals("ivr-2", savedSession.getIvrFlowId());
        assertEquals("OUTBOUND_IVR", savedSession.getCallMode());
        assertEquals("CALL_BRIDGED", savedSession.getStatus());
        assertEquals("CALL_BRIDGED", savedSession.getLastEventType());
        assertEquals(Instant.parse("2026-03-22T10:16:00Z"), savedSession.getLastEventAt());
    }

    @Test
    void getLatestAndTimelineRehydrateStoredEvents() throws Exception {
        CallEventRepository callEventRepository = mock(CallEventRepository.class);
        CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        EventStore store = new EventStore(callEventRepository, callSessionRepository, objectMapper);

        when(callEventRepository.findByCallSessionIdOrderByEventTimestampAsc("missing")).thenReturn(List.of());
        assertFalse(store.getLatest("missing").isPresent());

        List<CallEventEntity> entities = List.of(
                eventEntity(
                        objectMapper,
                        "event-1",
                        "session-3",
                        EventType.CALL_CREATED,
                        Instant.parse("2026-03-22T10:00:00Z"),
                        Map.of("campaignId", "campaign-3", "customerNumber", "+155501")
                ),
                eventEntity(
                        objectMapper,
                        "event-2",
                        "session-3",
                        EventType.CALL_COMPLETED,
                        Instant.parse("2026-03-22T10:05:00Z"),
                        Map.of("campaignId", "campaign-3", "agentId", "agent-3", "disposition", "SUCCESS")
                )
        );

        when(callEventRepository.findByCallSessionIdOrderByEventTimestampAsc("session-3")).thenReturn(entities);

        List<StandardEvent> timeline = store.getTimeline("session-3");
        StandardEvent latest = store.getLatest("session-3").orElseThrow();

        assertEquals(2, timeline.size());
        assertEquals(EventType.CALL_CREATED, timeline.get(0).getEventType());
        assertEquals("campaign-3", String.valueOf(timeline.get(0).getPayload().get("campaignId")));
        assertEquals(EventType.CALL_COMPLETED, latest.getEventType());
        assertEquals("agent-3", String.valueOf(latest.getPayload().get("agentId")));
        assertEquals("SUCCESS", String.valueOf(latest.getPayload().get("disposition")));
        assertEquals(Instant.parse("2026-03-22T10:05:00Z"), latest.getTimestamp());
    }

    private StandardEvent event(String eventId,
                                String callSessionId,
                                EventType eventType,
                                Instant timestamp,
                                Map<String, Object> payload) {
        StandardEvent event = new StandardEvent();
        event.setEventId(eventId);
        event.setCallSessionId(callSessionId);
        event.setCallLegId("leg-" + eventId);
        event.setEventType(eventType);
        event.setTimestamp(timestamp);
        event.setProvider("ASTERISK");
        event.setProviderCallId("provider-" + eventId);
        event.setLegType("CUSTOMER");
        event.setPayload(new LinkedHashMap<>(payload));
        return event;
    }

    private CallEventEntity eventEntity(ObjectMapper objectMapper,
                                        String eventId,
                                        String callSessionId,
                                        EventType eventType,
                                        Instant timestamp,
                                        Map<String, Object> payload) throws Exception {
        CallEventEntity entity = new CallEventEntity();
        entity.setEventId(eventId);
        entity.setCallSessionId(callSessionId);
        entity.setCallLegId("leg-" + eventId);
        entity.setEventType(eventType.name());
        entity.setEventTimestamp(timestamp);
        entity.setProvider("ASTERISK");
        entity.setProviderCallId("provider-" + eventId);
        entity.setLegType("CUSTOMER");
        entity.setCampaignId(payload.containsKey("campaignId") ? String.valueOf(payload.get("campaignId")) : null);
        entity.setLeadId(payload.containsKey("leadId") ? String.valueOf(payload.get("leadId")) : null);
        entity.setAgentId(payload.containsKey("agentId") ? String.valueOf(payload.get("agentId")) : null);
        entity.setPayloadJson(objectMapper.writeValueAsString(payload));
        return entity;
    }
}
