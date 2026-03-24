package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.AgentActivityBundleResponse;
import com.vantage.dialer.api.dto.AgentActivitySummary;
import com.vantage.dialer.api.dto.AsteriskDeploymentAuditResponse;
import com.vantage.dialer.api.dto.CampaignSummaryResponse;
import com.vantage.dialer.api.dto.CampaignReportBundleResponse;
import com.vantage.dialer.api.dto.CampaignSessionsBundleResponse;
import com.vantage.dialer.api.dto.IvrCampaignBundleResponse;
import com.vantage.dialer.api.dto.IvrCampaignSummaryResponse;
import com.vantage.dialer.api.dto.OperationalDashboardBundleResponse;
import com.vantage.dialer.api.dto.OperationalDashboardResponse;
import com.vantage.dialer.api.dto.OperationalDashboardExportResponse;
import com.vantage.dialer.api.dto.ReportExportRequest;
import com.vantage.dialer.api.dto.ReportExportResponse;
import com.vantage.dialer.api.service.AsteriskDeploymentAuditService;
import com.vantage.dialer.api.service.ExportService;
import com.vantage.dialer.api.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportingControllerTest {

    @Test
    void campaignSummaryParsesExplicitInstantQueryParameters() throws Exception {
        ReportingService reportingService = mock(ReportingService.class);
        ExportService exportService = mock(ExportService.class);
        AsteriskDeploymentAuditService deploymentAuditService = mock(AsteriskDeploymentAuditService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new ReportingController(reportingService, exportService, deploymentAuditService));

        Instant from = Instant.parse("2026-03-20T00:00:00Z");
        Instant to = Instant.parse("2026-03-21T00:00:00Z");
        when(reportingService.getCampaignSummary("camp-1", from, to))
                .thenReturn(new CampaignSummaryResponse("camp-1", 10, 6, 1, 5, 7));

        mockMvc.perform(get("/reports/campaigns/camp-1/summary")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-1"))
                .andExpect(jsonPath("$.completedSessions").value(6));

        verify(reportingService).getCampaignSummary("camp-1", from, to);
    }

    @Test
    void agentActivityBundleAndDashboardExportParseQueryParameters() throws Exception {
        ReportingService reportingService = mock(ReportingService.class);
        ExportService exportService = mock(ExportService.class);
        AsteriskDeploymentAuditService deploymentAuditService = mock(AsteriskDeploymentAuditService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new ReportingController(reportingService, exportService, deploymentAuditService));

        Instant from = Instant.parse("2026-03-19T00:00:00Z");
        Instant to = Instant.parse("2026-03-20T00:00:00Z");
        when(reportingService.getAgentActivity(from, to))
                .thenReturn(List.of(new AgentActivitySummary("agent-1", 4, 3, 2, 2, 1)));
        when(reportingService.generateAgentActivityBundle(from, to))
                .thenReturn(new AgentActivityBundleResponse(
                        from, to, "exports/agents", "agent.json", "agent.csv", "agent.md", "agent.html", "README.txt",
                        Instant.parse("2026-03-20T01:30:00Z")
                ));
        when(reportingService.exportOperationalDashboard("camp-9", from, to))
                .thenReturn(new OperationalDashboardExportResponse(
                        "camp-9", from, to, "exports/dashboard", "dashboard.json", "dashboard.csv", "dashboard.md", "README.txt",
                        Instant.parse("2026-03-20T01:00:00Z")
                ));

        mockMvc.perform(get("/reports/agents/activity")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agentId").value("agent-1"))
                .andExpect(jsonPath("$[0].callsHandled").value(4));

        mockMvc.perform(post("/reports/agents/activity/bundle")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectoryPath").value("exports/agents"));

        mockMvc.perform(post("/reports/dashboard/export")
                        .queryParam("campaignId", "camp-9")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-9"))
                .andExpect(jsonPath("$.dashboardJsonPath").value("dashboard.json"));

        verify(reportingService).getAgentActivity(from, to);
        verify(reportingService).generateAgentActivityBundle(from, to);
        verify(reportingService).exportOperationalDashboard("camp-9", from, to);
    }

    @Test
    void exportEndpointsDelegateRequestBodiesAndPathVariables() throws Exception {
        ReportingService reportingService = mock(ReportingService.class);
        ExportService exportService = mock(ExportService.class);
        AsteriskDeploymentAuditService deploymentAuditService = mock(AsteriskDeploymentAuditService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new ReportingController(reportingService, exportService, deploymentAuditService));

        ReportExportRequest request = new ReportExportRequest();
        request.setExportType("campaign-summary");
        request.setCampaignId("camp-2");
        request.setFrom("2026-03-20T00:00:00Z");
        request.setTo("2026-03-21T00:00:00Z");

        when(exportService.generateCampaignSummaryExport(any(ReportExportRequest.class)))
                .thenReturn(new ReportExportResponse(
                        "export-1", "campaign-summary", "COMPLETED", "exports/campaign.csv", 12L,
                        Instant.parse("2026-03-20T02:00:00Z"), Instant.parse("2026-03-20T02:01:00Z"), null
                ));
        when(exportService.getExport("export-1"))
                .thenReturn(new ReportExportResponse(
                        "export-1", "campaign-summary", "COMPLETED", "exports/campaign.csv", 12L,
                        Instant.parse("2026-03-20T02:00:00Z"), Instant.parse("2026-03-20T02:01:00Z"), null
                ));

        mockMvc.perform(post("/reports/exports")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportJobId").value("export-1"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/reports/exports/export-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filePath").value("exports/campaign.csv"));

        ArgumentCaptor<ReportExportRequest> captor = ArgumentCaptor.forClass(ReportExportRequest.class);
        verify(exportService).generateCampaignSummaryExport(captor.capture());
        assertEquals("campaign-summary", captor.getValue().getExportType());
        assertEquals("camp-2", captor.getValue().getCampaignId());
        assertEquals("2026-03-20T00:00:00Z", captor.getValue().getFrom());
        assertEquals("2026-03-21T00:00:00Z", captor.getValue().getTo());
        verify(exportService).getExport("export-1");
    }

    @Test
    void deploymentEndpointsReturnHistoryAndSpecificDeployment() throws Exception {
        ReportingService reportingService = mock(ReportingService.class);
        ExportService exportService = mock(ExportService.class);
        AsteriskDeploymentAuditService deploymentAuditService = mock(AsteriskDeploymentAuditService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new ReportingController(reportingService, exportService, deploymentAuditService));

        AsteriskDeploymentAuditResponse audit = new AsteriskDeploymentAuditResponse(
                "deploy-1", "pkg-1", "FULL", "SOFTPHONE", "COMPLETED", true, true,
                "host-1", 22, "/remote", "/remote/pkg", "/target",
                List.of("scp"), List.of("package.tar.gz"), List.of("A1"),
                Instant.parse("2026-03-20T00:00:00Z"),
                Instant.parse("2026-03-20T00:05:00Z"),
                Instant.parse("2026-03-20T00:00:00Z"),
                "Deployment completed", null
        );
        when(deploymentAuditService.list("pkg-1")).thenReturn(List.of(audit));
        when(deploymentAuditService.get("deploy-1")).thenReturn(audit);

        mockMvc.perform(get("/reports/deployments").queryParam("packageId", "pkg-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deploymentJobId").value("deploy-1"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        mockMvc.perform(get("/reports/deployments/deploy-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageId").value("pkg-1"))
                .andExpect(jsonPath("$.host").value("host-1"));

        verify(deploymentAuditService).list("pkg-1");
        verify(deploymentAuditService).get("deploy-1");
    }

    @Test
    void campaignBundleEndpointsDelegateWithExplicitTimeRange() throws Exception {
        ReportingService reportingService = mock(ReportingService.class);
        ExportService exportService = mock(ExportService.class);
        AsteriskDeploymentAuditService deploymentAuditService = mock(AsteriskDeploymentAuditService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new ReportingController(reportingService, exportService, deploymentAuditService));

        Instant from = Instant.parse("2026-03-18T00:00:00Z");
        Instant to = Instant.parse("2026-03-19T00:00:00Z");
        when(reportingService.generateCampaignSessionsBundle("camp-3"))
                .thenReturn(new CampaignSessionsBundleResponse(
                        "camp-3", "exports/campaign-sessions", "sessions.json", "sessions.csv", "sessions.md", "sessions.html", "README.txt",
                        Instant.parse("2026-03-19T01:00:00Z")
                ));
        when(reportingService.generateCampaignReportBundle("camp-3", from, to))
                .thenReturn(new CampaignReportBundleResponse(
                        "camp-3", from, to, "exports/campaign-report", "summary.json", "ivr.json", "sessions.json", "report.md", "report.html", "README.txt",
                        Instant.parse("2026-03-19T01:05:00Z")
                ));

        mockMvc.perform(post("/reports/campaigns/camp-3/sessions/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-3"))
                .andExpect(jsonPath("$.bundleDirectoryPath").value("exports/campaign-sessions"));

        mockMvc.perform(post("/reports/campaigns/camp-3/bundle")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-3"))
                .andExpect(jsonPath("$.bundleDirectoryPath").value("exports/campaign-report"));

        verify(reportingService).generateCampaignSessionsBundle("camp-3");
        verify(reportingService).generateCampaignReportBundle("camp-3", from, to);
    }

    @Test
    void ivrAndDashboardEndpointsUseDefaultTimeWindowWhenQueryParamsAreOmitted() throws Exception {
        ReportingService reportingService = mock(ReportingService.class);
        ExportService exportService = mock(ExportService.class);
        AsteriskDeploymentAuditService deploymentAuditService = mock(AsteriskDeploymentAuditService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new ReportingController(reportingService, exportService, deploymentAuditService));

        when(reportingService.getIvrSummary(any(), any(), any()))
                .thenReturn(new IvrCampaignSummaryResponse("camp-9", "ivr-9", 10, 8, 2));
        when(reportingService.generateIvrCampaignBundle(any(), any(), any()))
                .thenReturn(new IvrCampaignBundleResponse(
                        "camp-9", Instant.parse("2026-03-10T00:00:00Z"), Instant.parse("2026-03-17T00:00:00Z"),
                        "exports/ivr", "summary.json", "summary.md", "summary.html", "README.txt", Instant.parse("2026-03-17T01:00:00Z")
                ));
        when(reportingService.getOperationalDashboard(any(), any(), any()))
                .thenReturn(new OperationalDashboardResponse(
                        "camp-9",
                        Instant.parse("2026-03-10T00:00:00Z"),
                        Instant.parse("2026-03-17T00:00:00Z"),
                        Instant.parse("2026-03-17T01:05:00Z"),
                        new CampaignSummaryResponse("camp-9", 12, 8, 2, 7, 5),
                        new IvrCampaignSummaryResponse("camp-9", "ivr-9", 10, 8, 2),
                        List.of(new AgentActivitySummary("agent-1", 4, 3, 2, 2, 1)),
                        1,
                        4,
                        3,
                        2,
                        1
                ));
        when(reportingService.generateOperationalDashboardBundle(any(), any(), any()))
                .thenReturn(new OperationalDashboardBundleResponse(
                        "camp-9",
                        Instant.parse("2026-03-10T00:00:00Z"),
                        Instant.parse("2026-03-17T00:00:00Z"),
                        "exports/dashboard-bundle",
                        "dashboard.json",
                        "dashboard.csv",
                        "dashboard.md",
                        "dashboard.html",
                        "README.txt",
                        Instant.parse("2026-03-17T01:10:00Z")
                ));

        Instant before = Instant.now();

        mockMvc.perform(get("/reports/ivr/camp-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-9"))
                .andExpect(jsonPath("$.ivrFlowId").value("ivr-9"));

        mockMvc.perform(post("/reports/ivr/camp-9/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectoryPath").value("exports/ivr"));

        mockMvc.perform(get("/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignSummary.campaignId").value("camp-9"))
                .andExpect(jsonPath("$.totalCallsHandled").value(4));

        mockMvc.perform(post("/reports/dashboard/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectoryPath").value("exports/dashboard-bundle"));

        Instant after = Instant.now();

        ArgumentCaptor<Instant> ivrFromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> ivrToCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(reportingService).getIvrSummary(org.mockito.Mockito.eq("camp-9"), ivrFromCaptor.capture(), ivrToCaptor.capture());

        ArgumentCaptor<Instant> ivrBundleFromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> ivrBundleToCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(reportingService).generateIvrCampaignBundle(org.mockito.Mockito.eq("camp-9"), ivrBundleFromCaptor.capture(), ivrBundleToCaptor.capture());

        ArgumentCaptor<Instant> dashboardFromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> dashboardToCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(reportingService).getOperationalDashboard(org.mockito.Mockito.isNull(), dashboardFromCaptor.capture(), dashboardToCaptor.capture());

        ArgumentCaptor<Instant> dashboardBundleFromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> dashboardBundleToCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(reportingService).generateOperationalDashboardBundle(org.mockito.Mockito.isNull(), dashboardBundleFromCaptor.capture(), dashboardBundleToCaptor.capture());

        assertDefaultRange(before, after, ivrFromCaptor.getValue(), ivrToCaptor.getValue());
        assertDefaultRange(before, after, ivrBundleFromCaptor.getValue(), ivrBundleToCaptor.getValue());
        assertDefaultRange(before, after, dashboardFromCaptor.getValue(), dashboardToCaptor.getValue());
        assertDefaultRange(before, after, dashboardBundleFromCaptor.getValue(), dashboardBundleToCaptor.getValue());
    }

    @Test
    void campaignSummaryAndAgentActivityEndpointsUseDefaultTimeWindowWhenQueryParamsAreOmitted() throws Exception {
        ReportingService reportingService = mock(ReportingService.class);
        ExportService exportService = mock(ExportService.class);
        AsteriskDeploymentAuditService deploymentAuditService = mock(AsteriskDeploymentAuditService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new ReportingController(reportingService, exportService, deploymentAuditService));

        when(reportingService.getCampaignSummary(any(), any(), any()))
                .thenReturn(new CampaignSummaryResponse("camp-7", 20, 11, 3, 9, 8));
        when(reportingService.getAgentActivity(any(), any()))
                .thenReturn(List.of(new AgentActivitySummary("agent-7", 7, 5, 3, 2, 1)));

        Instant before = Instant.now();

        mockMvc.perform(get("/reports/campaigns/camp-7/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-7"))
                .andExpect(jsonPath("$.completedSessions").value(11));

        mockMvc.perform(get("/reports/agents/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agentId").value("agent-7"))
                .andExpect(jsonPath("$[0].callsHandled").value(7));

        Instant after = Instant.now();

        ArgumentCaptor<Instant> summaryFromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> summaryToCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(reportingService).getCampaignSummary(org.mockito.Mockito.eq("camp-7"), summaryFromCaptor.capture(), summaryToCaptor.capture());

        ArgumentCaptor<Instant> activityFromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> activityToCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(reportingService).getAgentActivity(activityFromCaptor.capture(), activityToCaptor.capture());

        assertDefaultRange(before, after, summaryFromCaptor.getValue(), summaryToCaptor.getValue());
        assertDefaultRange(before, after, activityFromCaptor.getValue(), activityToCaptor.getValue());
    }

    @Test
    void dashboardExportAndDeploymentHistoryAllowOmittedOptionalParams() throws Exception {
        ReportingService reportingService = mock(ReportingService.class);
        ExportService exportService = mock(ExportService.class);
        AsteriskDeploymentAuditService deploymentAuditService = mock(AsteriskDeploymentAuditService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new ReportingController(reportingService, exportService, deploymentAuditService));

        AsteriskDeploymentAuditResponse audit = new AsteriskDeploymentAuditResponse(
                "deploy-2", "pkg-2", "FULL", "WEBRTC", "PENDING", true, false,
                "host-2", 22, "/remote", "/remote/pkg", "/target",
                List.of("scp"), List.of("package.tar.gz"), List.of("A2"),
                Instant.parse("2026-03-21T00:00:00Z"),
                null,
                Instant.parse("2026-03-21T00:00:00Z"),
                "Pending deployment", null
        );
        when(reportingService.exportOperationalDashboard(any(), any(), any()))
                .thenReturn(new OperationalDashboardExportResponse(
                        null,
                        Instant.parse("2026-03-10T00:00:00Z"),
                        Instant.parse("2026-03-17T00:00:00Z"),
                        "exports/dashboard-default",
                        "dashboard.json",
                        "dashboard.csv",
                        "dashboard.md",
                        "README.txt",
                        Instant.parse("2026-03-17T01:00:00Z")
                ));
        when(deploymentAuditService.list(null)).thenReturn(List.of(audit));

        Instant before = Instant.now();

        mockMvc.perform(post("/reports/dashboard/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboardJsonPath").value("dashboard.json"))
                .andExpect(jsonPath("$.exportDirectoryPath").value("exports/dashboard-default"));

        mockMvc.perform(get("/reports/deployments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deploymentJobId").value("deploy-2"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        Instant after = Instant.now();

        ArgumentCaptor<Instant> exportFromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> exportToCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<String> campaignCaptor = ArgumentCaptor.forClass(String.class);
        verify(reportingService).exportOperationalDashboard(campaignCaptor.capture(), exportFromCaptor.capture(), exportToCaptor.capture());
        assertNull(campaignCaptor.getValue());
        assertDefaultRange(before, after, exportFromCaptor.getValue(), exportToCaptor.getValue());
        verify(deploymentAuditService).list(null);
    }

    private void assertDefaultRange(Instant before, Instant after, Instant actualFrom, Instant actualTo) {
        Instant earliestFrom = before.minus(7, ChronoUnit.DAYS);
        Instant latestFrom = after.minus(7, ChronoUnit.DAYS);
        org.junit.jupiter.api.Assertions.assertFalse(actualFrom.isBefore(earliestFrom));
        org.junit.jupiter.api.Assertions.assertFalse(actualFrom.isAfter(latestFrom));
        org.junit.jupiter.api.Assertions.assertFalse(actualTo.isBefore(before));
        org.junit.jupiter.api.Assertions.assertFalse(actualTo.isAfter(after));
    }
}
