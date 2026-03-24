package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.AgentActivitySummary;
import com.vantage.dialer.api.dto.AgentActivityBundleResponse;
import com.vantage.dialer.api.dto.CallSessionResponse;
import com.vantage.dialer.api.dto.CallTimelineBundleResponse;
import com.vantage.dialer.api.dto.CampaignReportBundleResponse;
import com.vantage.dialer.api.dto.CampaignSessionsBundleResponse;
import com.vantage.dialer.api.dto.CampaignSummaryResponse;
import com.vantage.dialer.api.dto.IvrCampaignBundleResponse;
import com.vantage.dialer.api.dto.IvrCampaignSummaryResponse;
import com.vantage.dialer.api.dto.OperationalDashboardBundleResponse;
import com.vantage.dialer.api.dto.OperationalDashboardExportResponse;
import com.vantage.dialer.api.dto.OperationalDashboardResponse;
import com.vantage.dialer.api.persistence.repository.CallEventRepository;
import com.vantage.dialer.api.persistence.repository.CallSessionRepository;
import com.vantage.dialer.api.store.EventStore;
import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private final EventStore eventStore;
    private final CallSessionService callSessionService;
    private final CallEventRepository callEventRepository;
    private final CallSessionRepository callSessionRepository;
    private final ObjectMapper objectMapper;
    private final Path exportDirectory;

    public ReportingService(EventStore eventStore,
                            CallSessionService callSessionService,
                            CallEventRepository callEventRepository,
                            CallSessionRepository callSessionRepository,
                            ObjectMapper objectMapper,
                            @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.eventStore = eventStore;
        this.callSessionService = callSessionService;
        this.callEventRepository = callEventRepository;
        this.callSessionRepository = callSessionRepository;
        this.objectMapper = objectMapper;
        this.exportDirectory = Path.of(exportDirectory).resolve("reports");
    }

    @Transactional(readOnly = true)
    public List<StandardEvent> getTimeline(String callSessionId) {
        return eventStore.getTimeline(callSessionId);
    }

    @Transactional(readOnly = true)
    public CallTimelineBundleResponse generateCallTimelineBundle(String callSessionId) {
        CallSessionResponse session = callSessionService.getCallSession(callSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown call session: " + callSessionId));
        List<StandardEvent> timeline = getTimeline(callSessionId);
        Instant generatedAt = Instant.now();

        try {
            Path bundleDirectory = exportDirectory.resolve("calls").resolve(callSessionId);
            Files.createDirectories(bundleDirectory);

            Path sessionJsonPath = bundleDirectory.resolve("session.json");
            Path timelineJsonPath = bundleDirectory.resolve("timeline.json");
            Path timelineCsvPath = bundleDirectory.resolve("timeline.csv");
            Path timelineMarkdownPath = bundleDirectory.resolve("timeline.md");
            Path timelineHtmlPath = bundleDirectory.resolve("timeline.html");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(sessionJsonPath, writeJson(session));
            Files.writeString(timelineJsonPath, writeJson(timeline));
            Files.write(timelineCsvPath, buildCallTimelineCsv(timeline));
            Files.writeString(timelineMarkdownPath, buildCallTimelineMarkdown(session, timeline, generatedAt));
            Files.writeString(timelineHtmlPath, buildCallTimelineHtml(session, timeline, generatedAt));
            Files.writeString(readmePath, buildCallTimelineReadme(session, timeline.size(), generatedAt));

            return new CallTimelineBundleResponse(
                    callSessionId,
                    bundleDirectory.toAbsolutePath().toString(),
                    sessionJsonPath.toAbsolutePath().toString(),
                    timelineJsonPath.toAbsolutePath().toString(),
                    timelineCsvPath.toAbsolutePath().toString(),
                    timelineMarkdownPath.toAbsolutePath().toString(),
                    timelineHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate call timeline bundle", e);
        }
    }

    @Transactional(readOnly = true)
    public CampaignSummaryResponse getCampaignSummary(String campaignId, Instant from, Instant to) {
        List<CallSessionResponse> sessions = callSessionRepository
                .findByCampaignIdAndCreatedAtBetweenOrderByCreatedAtDesc(campaignId, from, to).stream()
                .map(entity -> new CallSessionResponse(
                        entity.getCallSessionId(),
                        entity.getCampaignId(),
                        entity.getLeadId(),
                        entity.getProvider(),
                        entity.getCustomerNumber(),
                        entity.getAgentId(),
                        entity.getAgentChannel(),
                        entity.getCallMode(),
                        entity.getIvrFlowId(),
                        entity.getStatus(),
                        entity.getLastEventType(),
                        entity.getLastEventAt(),
                        entity.getCreatedAt()))
                .toList();
        long completed = sessions.stream().filter(s -> "CALL_COMPLETED".equals(s.lastEventType())).count();
        long failed = sessions.stream().filter(s -> "CALL_FAILED".equals(s.lastEventType())).count();
        long bridged = sessions.stream().filter(s -> "CALL_BRIDGED".equals(s.lastEventType()) || "CALL_COMPLETED".equals(s.lastEventType())).count();
        long answered = sessions.stream().filter(s -> List.of("CUSTOMER_ANSWERED", "AGENT_ANSWERED", "CALL_BRIDGED", "CALL_COMPLETED").contains(s.lastEventType())).count();
        return new CampaignSummaryResponse(campaignId, sessions.size(), completed, failed, bridged, answered);
    }

    @Transactional(readOnly = true)
    public List<AgentActivitySummary> getAgentActivity(Instant from, Instant to) {
        Map<String, List<com.vantage.dialer.api.persistence.model.CallEventEntity>> eventsByAgent =
                callEventRepository.findAll().stream()
                        .filter(event -> event.getAgentId() != null)
                        .filter(event -> !event.getEventTimestamp().isBefore(from) && !event.getEventTimestamp().isAfter(to))
                        .collect(Collectors.groupingBy(com.vantage.dialer.api.persistence.model.CallEventEntity::getAgentId));

        return eventsByAgent.entrySet().stream()
                .map(entry -> {
                    List<com.vantage.dialer.api.persistence.model.CallEventEntity> events = entry.getValue();
                    return new AgentActivitySummary(
                            entry.getKey(),
                            events.stream().map(com.vantage.dialer.api.persistence.model.CallEventEntity::getCallSessionId).distinct().count(),
                            countType(events, EventType.AGENT_ANSWERED),
                            countType(events, EventType.CALL_BRIDGED),
                            countType(events, EventType.CALL_COMPLETED),
                            countType(events, EventType.CALL_FAILED)
                    );
                })
                .sorted(Comparator.comparing(AgentActivitySummary::agentId))
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentActivityBundleResponse generateAgentActivityBundle(Instant from, Instant to) {
        List<AgentActivitySummary> activity = getAgentActivity(from, to);
        Instant generatedAt = Instant.now();

        try {
            Path bundleDirectory = exportDirectory.resolve("agents")
                    .resolve("activity-" + generatedAt.toEpochMilli());
            Files.createDirectories(bundleDirectory);

            Path activityJsonPath = bundleDirectory.resolve("agent-activity.json");
            Path activityCsvPath = bundleDirectory.resolve("agent-activity.csv");
            Path activityMarkdownPath = bundleDirectory.resolve("agent-activity.md");
            Path activityHtmlPath = bundleDirectory.resolve("agent-activity.html");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(activityJsonPath, writeJson(activity));
            Files.write(activityCsvPath, buildAgentActivityCsv(activity));
            Files.writeString(activityMarkdownPath, buildAgentActivityMarkdown(activity, from, to, generatedAt));
            Files.writeString(activityHtmlPath, buildAgentActivityHtml(activity, from, to, generatedAt));
            Files.writeString(readmePath, buildAgentActivityReadme(activity.size(), from, to, generatedAt));

            return new AgentActivityBundleResponse(
                    from,
                    to,
                    bundleDirectory.toAbsolutePath().toString(),
                    activityJsonPath.toAbsolutePath().toString(),
                    activityCsvPath.toAbsolutePath().toString(),
                    activityMarkdownPath.toAbsolutePath().toString(),
                    activityHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate agent activity bundle", e);
        }
    }

    @Transactional(readOnly = true)
    public IvrCampaignSummaryResponse getIvrSummary(String campaignId, Instant from, Instant to) {
        List<CallSessionResponse> sessions = callSessionRepository
                .findByCampaignIdAndCreatedAtBetweenOrderByCreatedAtDesc(campaignId, from, to).stream()
                .map(entity -> new CallSessionResponse(
                        entity.getCallSessionId(),
                        entity.getCampaignId(),
                        entity.getLeadId(),
                        entity.getProvider(),
                        entity.getCustomerNumber(),
                        entity.getAgentId(),
                        entity.getAgentChannel(),
                        entity.getCallMode(),
                        entity.getIvrFlowId(),
                        entity.getStatus(),
                        entity.getLastEventType(),
                        entity.getLastEventAt(),
                        entity.getCreatedAt()))
                .filter(session -> "OUTBOUND_IVR".equals(session.callMode()))
                .toList();

        String ivrFlowId = sessions.stream().map(CallSessionResponse::ivrFlowId).filter(Objects::nonNull).findFirst().orElse(null);
        long completed = sessions.stream().filter(s -> "CALL_COMPLETED".equals(s.lastEventType())).count();
        long failed = sessions.stream().filter(s -> "CALL_FAILED".equals(s.lastEventType())).count();
        return new IvrCampaignSummaryResponse(campaignId, ivrFlowId, sessions.size(), completed, failed);
    }

    @Transactional(readOnly = true)
    public IvrCampaignBundleResponse generateIvrCampaignBundle(String campaignId, Instant from, Instant to) {
        IvrCampaignSummaryResponse summary = getIvrSummary(campaignId, from, to);
        Instant generatedAt = Instant.now();

        try {
            Path bundleDirectory = exportDirectory.resolve("ivr").resolve(campaignId);
            Files.createDirectories(bundleDirectory);

            Path summaryJsonPath = bundleDirectory.resolve("ivr-summary.json");
            Path summaryMarkdownPath = bundleDirectory.resolve("ivr-summary.md");
            Path summaryHtmlPath = bundleDirectory.resolve("ivr-summary.html");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(summaryJsonPath, writeJson(summary));
            Files.writeString(summaryMarkdownPath, buildIvrSummaryMarkdown(summary, generatedAt));
            Files.writeString(summaryHtmlPath, buildIvrSummaryHtml(summary, generatedAt));
            Files.writeString(readmePath, buildIvrSummaryReadme(summary, generatedAt));

            return new IvrCampaignBundleResponse(
                    campaignId,
                    from,
                    to,
                    bundleDirectory.toAbsolutePath().toString(),
                    summaryJsonPath.toAbsolutePath().toString(),
                    summaryMarkdownPath.toAbsolutePath().toString(),
                    summaryHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate IVR campaign bundle", e);
        }
    }

    @Transactional(readOnly = true)
    public CampaignSessionsBundleResponse generateCampaignSessionsBundle(String campaignId) {
        List<CallSessionResponse> sessions = callSessionService.getCampaignSessions(campaignId);
        Instant generatedAt = Instant.now();

        try {
            Path bundleDirectory = exportDirectory.resolve("campaigns").resolve(campaignId).resolve("sessions");
            Files.createDirectories(bundleDirectory);

            Path sessionsJsonPath = bundleDirectory.resolve("sessions.json");
            Path sessionsCsvPath = bundleDirectory.resolve("sessions.csv");
            Path sessionsMarkdownPath = bundleDirectory.resolve("sessions.md");
            Path sessionsHtmlPath = bundleDirectory.resolve("sessions.html");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(sessionsJsonPath, writeJson(sessions));
            Files.write(sessionsCsvPath, buildCampaignSessionsCsv(sessions));
            Files.writeString(sessionsMarkdownPath, buildCampaignSessionsMarkdown(campaignId, sessions, generatedAt));
            Files.writeString(sessionsHtmlPath, buildCampaignSessionsHtml(campaignId, sessions, generatedAt));
            Files.writeString(readmePath, buildCampaignSessionsReadme(campaignId, sessions.size(), generatedAt));

            return new CampaignSessionsBundleResponse(
                    campaignId,
                    bundleDirectory.toAbsolutePath().toString(),
                    sessionsJsonPath.toAbsolutePath().toString(),
                    sessionsCsvPath.toAbsolutePath().toString(),
                    sessionsMarkdownPath.toAbsolutePath().toString(),
                    sessionsHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate campaign sessions bundle", e);
        }
    }

    @Transactional(readOnly = true)
    public CampaignReportBundleResponse generateCampaignReportBundle(String campaignId, Instant from, Instant to) {
        CampaignSummaryResponse summary = getCampaignSummary(campaignId, from, to);
        IvrCampaignSummaryResponse ivrSummary = getIvrSummary(campaignId, from, to);
        List<CallSessionResponse> sessions = callSessionService.getCampaignSessions(campaignId);
        Instant generatedAt = Instant.now();

        try {
            Path bundleDirectory = exportDirectory.resolve("campaigns").resolve(campaignId).resolve("report");
            Files.createDirectories(bundleDirectory);

            Path summaryJsonPath = bundleDirectory.resolve("summary.json");
            Path ivrSummaryJsonPath = bundleDirectory.resolve("ivr-summary.json");
            Path sessionsJsonPath = bundleDirectory.resolve("sessions.json");
            Path reportMarkdownPath = bundleDirectory.resolve("report.md");
            Path reportHtmlPath = bundleDirectory.resolve("report.html");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(summaryJsonPath, writeJson(summary));
            Files.writeString(ivrSummaryJsonPath, writeJson(ivrSummary));
            Files.writeString(sessionsJsonPath, writeJson(sessions));
            Files.writeString(reportMarkdownPath, buildCampaignReportMarkdown(summary, ivrSummary, sessions, generatedAt));
            Files.writeString(reportHtmlPath, buildCampaignReportHtml(summary, ivrSummary, sessions, generatedAt));
            Files.writeString(readmePath, buildCampaignReportReadme(campaignId, sessions.size(), generatedAt));

            return new CampaignReportBundleResponse(
                    campaignId,
                    from,
                    to,
                    bundleDirectory.toAbsolutePath().toString(),
                    summaryJsonPath.toAbsolutePath().toString(),
                    ivrSummaryJsonPath.toAbsolutePath().toString(),
                    sessionsJsonPath.toAbsolutePath().toString(),
                    reportMarkdownPath.toAbsolutePath().toString(),
                    reportHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate campaign report bundle", e);
        }
    }

    @Transactional(readOnly = true)
    public OperationalDashboardResponse getOperationalDashboard(String campaignId, Instant from, Instant to) {
        CampaignSummaryResponse campaignSummary = campaignId == null || campaignId.isBlank()
                ? null
                : getCampaignSummary(campaignId, from, to);
        IvrCampaignSummaryResponse ivrSummary = campaignId == null || campaignId.isBlank()
                ? null
                : getIvrSummary(campaignId, from, to);
        List<AgentActivitySummary> agentActivity = getAgentActivity(from, to);

        return new OperationalDashboardResponse(
                campaignId,
                from,
                to,
                Instant.now(),
                campaignSummary,
                ivrSummary,
                agentActivity,
                agentActivity.size(),
                agentActivity.stream().mapToLong(AgentActivitySummary::callsHandled).sum(),
                agentActivity.stream().mapToLong(AgentActivitySummary::answeredCalls).sum(),
                agentActivity.stream().mapToLong(AgentActivitySummary::completedCalls).sum(),
                agentActivity.stream().mapToLong(AgentActivitySummary::failedCalls).sum()
        );
    }

    @Transactional(readOnly = true)
    public OperationalDashboardExportResponse exportOperationalDashboard(String campaignId, Instant from, Instant to) {
        OperationalDashboardResponse dashboard = getOperationalDashboard(campaignId, from, to);
        Instant generatedAt = dashboard.generatedAt();

        try {
            String scope = (campaignId == null || campaignId.isBlank()) ? "all-campaigns" : campaignId;
            Path bundleDirectory = exportDirectory.resolve(scope).resolve("dashboard");
            Files.createDirectories(bundleDirectory);

            Path dashboardJsonPath = bundleDirectory.resolve("dashboard.json");
            Path dashboardCsvPath = bundleDirectory.resolve("dashboard.csv");
            Path dashboardMarkdownPath = bundleDirectory.resolve("dashboard.md");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(dashboardJsonPath, writeJson(dashboard));
            Files.write(dashboardCsvPath, buildDashboardCsv(dashboard));
            Files.writeString(dashboardMarkdownPath, buildDashboardMarkdown(dashboard));
            Files.writeString(readmePath, buildDashboardReadme(dashboard));

            return new OperationalDashboardExportResponse(
                    campaignId,
                    from,
                    to,
                    bundleDirectory.toAbsolutePath().toString(),
                    dashboardJsonPath.toAbsolutePath().toString(),
                    dashboardCsvPath.toAbsolutePath().toString(),
                    dashboardMarkdownPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export operational dashboard", e);
        }
    }

    @Transactional(readOnly = true)
    public OperationalDashboardBundleResponse generateOperationalDashboardBundle(String campaignId, Instant from, Instant to) {
        OperationalDashboardResponse dashboard = getOperationalDashboard(campaignId, from, to);
        Instant generatedAt = dashboard.generatedAt();

        try {
            String scope = (campaignId == null || campaignId.isBlank()) ? "all-campaigns" : campaignId;
            Path bundleDirectory = exportDirectory.resolve(scope).resolve("dashboard-bundle");
            Files.createDirectories(bundleDirectory);

            Path dashboardJsonPath = bundleDirectory.resolve("dashboard.json");
            Path dashboardCsvPath = bundleDirectory.resolve("dashboard.csv");
            Path dashboardMarkdownPath = bundleDirectory.resolve("dashboard.md");
            Path dashboardHtmlPath = bundleDirectory.resolve("dashboard.html");
            Path readmePath = bundleDirectory.resolve("README.txt");

            Files.writeString(dashboardJsonPath, writeJson(dashboard));
            Files.write(dashboardCsvPath, buildDashboardCsv(dashboard));
            Files.writeString(dashboardMarkdownPath, buildDashboardMarkdown(dashboard));
            Files.writeString(dashboardHtmlPath, buildDashboardHtml(dashboard));
            Files.writeString(readmePath, buildDashboardBundleReadme(dashboard));

            return new OperationalDashboardBundleResponse(
                    campaignId,
                    from,
                    to,
                    bundleDirectory.toAbsolutePath().toString(),
                    dashboardJsonPath.toAbsolutePath().toString(),
                    dashboardCsvPath.toAbsolutePath().toString(),
                    dashboardMarkdownPath.toAbsolutePath().toString(),
                    dashboardHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate operational dashboard bundle", e);
        }
    }

    private long countType(List<com.vantage.dialer.api.persistence.model.CallEventEntity> events, EventType eventType) {
        return events.stream().filter(event -> eventType.name().equals(event.getEventType())).count();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize operational dashboard payload", e);
        }
    }

    private List<String> buildDashboardCsv(OperationalDashboardResponse dashboard) {
        List<String> lines = new ArrayList<>();
        lines.add("campaignId,from,to,totalAgents,totalCallsHandled,totalAnsweredCalls,totalCompletedCalls,totalFailedCalls,campaignTotalSessions,campaignCompletedSessions,campaignFailedSessions,ivrTotalSessions,ivrCompletedSessions,ivrFailedSessions");
        lines.add(String.join(",",
                safe(dashboard.campaignId()),
                String.valueOf(dashboard.from()),
                String.valueOf(dashboard.to()),
                String.valueOf(dashboard.totalAgents()),
                String.valueOf(dashboard.totalCallsHandled()),
                String.valueOf(dashboard.totalAnsweredCalls()),
                String.valueOf(dashboard.totalCompletedCalls()),
                String.valueOf(dashboard.totalFailedCalls()),
                String.valueOf(dashboard.campaignSummary() == null ? null : dashboard.campaignSummary().totalSessions()),
                String.valueOf(dashboard.campaignSummary() == null ? null : dashboard.campaignSummary().completedSessions()),
                String.valueOf(dashboard.campaignSummary() == null ? null : dashboard.campaignSummary().failedSessions()),
                String.valueOf(dashboard.ivrSummary() == null ? null : dashboard.ivrSummary().totalIvrSessions()),
                String.valueOf(dashboard.ivrSummary() == null ? null : dashboard.ivrSummary().completedIvrSessions()),
                String.valueOf(dashboard.ivrSummary() == null ? null : dashboard.ivrSummary().failedIvrSessions())
        ));
        return lines;
    }

    private String buildDashboardMarkdown(OperationalDashboardResponse dashboard) {
        List<String> lines = new ArrayList<>();
        lines.add("# Operational Dashboard");
        lines.add("");
        lines.add("- Generated: " + dashboard.generatedAt());
        lines.add("- Campaign Id: " + safe(dashboard.campaignId()));
        lines.add("- From: " + dashboard.from());
        lines.add("- To: " + dashboard.to());
        lines.add("- Total Agents: " + dashboard.totalAgents());
        lines.add("- Total Calls Handled: " + dashboard.totalCallsHandled());
        lines.add("- Total Answered Calls: " + dashboard.totalAnsweredCalls());
        lines.add("- Total Completed Calls: " + dashboard.totalCompletedCalls());
        lines.add("- Total Failed Calls: " + dashboard.totalFailedCalls());
        if (dashboard.campaignSummary() != null) {
            lines.add("");
            lines.add("## Campaign Summary");
            lines.add("");
            lines.add("- Total Sessions: " + dashboard.campaignSummary().totalSessions());
            lines.add("- Completed Sessions: " + dashboard.campaignSummary().completedSessions());
            lines.add("- Failed Sessions: " + dashboard.campaignSummary().failedSessions());
            lines.add("- Bridged Sessions: " + dashboard.campaignSummary().bridgedSessions());
            lines.add("- Answered Sessions: " + dashboard.campaignSummary().answeredSessions());
        }
        if (dashboard.ivrSummary() != null) {
            lines.add("");
            lines.add("## IVR Summary");
            lines.add("");
            lines.add("- IVR Flow Id: " + safe(dashboard.ivrSummary().ivrFlowId()));
            lines.add("- Total IVR Sessions: " + dashboard.ivrSummary().totalIvrSessions());
            lines.add("- Completed IVR Sessions: " + dashboard.ivrSummary().completedIvrSessions());
            lines.add("- Failed IVR Sessions: " + dashboard.ivrSummary().failedIvrSessions());
        }
        lines.add("");
        lines.add("## Agent Activity");
        for (AgentActivitySummary item : dashboard.agentActivity()) {
            lines.add("- " + item.agentId() + ": handled=" + item.callsHandled()
                    + ", answered=" + item.answeredCalls()
                    + ", completed=" + item.completedCalls()
                    + ", failed=" + item.failedCalls());
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildDashboardReadme(OperationalDashboardResponse dashboard) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Operational Dashboard Export",
                "",
                "Generated At: " + dashboard.generatedAt(),
                "Campaign Id: " + safe(dashboard.campaignId()),
                "From: " + dashboard.from(),
                "To: " + dashboard.to(),
                "",
                "Files in this export:",
                "- dashboard.json",
                "- dashboard.csv",
                "- dashboard.md");
    }

    private String buildDashboardBundleReadme(OperationalDashboardResponse dashboard) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Operational Dashboard Bundle",
                "",
                "Generated At: " + dashboard.generatedAt(),
                "Campaign Id: " + safe(dashboard.campaignId()),
                "From: " + dashboard.from(),
                "To: " + dashboard.to(),
                "",
                "Files in this bundle:",
                "- dashboard.json",
                "- dashboard.csv",
                "- dashboard.md",
                "- dashboard.html");
    }

    private String buildDashboardHtml(OperationalDashboardResponse dashboard) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<title>Operational Dashboard</title>")
                .append("<style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;background:#f6f2ea;color:#1f2a30;padding:24px;}")
                .append(".card{background:#fff;border-radius:14px;padding:18px;margin-bottom:16px;box-shadow:0 8px 24px rgba(0,0,0,.08);}")
                .append("h1,h2{margin:0 0 12px;}table{width:100%;border-collapse:collapse;}th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left;}")
                .append(".muted{color:#5d6a70;}")
                .append("</style></head><body>");
        html.append("<h1>Operational Dashboard</h1>");
        html.append("<p class=\"muted\">Generated: ").append(dashboard.generatedAt()).append("</p>");

        html.append("<div class=\"card\"><h2>Summary</h2><table>");
        appendHtmlRow(html, "Campaign Id", safe(dashboard.campaignId()));
        appendHtmlRow(html, "From", String.valueOf(dashboard.from()));
        appendHtmlRow(html, "To", String.valueOf(dashboard.to()));
        appendHtmlRow(html, "Total Agents", String.valueOf(dashboard.totalAgents()));
        appendHtmlRow(html, "Total Calls Handled", String.valueOf(dashboard.totalCallsHandled()));
        appendHtmlRow(html, "Total Answered Calls", String.valueOf(dashboard.totalAnsweredCalls()));
        appendHtmlRow(html, "Total Completed Calls", String.valueOf(dashboard.totalCompletedCalls()));
        appendHtmlRow(html, "Total Failed Calls", String.valueOf(dashboard.totalFailedCalls()));
        html.append("</table></div>");

        if (dashboard.campaignSummary() != null) {
            html.append("<div class=\"card\"><h2>Campaign Summary</h2><table>");
            appendHtmlRow(html, "Total Sessions", String.valueOf(dashboard.campaignSummary().totalSessions()));
            appendHtmlRow(html, "Completed Sessions", String.valueOf(dashboard.campaignSummary().completedSessions()));
            appendHtmlRow(html, "Failed Sessions", String.valueOf(dashboard.campaignSummary().failedSessions()));
            appendHtmlRow(html, "Bridged Sessions", String.valueOf(dashboard.campaignSummary().bridgedSessions()));
            appendHtmlRow(html, "Answered Sessions", String.valueOf(dashboard.campaignSummary().answeredSessions()));
            html.append("</table></div>");
        }

        if (dashboard.ivrSummary() != null) {
            html.append("<div class=\"card\"><h2>IVR Summary</h2><table>");
            appendHtmlRow(html, "IVR Flow Id", safe(dashboard.ivrSummary().ivrFlowId()));
            appendHtmlRow(html, "Total IVR Sessions", String.valueOf(dashboard.ivrSummary().totalIvrSessions()));
            appendHtmlRow(html, "Completed IVR Sessions", String.valueOf(dashboard.ivrSummary().completedIvrSessions()));
            appendHtmlRow(html, "Failed IVR Sessions", String.valueOf(dashboard.ivrSummary().failedIvrSessions()));
            html.append("</table></div>");
        }

        html.append("<div class=\"card\"><h2>Agent Activity</h2><table>");
        html.append("<tr><th>Agent</th><th>Handled</th><th>Answered</th><th>Completed</th><th>Failed</th></tr>");
        for (AgentActivitySummary item : dashboard.agentActivity()) {
            html.append("<tr><td>").append(safe(item.agentId())).append("</td><td>")
                    .append(item.callsHandled()).append("</td><td>")
                    .append(item.answeredCalls()).append("</td><td>")
                    .append(item.completedCalls()).append("</td><td>")
                    .append(item.failedCalls()).append("</td></tr>");
        }
        html.append("</table></div></body></html>");
        return html.toString();
    }

    private void appendHtmlRow(StringBuilder html, String label, String value) {
        html.append("<tr><th>").append(label).append("</th><td>").append(value).append("</td></tr>");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String> buildCallTimelineCsv(List<StandardEvent> timeline) {
        List<String> lines = new ArrayList<>();
        lines.add("eventId,callSessionId,callLegId,eventType,timestamp,provider,providerCallId,legType,payload");
        for (StandardEvent event : timeline) {
            lines.add(String.join(",",
                    safe(event.getEventId()),
                    safe(event.getCallSessionId()),
                    safe(event.getCallLegId()),
                    event.getEventType() == null ? "" : event.getEventType().name(),
                    String.valueOf(event.getTimestamp()),
                    safe(event.getProvider()),
                    safe(event.getProviderCallId()),
                    safe(event.getLegType()),
                    safe(writeJson(event.getPayload()))
            ));
        }
        return lines;
    }

    private String buildCallTimelineMarkdown(CallSessionResponse session, List<StandardEvent> timeline, Instant generatedAt) {
        List<String> lines = new ArrayList<>();
        lines.add("# Call Timeline");
        lines.add("");
        lines.add("- Generated: " + generatedAt);
        lines.add("- Call Session Id: " + safe(session.callSessionId()));
        lines.add("- Campaign Id: " + safe(session.campaignId()));
        lines.add("- Customer Number: " + safe(session.customerNumber()));
        lines.add("- Agent Id: " + safe(session.agentId()));
        lines.add("- Agent Channel: " + safe(session.agentChannel()));
        lines.add("- Call Mode: " + safe(session.callMode()));
        lines.add("- Status: " + safe(session.status()));
        lines.add("- Last Event Type: " + safe(session.lastEventType()));
        lines.add("");
        lines.add("## Events");
        for (StandardEvent event : timeline) {
            lines.add("");
            lines.add("### " + (event.getEventType() == null ? "UNKNOWN" : event.getEventType().name()));
            lines.add("- Timestamp: " + event.getTimestamp());
            lines.add("- Event Id: " + safe(event.getEventId()));
            lines.add("- Call Leg Id: " + safe(event.getCallLegId()));
            lines.add("- Provider: " + safe(event.getProvider()));
            lines.add("- Provider Call Id: " + safe(event.getProviderCallId()));
            lines.add("- Leg Type: " + safe(event.getLegType()));
            lines.add("- Payload: " + safe(writeJson(event.getPayload())));
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildCallTimelineHtml(CallSessionResponse session, List<StandardEvent> timeline, Instant generatedAt) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<title>Call Timeline</title>")
                .append("<style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;background:#f7f3eb;color:#1f2a30;padding:24px;}")
                .append(".card{background:#fff;border-radius:14px;padding:18px;margin-bottom:16px;box-shadow:0 8px 24px rgba(0,0,0,.08);}")
                .append("h1,h2{margin:0 0 12px;}table{width:100%;border-collapse:collapse;}th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left;vertical-align:top;}")
                .append(".muted{color:#5d6a70;}")
                .append("</style></head><body>");
        html.append("<h1>Call Timeline</h1>");
        html.append("<p class=\"muted\">Generated: ").append(generatedAt).append("</p>");
        html.append("<div class=\"card\"><h2>Session</h2><table>");
        appendHtmlRow(html, "Call Session Id", safe(session.callSessionId()));
        appendHtmlRow(html, "Campaign Id", safe(session.campaignId()));
        appendHtmlRow(html, "Customer Number", safe(session.customerNumber()));
        appendHtmlRow(html, "Agent Id", safe(session.agentId()));
        appendHtmlRow(html, "Agent Channel", safe(session.agentChannel()));
        appendHtmlRow(html, "Call Mode", safe(session.callMode()));
        appendHtmlRow(html, "Status", safe(session.status()));
        appendHtmlRow(html, "Last Event Type", safe(session.lastEventType()));
        html.append("</table></div>");
        html.append("<div class=\"card\"><h2>Events</h2><table>");
        html.append("<tr><th>Timestamp</th><th>Type</th><th>Leg</th><th>Provider</th><th>Payload</th></tr>");
        for (StandardEvent event : timeline) {
            html.append("<tr><td>").append(String.valueOf(event.getTimestamp())).append("</td><td>")
                    .append(event.getEventType() == null ? "" : event.getEventType().name()).append("</td><td>")
                    .append(safe(event.getLegType())).append("</td><td>")
                    .append(safe(event.getProvider())).append("</td><td>")
                    .append(safe(writeJson(event.getPayload()))).append("</td></tr>");
        }
        html.append("</table></div></body></html>");
        return html.toString();
    }

    private String buildCallTimelineReadme(CallSessionResponse session, int eventCount, Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Call Timeline Bundle",
                "",
                "Generated At: " + generatedAt,
                "Call Session Id: " + safe(session.callSessionId()),
                "Campaign Id: " + safe(session.campaignId()),
                "Event Count: " + eventCount,
                "",
                "Files in this bundle:",
                "- session.json",
                "- timeline.json",
                "- timeline.csv",
                "- timeline.md",
                "- timeline.html");
    }

    private List<String> buildCampaignSessionsCsv(List<CallSessionResponse> sessions) {
        List<String> lines = new ArrayList<>();
        lines.add("callSessionId,campaignId,leadId,provider,customerNumber,agentId,agentChannel,callMode,ivrFlowId,status,lastEventType,lastEventAt,createdAt");
        for (CallSessionResponse session : sessions) {
            lines.add(String.join(",",
                    safe(session.callSessionId()),
                    safe(session.campaignId()),
                    safe(session.leadId()),
                    safe(session.provider()),
                    safe(session.customerNumber()),
                    safe(session.agentId()),
                    safe(session.agentChannel()),
                    safe(session.callMode()),
                    safe(session.ivrFlowId()),
                    safe(session.status()),
                    safe(session.lastEventType()),
                    String.valueOf(session.lastEventAt()),
                    String.valueOf(session.createdAt())
            ));
        }
        return lines;
    }

    private String buildCampaignSessionsMarkdown(String campaignId, List<CallSessionResponse> sessions, Instant generatedAt) {
        List<String> lines = new ArrayList<>();
        lines.add("# Campaign Sessions");
        lines.add("");
        lines.add("- Generated: " + generatedAt);
        lines.add("- Campaign Id: " + safe(campaignId));
        lines.add("- Session Count: " + sessions.size());
        lines.add("");
        lines.add("## Sessions");
        for (CallSessionResponse session : sessions) {
            lines.add("");
            lines.add("### " + safe(session.callSessionId()));
            lines.add("- Customer Number: " + safe(session.customerNumber()));
            lines.add("- Agent Id: " + safe(session.agentId()));
            lines.add("- Agent Channel: " + safe(session.agentChannel()));
            lines.add("- Call Mode: " + safe(session.callMode()));
            lines.add("- Status: " + safe(session.status()));
            lines.add("- Last Event Type: " + safe(session.lastEventType()));
            lines.add("- Created At: " + session.createdAt());
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildCampaignSessionsHtml(String campaignId, List<CallSessionResponse> sessions, Instant generatedAt) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<title>Campaign Sessions</title>")
                .append("<style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;background:#f7f3eb;color:#1f2a30;padding:24px;}")
                .append(".card{background:#fff;border-radius:14px;padding:18px;margin-bottom:16px;box-shadow:0 8px 24px rgba(0,0,0,.08);}")
                .append("h1,h2{margin:0 0 12px;}table{width:100%;border-collapse:collapse;}th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left;}")
                .append(".muted{color:#5d6a70;}")
                .append("</style></head><body>");
        html.append("<h1>Campaign Sessions</h1>");
        html.append("<p class=\"muted\">Generated: ").append(generatedAt).append("</p>");
        html.append("<div class=\"card\"><h2>Summary</h2><table>");
        appendHtmlRow(html, "Campaign Id", safe(campaignId));
        appendHtmlRow(html, "Session Count", String.valueOf(sessions.size()));
        html.append("</table></div>");
        html.append("<div class=\"card\"><h2>Sessions</h2><table>");
        html.append("<tr><th>Call Session</th><th>Customer</th><th>Agent</th><th>Mode</th><th>Status</th><th>Last Event</th></tr>");
        for (CallSessionResponse session : sessions) {
            html.append("<tr><td>").append(safe(session.callSessionId())).append("</td><td>")
                    .append(safe(session.customerNumber())).append("</td><td>")
                    .append(safe(session.agentId())).append("</td><td>")
                    .append(safe(session.callMode())).append("</td><td>")
                    .append(safe(session.status())).append("</td><td>")
                    .append(safe(session.lastEventType())).append("</td></tr>");
        }
        html.append("</table></div></body></html>");
        return html.toString();
    }

    private String buildCampaignSessionsReadme(String campaignId, int sessionCount, Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Campaign Sessions Bundle",
                "",
                "Generated At: " + generatedAt,
                "Campaign Id: " + safe(campaignId),
                "Session Count: " + sessionCount,
                "",
                "Files in this bundle:",
                "- sessions.json",
                "- sessions.csv",
                "- sessions.md",
                "- sessions.html");
    }

    private String buildCampaignReportMarkdown(CampaignSummaryResponse summary,
                                               IvrCampaignSummaryResponse ivrSummary,
                                               List<CallSessionResponse> sessions,
                                               Instant generatedAt) {
        List<String> lines = new ArrayList<>();
        lines.add("# Campaign Report");
        lines.add("");
        lines.add("- Generated: " + generatedAt);
        lines.add("- Campaign Id: " + safe(summary.campaignId()));
        lines.add("- Total Sessions: " + summary.totalSessions());
        lines.add("- Completed Sessions: " + summary.completedSessions());
        lines.add("- Failed Sessions: " + summary.failedSessions());
        lines.add("- Bridged Sessions: " + summary.bridgedSessions());
        lines.add("- Answered Sessions: " + summary.answeredSessions());
        lines.add("");
        lines.add("## IVR Summary");
        lines.add("");
        lines.add("- IVR Flow Id: " + safe(ivrSummary.ivrFlowId()));
        lines.add("- Total IVR Sessions: " + ivrSummary.totalIvrSessions());
        lines.add("- Completed IVR Sessions: " + ivrSummary.completedIvrSessions());
        lines.add("- Failed IVR Sessions: " + ivrSummary.failedIvrSessions());
        lines.add("");
        lines.add("## Sessions");
        for (CallSessionResponse session : sessions) {
            lines.add("- " + safe(session.callSessionId())
                    + " | customer=" + safe(session.customerNumber())
                    + " | agent=" + safe(session.agentId())
                    + " | status=" + safe(session.status())
                    + " | lastEvent=" + safe(session.lastEventType()));
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildCampaignReportHtml(CampaignSummaryResponse summary,
                                           IvrCampaignSummaryResponse ivrSummary,
                                           List<CallSessionResponse> sessions,
                                           Instant generatedAt) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<title>Campaign Report</title>")
                .append("<style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;background:#f7f3eb;color:#1f2a30;padding:24px;}")
                .append(".card{background:#fff;border-radius:14px;padding:18px;margin-bottom:16px;box-shadow:0 8px 24px rgba(0,0,0,.08);}")
                .append("h1,h2{margin:0 0 12px;}table{width:100%;border-collapse:collapse;}th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left;}")
                .append(".muted{color:#5d6a70;}")
                .append("</style></head><body>");
        html.append("<h1>Campaign Report</h1>");
        html.append("<p class=\"muted\">Generated: ").append(generatedAt).append("</p>");

        html.append("<div class=\"card\"><h2>Campaign Summary</h2><table>");
        appendHtmlRow(html, "Campaign Id", safe(summary.campaignId()));
        appendHtmlRow(html, "Total Sessions", String.valueOf(summary.totalSessions()));
        appendHtmlRow(html, "Completed Sessions", String.valueOf(summary.completedSessions()));
        appendHtmlRow(html, "Failed Sessions", String.valueOf(summary.failedSessions()));
        appendHtmlRow(html, "Bridged Sessions", String.valueOf(summary.bridgedSessions()));
        appendHtmlRow(html, "Answered Sessions", String.valueOf(summary.answeredSessions()));
        html.append("</table></div>");

        html.append("<div class=\"card\"><h2>IVR Summary</h2><table>");
        appendHtmlRow(html, "IVR Flow Id", safe(ivrSummary.ivrFlowId()));
        appendHtmlRow(html, "Total IVR Sessions", String.valueOf(ivrSummary.totalIvrSessions()));
        appendHtmlRow(html, "Completed IVR Sessions", String.valueOf(ivrSummary.completedIvrSessions()));
        appendHtmlRow(html, "Failed IVR Sessions", String.valueOf(ivrSummary.failedIvrSessions()));
        html.append("</table></div>");

        html.append("<div class=\"card\"><h2>Sessions</h2><table>");
        html.append("<tr><th>Call Session</th><th>Customer</th><th>Agent</th><th>Status</th><th>Last Event</th></tr>");
        for (CallSessionResponse session : sessions) {
            html.append("<tr><td>").append(safe(session.callSessionId())).append("</td><td>")
                    .append(safe(session.customerNumber())).append("</td><td>")
                    .append(safe(session.agentId())).append("</td><td>")
                    .append(safe(session.status())).append("</td><td>")
                    .append(safe(session.lastEventType())).append("</td></tr>");
        }
        html.append("</table></div></body></html>");
        return html.toString();
    }

    private String buildCampaignReportReadme(String campaignId, int sessionCount, Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Campaign Report Bundle",
                "",
                "Generated At: " + generatedAt,
                "Campaign Id: " + safe(campaignId),
                "Session Count: " + sessionCount,
                "",
                "Files in this bundle:",
                "- summary.json",
                "- ivr-summary.json",
                "- sessions.json",
                "- report.md",
                "- report.html");
    }

    private String buildIvrSummaryMarkdown(IvrCampaignSummaryResponse summary, Instant generatedAt) {
        List<String> lines = new ArrayList<>();
        lines.add("# IVR Campaign Summary");
        lines.add("");
        lines.add("- Generated: " + generatedAt);
        lines.add("- Campaign Id: " + safe(summary.campaignId()));
        lines.add("- IVR Flow Id: " + safe(summary.ivrFlowId()));
        lines.add("- Total IVR Sessions: " + summary.totalIvrSessions());
        lines.add("- Completed IVR Sessions: " + summary.completedIvrSessions());
        lines.add("- Failed IVR Sessions: " + summary.failedIvrSessions());
        return String.join(System.lineSeparator(), lines);
    }

    private String buildIvrSummaryHtml(IvrCampaignSummaryResponse summary, Instant generatedAt) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<title>IVR Campaign Summary</title>")
                .append("<style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;background:#f7f3eb;color:#1f2a30;padding:24px;}")
                .append(".card{background:#fff;border-radius:14px;padding:18px;margin-bottom:16px;box-shadow:0 8px 24px rgba(0,0,0,.08);}")
                .append("h1,h2{margin:0 0 12px;}table{width:100%;border-collapse:collapse;}th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left;}")
                .append(".muted{color:#5d6a70;}")
                .append("</style></head><body>");
        html.append("<h1>IVR Campaign Summary</h1>");
        html.append("<p class=\"muted\">Generated: ").append(generatedAt).append("</p>");
        html.append("<div class=\"card\"><h2>Summary</h2><table>");
        appendHtmlRow(html, "Campaign Id", safe(summary.campaignId()));
        appendHtmlRow(html, "IVR Flow Id", safe(summary.ivrFlowId()));
        appendHtmlRow(html, "Total IVR Sessions", String.valueOf(summary.totalIvrSessions()));
        appendHtmlRow(html, "Completed IVR Sessions", String.valueOf(summary.completedIvrSessions()));
        appendHtmlRow(html, "Failed IVR Sessions", String.valueOf(summary.failedIvrSessions()));
        html.append("</table></div></body></html>");
        return html.toString();
    }

    private String buildIvrSummaryReadme(IvrCampaignSummaryResponse summary, Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer IVR Campaign Bundle",
                "",
                "Generated At: " + generatedAt,
                "Campaign Id: " + safe(summary.campaignId()),
                "IVR Flow Id: " + safe(summary.ivrFlowId()),
                "",
                "Files in this bundle:",
                "- ivr-summary.json",
                "- ivr-summary.md",
                "- ivr-summary.html");
    }

    private List<String> buildAgentActivityCsv(List<AgentActivitySummary> activity) {
        List<String> lines = new ArrayList<>();
        lines.add("agentId,callsHandled,answeredCalls,bridgedCalls,completedCalls,failedCalls");
        for (AgentActivitySummary item : activity) {
            lines.add(String.join(",",
                    safe(item.agentId()),
                    String.valueOf(item.callsHandled()),
                    String.valueOf(item.answeredCalls()),
                    String.valueOf(item.bridgedCalls()),
                    String.valueOf(item.completedCalls()),
                    String.valueOf(item.failedCalls())
            ));
        }
        return lines;
    }

    private String buildAgentActivityMarkdown(List<AgentActivitySummary> activity, Instant from, Instant to, Instant generatedAt) {
        List<String> lines = new ArrayList<>();
        lines.add("# Agent Activity");
        lines.add("");
        lines.add("- Generated: " + generatedAt);
        lines.add("- From: " + from);
        lines.add("- To: " + to);
        lines.add("- Agent Count: " + activity.size());
        lines.add("");
        lines.add("## Agents");
        for (AgentActivitySummary item : activity) {
            lines.add("- " + safe(item.agentId())
                    + ": handled=" + item.callsHandled()
                    + ", answered=" + item.answeredCalls()
                    + ", bridged=" + item.bridgedCalls()
                    + ", completed=" + item.completedCalls()
                    + ", failed=" + item.failedCalls());
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildAgentActivityHtml(List<AgentActivitySummary> activity, Instant from, Instant to, Instant generatedAt) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<title>Agent Activity</title>")
                .append("<style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;background:#f7f3eb;color:#1f2a30;padding:24px;}")
                .append(".card{background:#fff;border-radius:14px;padding:18px;margin-bottom:16px;box-shadow:0 8px 24px rgba(0,0,0,.08);}")
                .append("h1,h2{margin:0 0 12px;}table{width:100%;border-collapse:collapse;}th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left;}")
                .append(".muted{color:#5d6a70;}")
                .append("</style></head><body>");
        html.append("<h1>Agent Activity</h1>");
        html.append("<p class=\"muted\">Generated: ").append(generatedAt).append("</p>");
        html.append("<div class=\"card\"><h2>Summary</h2><table>");
        appendHtmlRow(html, "From", String.valueOf(from));
        appendHtmlRow(html, "To", String.valueOf(to));
        appendHtmlRow(html, "Agent Count", String.valueOf(activity.size()));
        html.append("</table></div>");
        html.append("<div class=\"card\"><h2>Agents</h2><table>");
        html.append("<tr><th>Agent</th><th>Handled</th><th>Answered</th><th>Bridged</th><th>Completed</th><th>Failed</th></tr>");
        for (AgentActivitySummary item : activity) {
            html.append("<tr><td>").append(safe(item.agentId())).append("</td><td>")
                    .append(item.callsHandled()).append("</td><td>")
                    .append(item.answeredCalls()).append("</td><td>")
                    .append(item.bridgedCalls()).append("</td><td>")
                    .append(item.completedCalls()).append("</td><td>")
                    .append(item.failedCalls()).append("</td></tr>");
        }
        html.append("</table></div></body></html>");
        return html.toString();
    }

    private String buildAgentActivityReadme(int agentCount, Instant from, Instant to, Instant generatedAt) {
        return String.join(System.lineSeparator(),
                "Vantage Dialer Agent Activity Bundle",
                "",
                "Generated At: " + generatedAt,
                "From: " + from,
                "To: " + to,
                "Agent Count: " + agentCount,
                "",
                "Files in this bundle:",
                "- agent-activity.json",
                "- agent-activity.csv",
                "- agent-activity.md",
                "- agent-activity.html");
    }
}
