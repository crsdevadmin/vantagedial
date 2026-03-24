package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.AgentActivityBundleResponse;
import com.vantage.dialer.api.dto.CampaignReportBundleResponse;
import com.vantage.dialer.api.dto.CampaignSessionsBundleResponse;
import com.vantage.dialer.api.dto.CallSessionResponse;
import com.vantage.dialer.api.dto.CallTimelineBundleResponse;
import com.vantage.dialer.api.dto.IvrCampaignBundleResponse;
import com.vantage.dialer.api.dto.OperationalDashboardBundleResponse;
import com.vantage.dialer.api.dto.OperationalDashboardExportResponse;
import com.vantage.dialer.api.dto.OperationalDashboardResponse;
import com.vantage.dialer.api.persistence.model.CallEventEntity;
import com.vantage.dialer.api.persistence.model.CallSessionEntity;
import com.vantage.dialer.api.persistence.repository.CallEventRepository;
import com.vantage.dialer.api.persistence.repository.CallSessionRepository;
import com.vantage.dialer.api.store.EventStore;
import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportingServiceTest {

    private static final String CAMPAIGN_ID = "campaign-1";
    private static final String CALL_SESSION_ID = "session-1";
    private static final Instant FROM = Instant.parse("2026-03-22T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-03-23T00:00:00Z");

    @Test
    void operationalDashboardAggregatesCampaignIvrAndAgentActivity(@TempDir Path tempDir) throws Exception {
        EventStore eventStore = mock(EventStore.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        CallEventRepository callEventRepository = mock(CallEventRepository.class);
        CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
        ReportingService service = service(eventStore, callSessionService, callEventRepository, callSessionRepository, tempDir);

        when(callSessionRepository.findByCampaignIdAndCreatedAtBetweenOrderByCreatedAtDesc(CAMPAIGN_ID, FROM, TO))
                .thenReturn(sampleSessionEntities());
        when(callEventRepository.findAll()).thenReturn(sampleCallEvents());

        OperationalDashboardResponse dashboard = service.getOperationalDashboard(CAMPAIGN_ID, FROM, TO);

        assertEquals(CAMPAIGN_ID, dashboard.campaignId());
        assertEquals(3, dashboard.campaignSummary().totalSessions());
        assertEquals(1, dashboard.campaignSummary().completedSessions());
        assertEquals(1, dashboard.campaignSummary().failedSessions());
        assertEquals(2, dashboard.campaignSummary().bridgedSessions());
        assertEquals(2, dashboard.campaignSummary().answeredSessions());
        assertEquals("ivr-1", dashboard.ivrSummary().ivrFlowId());
        assertEquals(2, dashboard.ivrSummary().totalIvrSessions());
        assertEquals(1, dashboard.ivrSummary().completedIvrSessions());
        assertEquals(1, dashboard.ivrSummary().failedIvrSessions());
        assertEquals(2, dashboard.totalAgents());
        assertEquals(3, dashboard.totalCallsHandled());
        assertEquals(2, dashboard.totalAnsweredCalls());
        assertEquals(2, dashboard.totalCompletedCalls());
        assertEquals(1, dashboard.totalFailedCalls());
    }

    @Test
    void generateCallTimelineBundleWritesExpectedArtifacts(@TempDir Path tempDir) throws Exception {
        EventStore eventStore = mock(EventStore.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        CallEventRepository callEventRepository = mock(CallEventRepository.class);
        CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
        ReportingService service = service(eventStore, callSessionService, callEventRepository, callSessionRepository, tempDir);

        when(callSessionService.getCallSession(CALL_SESSION_ID)).thenReturn(Optional.of(sampleSessionResponses().get(0)));
        when(eventStore.getTimeline(CALL_SESSION_ID)).thenReturn(sampleTimelineEvents());

        CallTimelineBundleResponse bundle = service.generateCallTimelineBundle(CALL_SESSION_ID);

        JsonNode sessionJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.sessionJsonPath())));
        JsonNode timelineJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.timelineJsonPath())));
        String timelineCsv = Files.readString(Path.of(bundle.timelineCsvPath()));
        String timelineMarkdown = Files.readString(Path.of(bundle.timelineMarkdownPath()));
        String timelineHtml = Files.readString(Path.of(bundle.timelineHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals(CALL_SESSION_ID, sessionJson.get("callSessionId").asText());
        assertEquals(2, timelineJson.size());
        assertTrue(timelineCsv.contains("CALL_COMPLETED"));
        assertTrue(timelineMarkdown.contains("Call Session Id: session-1"));
        assertTrue(timelineMarkdown.contains("Payload:"));
        assertTrue(timelineMarkdown.contains("\"campaignId\":\"campaign-1\""));
        assertTrue(timelineHtml.contains("Call Timeline"));
        assertTrue(timelineHtml.contains("CALL_CREATED"));
        assertTrue(readme.contains("Event Count: 2"));
    }

    @Test
    void reportingBundlesAndOperationalDashboardExportWriteExpectedFiles(@TempDir Path tempDir) throws Exception {
        EventStore eventStore = mock(EventStore.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        CallEventRepository callEventRepository = mock(CallEventRepository.class);
        CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
        ReportingService service = service(eventStore, callSessionService, callEventRepository, callSessionRepository, tempDir);

        List<CallSessionEntity> sessionEntities = sampleSessionEntities();
        List<CallSessionResponse> sessionResponses = sampleSessionResponses();
        List<CallEventEntity> callEvents = sampleCallEvents();

        when(callSessionRepository.findByCampaignIdAndCreatedAtBetweenOrderByCreatedAtDesc(CAMPAIGN_ID, FROM, TO))
                .thenReturn(sessionEntities);
        when(callEventRepository.findAll()).thenReturn(callEvents);
        when(callSessionService.getCampaignSessions(CAMPAIGN_ID)).thenReturn(sessionResponses);

        CampaignSessionsBundleResponse sessionsBundle = service.generateCampaignSessionsBundle(CAMPAIGN_ID);
        IvrCampaignBundleResponse ivrBundle = service.generateIvrCampaignBundle(CAMPAIGN_ID, FROM, TO);
        CampaignReportBundleResponse reportBundle = service.generateCampaignReportBundle(CAMPAIGN_ID, FROM, TO);
        AgentActivityBundleResponse agentBundle = service.generateAgentActivityBundle(FROM, TO);
        OperationalDashboardExportResponse dashboardExport = service.exportOperationalDashboard(CAMPAIGN_ID, FROM, TO);
        OperationalDashboardBundleResponse dashboardBundle = service.generateOperationalDashboardBundle(CAMPAIGN_ID, FROM, TO);

        JsonNode ivrJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(ivrBundle.summaryJsonPath())));
        JsonNode dashboardJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(dashboardExport.dashboardJsonPath())));
        String sessionsCsv = Files.readString(Path.of(sessionsBundle.sessionsCsvPath()));
        String reportMarkdown = Files.readString(Path.of(reportBundle.reportMarkdownPath()));
        String agentHtml = Files.readString(Path.of(agentBundle.activityHtmlPath()));
        String exportReadme = Files.readString(Path.of(dashboardExport.readmePath()));
        String bundleHtml = Files.readString(Path.of(dashboardBundle.dashboardHtmlPath()));

        assertTrue(sessionsCsv.contains("session-1"));
        assertTrue(sessionsCsv.contains("OUTBOUND_IVR"));
        assertEquals("ivr-1", ivrJson.get("ivrFlowId").asText());
        assertEquals(2, ivrJson.get("totalIvrSessions").asInt());
        assertTrue(reportMarkdown.contains("Failed Sessions: 1"));
        assertTrue(reportMarkdown.contains("IVR Flow Id: ivr-1"));
        assertTrue(agentHtml.contains("Agent Activity"));
        assertTrue(agentHtml.contains("agent-2"));
        assertEquals(3, dashboardJson.get("totalCallsHandled").asInt());
        assertEquals(2, dashboardJson.get("totalAnsweredCalls").asInt());
        assertTrue(exportReadme.contains("Vantage Dialer Operational Dashboard Export"));
        assertTrue(bundleHtml.contains("Operational Dashboard"));
        assertTrue(bundleHtml.contains("IVR Summary"));
    }

    private ReportingService service(EventStore eventStore,
                                     CallSessionService callSessionService,
                                     CallEventRepository callEventRepository,
                                     CallSessionRepository callSessionRepository,
                                     Path tempDir) {
        return new ReportingService(
                eventStore,
                callSessionService,
                callEventRepository,
                callSessionRepository,
                CustomerServiceTestFixtures.objectMapper(),
                tempDir.toString()
        );
    }

    private List<CallSessionEntity> sampleSessionEntities() throws Exception {
        return List.of(
                sessionEntity("session-1", "lead-1", "AGENT_ASSISTED", null, "FLOWING", "CALL_BRIDGED", "agent-1", Instant.parse("2026-03-22T12:00:00Z")),
                sessionEntity("session-2", "lead-2", "OUTBOUND_IVR", "ivr-1", "COMPLETED", "CALL_COMPLETED", "agent-2", Instant.parse("2026-03-22T11:00:00Z")),
                sessionEntity("session-3", "lead-3", "OUTBOUND_IVR", "ivr-1", "FAILED", "CALL_FAILED", "agent-2", Instant.parse("2026-03-22T10:00:00Z"))
        );
    }

    private List<CallSessionResponse> sampleSessionResponses() {
        return List.of(
                new CallSessionResponse("session-1", CAMPAIGN_ID, "lead-1", "ASTERISK", "+15550001", "agent-1", "PJSIP/1001", "AGENT_ASSISTED", null, "FLOWING", "CALL_BRIDGED", Instant.parse("2026-03-22T12:05:00Z"), Instant.parse("2026-03-22T12:00:00Z")),
                new CallSessionResponse("session-2", CAMPAIGN_ID, "lead-2", "ASTERISK", "+15550002", "agent-2", "PJSIP/1002", "OUTBOUND_IVR", "ivr-1", "COMPLETED", "CALL_COMPLETED", Instant.parse("2026-03-22T11:05:00Z"), Instant.parse("2026-03-22T11:00:00Z")),
                new CallSessionResponse("session-3", CAMPAIGN_ID, "lead-3", "ASTERISK", "+15550003", "agent-2", "PJSIP/1002", "OUTBOUND_IVR", "ivr-1", "FAILED", "CALL_FAILED", Instant.parse("2026-03-22T10:05:00Z"), Instant.parse("2026-03-22T10:00:00Z"))
        );
    }

    private List<CallEventEntity> sampleCallEvents() {
        return List.of(
                callEvent("event-1", "session-1", "agent-1", EventType.AGENT_ANSWERED, Instant.parse("2026-03-22T12:01:00Z")),
                callEvent("event-2", "session-1", "agent-1", EventType.CALL_BRIDGED, Instant.parse("2026-03-22T12:02:00Z")),
                callEvent("event-3", "session-1", "agent-1", EventType.CALL_COMPLETED, Instant.parse("2026-03-22T12:03:00Z")),
                callEvent("event-4", "session-2", "agent-2", EventType.AGENT_ANSWERED, Instant.parse("2026-03-22T11:01:00Z")),
                callEvent("event-5", "session-2", "agent-2", EventType.CALL_COMPLETED, Instant.parse("2026-03-22T11:03:00Z")),
                callEvent("event-6", "session-3", "agent-2", EventType.CALL_FAILED, Instant.parse("2026-03-22T10:03:00Z"))
        );
    }

    private List<StandardEvent> sampleTimelineEvents() {
        return List.of(
                standardEvent("event-1", CALL_SESSION_ID, EventType.CALL_CREATED, Instant.parse("2026-03-22T12:00:00Z"), Map.of("campaignId", CAMPAIGN_ID, "customerNumber", "+15550001")),
                standardEvent("event-2", CALL_SESSION_ID, EventType.CALL_COMPLETED, Instant.parse("2026-03-22T12:05:00Z"), Map.of("campaignId", CAMPAIGN_ID, "durationSeconds", 42))
        );
    }

    private CallSessionEntity sessionEntity(String callSessionId,
                                            String leadId,
                                            String callMode,
                                            String ivrFlowId,
                                            String status,
                                            String lastEventType,
                                            String agentId,
                                            Instant createdAt) throws Exception {
        CallSessionEntity entity = new CallSessionEntity();
        entity.setCallSessionId(callSessionId);
        entity.setCampaignId(CAMPAIGN_ID);
        entity.setLeadId(leadId);
        entity.setProvider("ASTERISK");
        entity.setCustomerNumber("+1555" + leadId.substring(leadId.length() - 1));
        entity.setAgentId(agentId);
        entity.setAgentChannel(agentId == null ? null : "PJSIP/100" + agentId.substring(agentId.length() - 1));
        entity.setCallMode(callMode);
        entity.setIvrFlowId(ivrFlowId);
        entity.setStatus(status);
        entity.setLastEventType(lastEventType);
        entity.setLastEventAt(createdAt.plusSeconds(300));
        setField(entity, "createdAt", createdAt);
        return entity;
    }

    private CallEventEntity callEvent(String eventId,
                                      String callSessionId,
                                      String agentId,
                                      EventType eventType,
                                      Instant timestamp) {
        CallEventEntity entity = new CallEventEntity();
        entity.setEventId(eventId);
        entity.setCallSessionId(callSessionId);
        entity.setCallLegId("leg-" + eventId);
        entity.setEventType(eventType.name());
        entity.setEventTimestamp(timestamp);
        entity.setProvider("ASTERISK");
        entity.setProviderCallId("provider-" + eventId);
        entity.setLegType("CUSTOMER");
        entity.setCampaignId(CAMPAIGN_ID);
        entity.setLeadId("lead-" + callSessionId.substring(callSessionId.length() - 1));
        entity.setAgentId(agentId);
        entity.setPayloadJson("{\"event\":\"" + eventType.name() + "\"}");
        return entity;
    }

    private StandardEvent standardEvent(String eventId,
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
        event.setPayload(payload);
        return event;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
