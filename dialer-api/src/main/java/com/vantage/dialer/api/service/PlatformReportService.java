package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.PlatformReportBundleResponse;
import com.vantage.dialer.api.dto.PlatformReportExportResponse;
import com.vantage.dialer.api.dto.PlatformReportResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlatformReportService {

    private final PlatformControlCenterService platformControlCenterService;
    private final PlatformWorkspaceService platformWorkspaceService;
    private final PlatformHealthService platformHealthService;
    private final PlatformArtifactCatalogService platformArtifactCatalogService;
    private final PlatformDeliveryPackageService platformDeliveryPackageService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public PlatformReportService(PlatformControlCenterService platformControlCenterService,
                                 PlatformWorkspaceService platformWorkspaceService,
                                 PlatformHealthService platformHealthService,
                                 @Lazy
                                 PlatformArtifactCatalogService platformArtifactCatalogService,
                                 PlatformDeliveryPackageService platformDeliveryPackageService,
                                 ObjectMapper objectMapper,
                                 @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.platformControlCenterService = platformControlCenterService;
        this.platformWorkspaceService = platformWorkspaceService;
        this.platformHealthService = platformHealthService;
        this.platformArtifactCatalogService = platformArtifactCatalogService;
        this.platformDeliveryPackageService = platformDeliveryPackageService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("platform-report");
    }

    public PlatformReportResponse report() {
        var health = platformHealthService.health();
        return new PlatformReportResponse(
                Instant.now(),
                health,
                platformControlCenterService.controlCenter(),
                platformWorkspaceService.workspace(),
                health.healthyCustomers(),
                health.customersWithReports(),
                health.customersWithArtifactCatalog(),
                health.recentDeploymentJobIds(),
                health.recentDeploymentPackageIds(),
                health.recentDeploymentStatuses(),
                health.recentDeploymentProviders(),
                health.recentDeploymentClientTypes(),
                health.recentDeploymentHosts(),
                health.recentDeploymentPorts(),
                health.recentDeploymentPackageTypes(),
                health.recentDeploymentTargetDirectories(),
                health.recentDeploymentRemotePackageDirectories(),
                health.recentDeploymentRemoteBaseDirectories(),
                health.recentDeploymentDryRuns(),
                health.recentDeploymentDeployedFlags(),
                health.recentDeploymentAgentCounts(),
                health.recentDeploymentBundledFileCounts(),
                health.recentDeploymentCommandCounts(),
                health.recentDeploymentExecutedAts(),
                health.recentDeploymentGeneratedAts(),
                health.recentDeploymentCreatedAts(),
                health.recentDeploymentAts(),
                health.recentDeploymentMessages(),
                health.recentDeploymentErrorMessages(),
                health.recentDeploymentAgentIds(),
                health.recentDeploymentBundledFiles(),
                health.recentDeploymentCommands(),
                health.recentDeployments(),
                health.latestDeploymentSummary(),
                health.deploymentSnapshot(),
                health.recentDeploymentHistory(),
                health.deploymentStatusCounts(),
                health.latestDeploymentDetail(),
                health.deploymentOverview(),
                health.healthy(),
                health.statusMessage(),
                health.latestDeployment() == null ? null : health.latestDeployment().deploymentJobId(),
                health.latestDeploymentPackageId(),
                health.latestDeploymentDryRun(),
                health.latestDeploymentDeployed(),
                health.latestDeploymentAgentCount(),
                health.latestDeploymentAgentIds(),
                health.latestDeploymentBundledFiles(),
                health.latestDeploymentBundledFileCount(),
                health.latestDeploymentCommands(),
                health.latestDeploymentCommandCount(),
                health.latestDeploymentExecutedAt(),
                health.latestDeploymentGeneratedAt(),
                health.latestDeploymentCreatedAt(),
                health.latestDeployment() == null ? null : health.latestDeployment().status(),
                health.latestDeploymentAt(),
                health.latestDeploymentMessage(),
                health.latestDeploymentErrorMessage(),
                health.latestDeploymentHost(),
                health.latestDeploymentPort(),
                health.latestDeploymentProvider(),
                health.latestDeploymentClientType(),
                health.latestDeploymentPackageType(),
                health.latestDeploymentTargetDirectory(),
                health.latestDeploymentRemotePackageDirectory(),
                health.latestDeploymentRemoteBaseDirectory(),
                platformArtifactCatalogService.catalog(),
                platformDeliveryPackageService.detail()
        );
    }

    public PlatformReportExportResponse export() {
        PlatformReportResponse report = report();
        Instant generatedAt = report.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve("export");
            Files.createDirectories(exportDirectory);

            Path reportJsonPath = exportDirectory.resolve("platform-report.json");
            Path reportHtmlPath = exportDirectory.resolve("platform-report.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(reportJsonPath, json(report));
            Files.writeString(reportHtmlPath, buildHtml(report));
            Files.writeString(readmePath, buildReadme(report));

            return new PlatformReportExportResponse(
                    exportDirectory.toAbsolutePath().toString(),
                    reportJsonPath.toAbsolutePath().toString(),
                    reportHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export platform report", e);
        }
    }

    public PlatformReportBundleResponse generateBundle() {
        PlatformReportResponse report = report();
        Instant generatedAt = report.generatedAt();
        try {
            Path bundleDirectory = exportRoot.resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "platform-report.json", json(report)));
            files.add(write(bundleDirectory, "platform-report.html", buildHtml(report)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(report)));

            return new PlatformReportBundleResponse(
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-report.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-report.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate platform report bundle", e);
        }
    }

    private String buildHtml(PlatformReportResponse report) {
        String latestDeploymentSummary = report.latestDeploymentSummary() == null
                ? "N/A"
                : "%s(%s @ %s)".formatted(
                report.latestDeploymentSummary().deploymentJobId() == null ? "N/A" : report.latestDeploymentSummary().deploymentJobId(),
                report.latestDeploymentSummary().status() == null ? "N/A" : report.latestDeploymentSummary().status(),
                report.latestDeploymentSummary().deploymentAt() == null ? "N/A" : report.latestDeploymentSummary().deploymentAt());
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Platform Report</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe6; color: #1f2933; }
                    .page { max-width: 1180px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #dccfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 28px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff8f0; border: 1px solid #ead9c8; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #786a5d; }
                    .value { font-size: 26px; font-weight: 700; color: #8b4c1f; margin-top: 8px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.8; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Platform Report</h1>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Healthy Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Deployments</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Health</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Reports Ready</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Rollup</h2>
                      <ul>
                        <li>Failed deployments: %s</li>
                        <li>Recent deployments tracked: %s</li>
                        <li>Recent deployments: %s</li>
                        <li>Recent deployment providers: %s</li>
                        <li>Latest deployment summary: %s</li>
                        <li>Latest deployment detail: %s</li>
                        <li>Latest deployment provider: %s</li>
                        <li>Deployment overview: %s</li>
                        <li>Deployment snapshot: %s</li>
                        <li>Recent deployment history: %s</li>
                        <li>Deployment status counts: %s</li>
                        <li>Customer command center entries: %s</li>
                        <li>Customers with artifact catalog: %s</li>
                        <li>Health status: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(report.generatedAt().toString()),
                escapeHtml(report.statusMessage()),
                escapeHtml(String.valueOf(report.controlCenter().customerCommandCenter().totalCustomers())),
                escapeHtml(String.valueOf(report.healthyCustomers())),
                escapeHtml(String.valueOf(report.controlCenter().deploymentAuditSummary().totalDeployments())),
                escapeHtml(report.health().healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml("READY"),
                escapeHtml(String.valueOf(report.customersWithReports())),
                escapeHtml(String.valueOf(report.controlCenter().deploymentAuditSummary().failedDeployments())),
                escapeHtml(String.valueOf(report.workspace().recentDeployments().size())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentDeployments(report.recentDeployments())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentProviders())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestSummary(report.latestDeploymentSummary())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestDetail(report.latestDeploymentDetail())),
                escapeHtml(report.latestDeploymentProvider() == null ? "N/A" : report.latestDeploymentProvider()),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatOverview(report.deploymentOverview())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatSnapshot(report.deploymentSnapshot())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentHistory(report.recentDeploymentHistory())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatStatusCounts(report.deploymentStatusCounts())),
                escapeHtml(String.valueOf(report.controlCenter().customerCommandCenter().customers().size())),
                escapeHtml(String.valueOf(report.customersWithArtifactCatalog())),
                escapeHtml(report.health().statusMessage())
        );
    }

    private String buildReadme(PlatformReportResponse report) {
        return """
                Platform report
                ===============

                Generated at: %s

                Included:
                - platform health
                - platform control center
                - platform workspace
                - platform artifact catalog
                - platform delivery package
                - healthy customers: %s
                - report-ready customers: %s
                - artifact-catalog-ready customers: %s
                - healthy: %s
                - status: %s
                - recent deployment job ids: %s
                - recent deployment package ids: %s
                - recent deployment statuses: %s
                - recent deployment providers: %s
                - recent deployment client types: %s
                - recent deployment hosts: %s
                - recent deployment ports: %s
                - recent deployment package types: %s
                - recent deployment target directories: %s
                - recent deployment remote package directories: %s
                - recent deployment remote base directories: %s
                - recent deployment dry runs: %s
                - recent deployment deployed flags: %s
                - recent deployment agent counts: %s
                - recent deployment bundled file counts: %s
                - recent deployment command counts: %s
                - recent deployment executed ats: %s
                - recent deployment generated ats: %s
                - recent deployment created ats: %s
                - recent deployment ats: %s
                - recent deployment messages: %s
                - recent deployment error messages: %s
                - recent deployment agent ids: %s
                - recent deployment bundled files: %s
                - recent deployment commands: %s
                - recent deployments: %s
                - latest deployment summary: %s
                - latest deployment detail: %s
                - deployment overview: %s
                - deployment snapshot: %s
                - recent deployment history: %s
                - deployment status counts: %s
                - latest deployment: %s
                - latest deployment package id: %s
                - latest deployment dry run: %s
                - latest deployment deployed: %s
                - latest deployment agent count: %s
                - latest deployment agent ids: %s
                - latest deployment bundled files: %s
                - latest deployment bundled file count: %s
                - latest deployment commands: %s
                - latest deployment command count: %s
                - latest deployment executed at: %s
                - latest deployment generated at: %s
                - latest deployment created at: %s
                - latest deployment status: %s
                - latest deployment at: %s
                - latest deployment message: %s
                - latest deployment error message: %s
                - latest deployment host: %s
                - latest deployment port: %s
                - latest deployment provider: %s
                - latest deployment client type: %s
                - latest deployment package type: %s
                - latest deployment target directory: %s
                - latest deployment remote package directory: %s
                - latest deployment remote base directory: %s

                Files:
                - platform-report.json
                - platform-report.html
                - README.txt
                """.formatted(
                report.generatedAt(),
                report.healthyCustomers(),
                report.customersWithReports(),
                report.customersWithArtifactCatalog(),
                report.healthy(),
                report.statusMessage(),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentJobIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentPackageIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentStatuses()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentProviders()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentClientTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentHosts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentPorts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentPackageTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentTargetDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentRemotePackageDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentRemoteBaseDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentDryRuns()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentDeployedFlags()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentAgentCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentBundledFileCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentCommandCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentExecutedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentGeneratedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentCreatedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentMessages()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.recentDeploymentErrorMessages()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(report.recentDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(report.recentDeploymentBundledFiles()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(report.recentDeploymentCommands()),
                PlatformDeploymentSummaryFormatter.formatRecentDeployments(report.recentDeployments()),
                PlatformDeploymentSummaryFormatter.formatLatestSummary(report.latestDeploymentSummary()),
                PlatformDeploymentSummaryFormatter.formatLatestDetail(report.latestDeploymentDetail()),
                PlatformDeploymentSummaryFormatter.formatOverview(report.deploymentOverview()),
                PlatformDeploymentSummaryFormatter.formatSnapshot(report.deploymentSnapshot()),
                PlatformDeploymentSummaryFormatter.formatRecentHistory(report.recentDeploymentHistory()),
                PlatformDeploymentSummaryFormatter.formatStatusCounts(report.deploymentStatusCounts()),
                report.latestDeploymentJobId() == null ? "None" : report.latestDeploymentJobId(),
                report.latestDeploymentPackageId() == null ? "N/A" : report.latestDeploymentPackageId(),
                report.latestDeploymentDryRun() == null ? "N/A" : report.latestDeploymentDryRun(),
                report.latestDeploymentDeployed() == null ? "N/A" : report.latestDeploymentDeployed(),
                report.latestDeploymentAgentCount() == null ? "N/A" : report.latestDeploymentAgentCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.latestDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.latestDeploymentBundledFiles()),
                report.latestDeploymentBundledFileCount() == null ? "N/A" : report.latestDeploymentBundledFileCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(report.latestDeploymentCommands()),
                report.latestDeploymentCommandCount() == null ? "N/A" : report.latestDeploymentCommandCount(),
                report.latestDeploymentExecutedAt() == null ? "N/A" : report.latestDeploymentExecutedAt(),
                report.latestDeploymentGeneratedAt() == null ? "N/A" : report.latestDeploymentGeneratedAt(),
                report.latestDeploymentCreatedAt() == null ? "N/A" : report.latestDeploymentCreatedAt(),
                report.latestDeploymentStatus() == null ? "N/A" : report.latestDeploymentStatus(),
                report.latestDeploymentAt() == null ? "N/A" : report.latestDeploymentAt(),
                report.latestDeploymentMessage() == null ? "N/A" : report.latestDeploymentMessage(),
                report.latestDeploymentErrorMessage() == null ? "N/A" : report.latestDeploymentErrorMessage(),
                report.latestDeploymentHost() == null ? "N/A" : report.latestDeploymentHost(),
                report.latestDeploymentPort() == null ? "N/A" : report.latestDeploymentPort(),
                report.latestDeploymentProvider() == null ? "N/A" : report.latestDeploymentProvider(),
                report.latestDeploymentClientType() == null ? "N/A" : report.latestDeploymentClientType(),
                report.latestDeploymentPackageType() == null ? "N/A" : report.latestDeploymentPackageType(),
                report.latestDeploymentTargetDirectory() == null ? "N/A" : report.latestDeploymentTargetDirectory(),
                report.latestDeploymentRemotePackageDirectory() == null ? "N/A" : report.latestDeploymentRemotePackageDirectory(),
                report.latestDeploymentRemoteBaseDirectory() == null ? "N/A" : report.latestDeploymentRemoteBaseDirectory()
        );
    }

    private String write(Path directory, String fileName, String content) throws IOException {
        Path output = directory.resolve(fileName);
        Files.writeString(output, content);
        return output.getFileName().toString();
    }

    private String json(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize platform report", e);
        }
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
