package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.AgentActivitySummary;
import com.vantage.dialer.api.dto.AgentActivityBundleResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentAuditResponse;
import com.vantage.dialer.api.dto.CampaignSummaryResponse;
import com.vantage.dialer.api.dto.CampaignSessionsBundleResponse;
import com.vantage.dialer.api.dto.CampaignReportBundleResponse;
import com.vantage.dialer.api.dto.IvrCampaignBundleResponse;
import com.vantage.dialer.api.dto.IvrCampaignSummaryResponse;
import com.vantage.dialer.api.dto.OperationalDashboardBundleResponse;
import com.vantage.dialer.api.dto.OperationalDashboardExportResponse;
import com.vantage.dialer.api.dto.OperationalDashboardResponse;
import com.vantage.dialer.api.dto.ReportExportRequest;
import com.vantage.dialer.api.dto.ReportExportResponse;
import com.vantage.dialer.api.service.AsteriskDeploymentAuditService;
import com.vantage.dialer.api.service.ExportService;
import com.vantage.dialer.api.service.ReportingService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportingController {

    private final ReportingService reportingService;
    private final ExportService exportService;
    private final AsteriskDeploymentAuditService deploymentAuditService;

    public ReportingController(ReportingService reportingService,
                               ExportService exportService,
                               AsteriskDeploymentAuditService deploymentAuditService) {
        this.reportingService = reportingService;
        this.exportService = exportService;
        this.deploymentAuditService = deploymentAuditService;
    }

    @GetMapping("/campaigns/{campaignId}/summary")
    public CampaignSummaryResponse getCampaignSummary(@PathVariable String campaignId,
                                                      @RequestParam(required = false) String from,
                                                      @RequestParam(required = false) String to) {
        return reportingService.getCampaignSummary(campaignId, parseInstant(from, Instant.now().minus(7, ChronoUnit.DAYS)), parseInstant(to, Instant.now()));
    }

    @PostMapping("/campaigns/{campaignId}/sessions/bundle")
    public CampaignSessionsBundleResponse generateCampaignSessionsBundle(@PathVariable String campaignId) {
        return reportingService.generateCampaignSessionsBundle(campaignId);
    }

    @PostMapping("/campaigns/{campaignId}/bundle")
    public CampaignReportBundleResponse generateCampaignReportBundle(@PathVariable String campaignId,
                                                                     @RequestParam(required = false) String from,
                                                                     @RequestParam(required = false) String to) {
        return reportingService.generateCampaignReportBundle(
                campaignId,
                parseInstant(from, Instant.now().minus(7, ChronoUnit.DAYS)),
                parseInstant(to, Instant.now()));
    }

    @GetMapping("/agents/activity")
    public List<AgentActivitySummary> getAgentActivity(@RequestParam(required = false) String from,
                                                       @RequestParam(required = false) String to) {
        return reportingService.getAgentActivity(parseInstant(from, Instant.now().minus(7, ChronoUnit.DAYS)), parseInstant(to, Instant.now()));
    }

    @PostMapping("/agents/activity/bundle")
    public AgentActivityBundleResponse generateAgentActivityBundle(@RequestParam(required = false) String from,
                                                                   @RequestParam(required = false) String to) {
        return reportingService.generateAgentActivityBundle(
                parseInstant(from, Instant.now().minus(7, ChronoUnit.DAYS)),
                parseInstant(to, Instant.now()));
    }

    @GetMapping("/ivr/{campaignId}")
    public IvrCampaignSummaryResponse getIvrSummary(@PathVariable String campaignId,
                                                    @RequestParam(required = false) String from,
                                                    @RequestParam(required = false) String to) {
        return reportingService.getIvrSummary(campaignId, parseInstant(from, Instant.now().minus(7, ChronoUnit.DAYS)), parseInstant(to, Instant.now()));
    }

    @PostMapping("/ivr/{campaignId}/bundle")
    public IvrCampaignBundleResponse generateIvrCampaignBundle(@PathVariable String campaignId,
                                                               @RequestParam(required = false) String from,
                                                               @RequestParam(required = false) String to) {
        return reportingService.generateIvrCampaignBundle(
                campaignId,
                parseInstant(from, Instant.now().minus(7, ChronoUnit.DAYS)),
                parseInstant(to, Instant.now()));
    }

    @GetMapping("/dashboard")
    public OperationalDashboardResponse getOperationalDashboard(@RequestParam(required = false) String campaignId,
                                                                @RequestParam(required = false) String from,
                                                                @RequestParam(required = false) String to) {
        return reportingService.getOperationalDashboard(
                campaignId,
                parseInstant(from, Instant.now().minus(7, ChronoUnit.DAYS)),
                parseInstant(to, Instant.now()));
    }

    @PostMapping("/dashboard/export")
    public OperationalDashboardExportResponse exportOperationalDashboard(@RequestParam(required = false) String campaignId,
                                                                         @RequestParam(required = false) String from,
                                                                         @RequestParam(required = false) String to) {
        return reportingService.exportOperationalDashboard(
                campaignId,
                parseInstant(from, Instant.now().minus(7, ChronoUnit.DAYS)),
                parseInstant(to, Instant.now()));
    }

    @PostMapping("/dashboard/bundle")
    public OperationalDashboardBundleResponse generateOperationalDashboardBundle(@RequestParam(required = false) String campaignId,
                                                                                 @RequestParam(required = false) String from,
                                                                                 @RequestParam(required = false) String to) {
        return reportingService.generateOperationalDashboardBundle(
                campaignId,
                parseInstant(from, Instant.now().minus(7, ChronoUnit.DAYS)),
                parseInstant(to, Instant.now()));
    }

    @PostMapping("/exports")
    public ReportExportResponse export(@RequestBody ReportExportRequest request) {
        return exportService.generateCampaignSummaryExport(request);
    }

    @GetMapping("/exports/{exportJobId}")
    public ReportExportResponse getExport(@PathVariable String exportJobId) {
        return exportService.getExport(exportJobId);
    }

    @GetMapping("/deployments")
    public List<AsteriskDeploymentAuditResponse> getDeploymentHistory(@RequestParam(required = false) String packageId) {
        return deploymentAuditService.list(packageId);
    }

    @GetMapping("/deployments/{deploymentJobId}")
    public AsteriskDeploymentAuditResponse getDeployment(@PathVariable String deploymentJobId) {
        return deploymentAuditService.get(deploymentJobId);
    }

    private Instant parseInstant(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Instant.parse(value);
    }
}
