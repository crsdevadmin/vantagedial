package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.CallSessionResponse;
import com.vantage.dialer.api.dto.CallTimelineBundleResponse;
import com.vantage.dialer.api.service.CallSessionService;
import com.vantage.dialer.api.service.ReportingService;
import com.vantage.dialer.api.store.EventStore;
import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventQueryControllerTest {

    @Test
    void statusEndpointReturnsOkAndNotFoundDependingOnStoredEvent() throws Exception {
        EventStore eventStore = mock(EventStore.class);
        ReportingService reportingService = mock(ReportingService.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new EventQueryController(eventStore, reportingService, callSessionService));

        when(eventStore.getLatest("session-1")).thenReturn(Optional.of(event("event-1", EventType.CALL_COMPLETED)));
        when(eventStore.getLatest("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/outbound/status/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("event-1"))
                .andExpect(jsonPath("$.eventType").value("CALL_COMPLETED"));

        mockMvc.perform(get("/outbound/status/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void timelineEndpointsReturnEventsAndGeneratedBundle() throws Exception {
        EventStore eventStore = mock(EventStore.class);
        ReportingService reportingService = mock(ReportingService.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new EventQueryController(eventStore, reportingService, callSessionService));

        when(reportingService.getTimeline("session-2")).thenReturn(List.of(
                event("event-2", EventType.CALL_CREATED),
                event("event-3", EventType.CALL_BRIDGED)
        ));
        when(reportingService.generateCallTimelineBundle("session-2"))
                .thenReturn(new CallTimelineBundleResponse(
                        "session-2", "exports/session-2", "session.json", "timeline.json", "timeline.csv",
                        "timeline.md", "timeline.html", "README.txt", Instant.parse("2026-03-22T10:10:00Z")
                ));

        mockMvc.perform(get("/outbound/timeline/session-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("CALL_CREATED"))
                .andExpect(jsonPath("$[1].eventType").value("CALL_BRIDGED"));

        mockMvc.perform(post("/outbound/timeline/session-2/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callSessionId").value("session-2"))
                .andExpect(jsonPath("$.timelineHtmlPath").value("timeline.html"));
    }

    @Test
    void callSessionEndpointReturnsOkAndNotFoundDependingOnLookupResult() throws Exception {
        EventStore eventStore = mock(EventStore.class);
        ReportingService reportingService = mock(ReportingService.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new EventQueryController(eventStore, reportingService, callSessionService));

        when(callSessionService.getCallSession("session-3")).thenReturn(Optional.of(
                new CallSessionResponse("session-3", "camp-1", "lead-1", "ASTERISK", "+15550001", "agent-1", "PJSIP/1001", "AGENT_ASSISTED", null, "FLOWING", "CALL_BRIDGED", Instant.parse("2026-03-22T10:05:00Z"), Instant.parse("2026-03-22T10:00:00Z"))
        ));
        when(callSessionService.getCallSession("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/outbound/sessions/session-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callSessionId").value("session-3"))
                .andExpect(jsonPath("$.status").value("FLOWING"));

        mockMvc.perform(get("/outbound/sessions/missing"))
                .andExpect(status().isNotFound());
    }

    private StandardEvent event(String eventId, EventType eventType) {
        StandardEvent event = new StandardEvent();
        event.setEventId(eventId);
        event.setCallSessionId("session");
        event.setEventType(eventType);
        event.setTimestamp(Instant.parse("2026-03-22T10:00:00Z"));
        event.setProvider("ASTERISK");
        event.setProviderCallId("provider-" + eventId);
        event.setLegType("CUSTOMER");
        event.setPayload(Map.of("campaignId", "camp-1"));
        return event;
    }
}
