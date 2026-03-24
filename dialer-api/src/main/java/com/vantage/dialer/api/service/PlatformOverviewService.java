package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.PlatformOverviewBundleResponse;
import com.vantage.dialer.api.dto.PlatformOverviewExportResponse;
import com.vantage.dialer.api.dto.PlatformOverviewResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlatformOverviewService {

    private final PlatformHealthService platformHealthService;
    private final PlatformControlCenterService platformControlCenterService;
    private final PlatformWorkspaceService platformWorkspaceService;
    private final PlatformReportService platformReportService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public PlatformOverviewService(PlatformHealthService platformHealthService,
                                   PlatformControlCenterService platformControlCenterService,
                                   PlatformWorkspaceService platformWorkspaceService,
                                   PlatformReportService platformReportService,
                                   ObjectMapper objectMapper,
                                   @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.platformHealthService = platformHealthService;
        this.platformControlCenterService = platformControlCenterService;
        this.platformWorkspaceService = platformWorkspaceService;
        this.platformReportService = platformReportService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("platform-overview");
    }

    public PlatformOverviewResponse overview() {
        var health = platformHealthService.health();
        var controlCenter = platformControlCenterService.controlCenter();
        var workspace = platformWorkspaceService.workspace();
        return new PlatformOverviewResponse(
                Instant.now(),
                health,
                controlCenter,
                workspace,
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
                platformReportService.report()
        );
    }

    public PlatformOverviewExportResponse export() {
        PlatformOverviewResponse overview = overview();
        Instant generatedAt = overview.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve("export");
            Files.createDirectories(exportDirectory);

            Path overviewJsonPath = exportDirectory.resolve("platform-overview.json");
            Path overviewHtmlPath = exportDirectory.resolve("platform-overview.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(overviewJsonPath, json(overview));
            Files.writeString(overviewHtmlPath, buildHtml(overview));
            Files.writeString(readmePath, buildReadme(overview));

            return new PlatformOverviewExportResponse(
                    exportDirectory.toAbsolutePath().toString(),
                    overviewJsonPath.toAbsolutePath().toString(),
                    overviewHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export platform overview", e);
        }
    }

    public PlatformOverviewBundleResponse generateBundle() {
        PlatformOverviewResponse overview = overview();
        Instant generatedAt = overview.generatedAt();
        try {
            Path bundleDirectory = exportRoot.resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "platform-overview.json", json(overview)));
            files.add(write(bundleDirectory, "platform-overview.html", buildHtml(overview)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(overview)));

            return new PlatformOverviewBundleResponse(
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-overview.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-overview.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate platform overview bundle", e);
        }
    }

    private String buildHtml(PlatformOverviewResponse overview) {
        String latestDeploymentSummary = overview.latestDeploymentSummary() == null
                ? "N/A"
                : "%s(%s @ %s)".formatted(
                overview.latestDeploymentSummary().deploymentJobId() == null ? "N/A" : overview.latestDeploymentSummary().deploymentJobId(),
                overview.latestDeploymentSummary().status() == null ? "N/A" : overview.latestDeploymentSummary().status(),
                overview.latestDeploymentSummary().deploymentAt() == null ? "N/A" : overview.latestDeploymentSummary().deploymentAt());
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Platform Overview</title>
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
                      <h1>Platform Overview</h1>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Healthy</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Deployments</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Recent Deployments</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Reports Ready</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Overview</h2>
                      <ul>
                        <li>Customers with delivery package: %s</li>
                        <li>Customers with quotes: %s</li>
                        <li>Customers with artifact catalog: %s</li>
                        <li>Recent deployments: %s</li>
                        <li>Recent deployment providers: %s</li>
                        <li>Latest deployment summary: %s</li>
                        <li>Latest deployment detail: %s</li>
                        <li>Latest deployment provider: %s</li>
                        <li>Deployment overview: %s</li>
                        <li>Deployment snapshot: %s</li>
                        <li>Recent deployment history: %s</li>
                        <li>Deployment status counts: %s</li>
                        <li>Report entries: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(overview.generatedAt().toString()),
                escapeHtml(overview.health().healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(String.valueOf(overview.controlCenter().customerCommandCenter().totalCustomers())),
                escapeHtml(String.valueOf(overview.healthyCustomers())),
                escapeHtml(String.valueOf(overview.controlCenter().deploymentAuditSummary().totalDeployments())),
                escapeHtml(String.valueOf(overview.health().failedDeployments())),
                escapeHtml(String.valueOf(overview.workspace().recentDeployments().size())),
                escapeHtml(String.valueOf(overview.customersWithReports())),
                escapeHtml(String.valueOf(overview.health().customersWithDeliveryPackage())),
                escapeHtml(String.valueOf(overview.health().customersWithQuotes())),
                escapeHtml(String.valueOf(overview.customersWithArtifactCatalog())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentDeployments(overview.recentDeployments())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentProviders())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestSummary(overview.latestDeploymentSummary())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestDetail(overview.latestDeploymentDetail())),
                escapeHtml(overview.latestDeploymentProvider() == null ? "N/A" : overview.latestDeploymentProvider()),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatOverview(overview.deploymentOverview())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatSnapshot(overview.deploymentSnapshot())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentHistory(overview.recentDeploymentHistory())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatStatusCounts(overview.deploymentStatusCounts())),
                escapeHtml(String.valueOf(overview.report().controlCenter().customerCommandCenter().customers().size()))
        );
    }

    private String buildReadme(PlatformOverviewResponse overview) {
        return """
                Platform overview
                =================

                Generated at: %s
                Healthy customers: %s
                Customers with reports: %s
                Customers with artifact catalog: %s
                Healthy: %s
                Status: %s
                Recent deployment job ids: %s
                Recent deployment package ids: %s
                Recent deployment statuses: %s
                Recent deployment providers: %s
                Recent deployment client types: %s
                Recent deployment hosts: %s
                Recent deployment ports: %s
                Recent deployment package types: %s
                Recent deployment target directories: %s
                Recent deployment remote package directories: %s
                Recent deployment remote base directories: %s
                Recent deployment dry runs: %s
                Recent deployment deployed flags: %s
                Recent deployment agent counts: %s
                Recent deployment bundled file counts: %s
                Recent deployment command counts: %s
                Recent deployment executed ats: %s
                Recent deployment generated ats: %s
                Recent deployment created ats: %s
                Recent deployment ats: %s
                Recent deployment messages: %s
                Recent deployment error messages: %s
                Recent deployment agent ids: %s
                Recent deployment bundled files: %s
                Recent deployment commands: %s
                Recent deployments: %s
                Latest deployment summary: %s
                Latest deployment detail: %s
                Deployment overview: %s
                Deployment snapshot: %s
                Recent deployment history: %s
                Deployment status counts: %s
                Latest deployment: %s
                Latest deployment package id: %s
                Latest deployment dry run: %s
                Latest deployment deployed: %s
                Latest deployment agent count: %s
                Latest deployment agent ids: %s
                Latest deployment bundled files: %s
                Latest deployment bundled file count: %s
                Latest deployment commands: %s
                Latest deployment command count: %s
                Latest deployment executed at: %s
                Latest deployment generated at: %s
                Latest deployment created at: %s
                Latest deployment status: %s
                Latest deployment at: %s
                Latest deployment message: %s
                Latest deployment error message: %s
                Latest deployment host: %s
                Latest deployment port: %s
                Latest deployment provider: %s
                Latest deployment client type: %s
                Latest deployment package type: %s
                Latest deployment target directory: %s
                Latest deployment remote package directory: %s
                Latest deployment remote base directory: %s

                Files:
                - platform-overview.json
                - platform-overview.html
                - README.txt
                """.formatted(
                overview.generatedAt(),
                overview.healthyCustomers(),
                overview.customersWithReports(),
                overview.customersWithArtifactCatalog(),
                overview.healthy(),
                overview.statusMessage(),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentJobIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentPackageIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentStatuses()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentProviders()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentClientTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentHosts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentPorts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentPackageTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentTargetDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentRemotePackageDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentRemoteBaseDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentDryRuns()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentDeployedFlags()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentAgentCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentBundledFileCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentCommandCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentExecutedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentGeneratedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentCreatedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentMessages()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.recentDeploymentErrorMessages()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(overview.recentDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(overview.recentDeploymentBundledFiles()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(overview.recentDeploymentCommands()),
                PlatformDeploymentSummaryFormatter.formatRecentDeployments(overview.recentDeployments()),
                PlatformDeploymentSummaryFormatter.formatLatestSummary(overview.latestDeploymentSummary()),
                PlatformDeploymentSummaryFormatter.formatLatestDetail(overview.latestDeploymentDetail()),
                PlatformDeploymentSummaryFormatter.formatOverview(overview.deploymentOverview()),
                PlatformDeploymentSummaryFormatter.formatSnapshot(overview.deploymentSnapshot()),
                PlatformDeploymentSummaryFormatter.formatRecentHistory(overview.recentDeploymentHistory()),
                PlatformDeploymentSummaryFormatter.formatStatusCounts(overview.deploymentStatusCounts()),
                overview.latestDeploymentJobId() == null ? "None" : overview.latestDeploymentJobId(),
                overview.latestDeploymentPackageId() == null ? "N/A" : overview.latestDeploymentPackageId(),
                overview.latestDeploymentDryRun() == null ? "N/A" : overview.latestDeploymentDryRun(),
                overview.latestDeploymentDeployed() == null ? "N/A" : overview.latestDeploymentDeployed(),
                overview.latestDeploymentAgentCount() == null ? "N/A" : overview.latestDeploymentAgentCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.latestDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.latestDeploymentBundledFiles()),
                overview.latestDeploymentBundledFileCount() == null ? "N/A" : overview.latestDeploymentBundledFileCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(overview.latestDeploymentCommands()),
                overview.latestDeploymentCommandCount() == null ? "N/A" : overview.latestDeploymentCommandCount(),
                overview.latestDeploymentExecutedAt() == null ? "N/A" : overview.latestDeploymentExecutedAt(),
                overview.latestDeploymentGeneratedAt() == null ? "N/A" : overview.latestDeploymentGeneratedAt(),
                overview.latestDeploymentCreatedAt() == null ? "N/A" : overview.latestDeploymentCreatedAt(),
                overview.latestDeploymentStatus() == null ? "N/A" : overview.latestDeploymentStatus(),
                overview.latestDeploymentAt() == null ? "N/A" : overview.latestDeploymentAt(),
                overview.latestDeploymentMessage() == null ? "N/A" : overview.latestDeploymentMessage(),
                overview.latestDeploymentErrorMessage() == null ? "N/A" : overview.latestDeploymentErrorMessage(),
                overview.latestDeploymentHost() == null ? "N/A" : overview.latestDeploymentHost(),
                overview.latestDeploymentPort() == null ? "N/A" : overview.latestDeploymentPort(),
                overview.latestDeploymentProvider() == null ? "N/A" : overview.latestDeploymentProvider(),
                overview.latestDeploymentClientType() == null ? "N/A" : overview.latestDeploymentClientType(),
                overview.latestDeploymentPackageType() == null ? "N/A" : overview.latestDeploymentPackageType(),
                overview.latestDeploymentTargetDirectory() == null ? "N/A" : overview.latestDeploymentTargetDirectory(),
                overview.latestDeploymentRemotePackageDirectory() == null ? "N/A" : overview.latestDeploymentRemotePackageDirectory(),
                overview.latestDeploymentRemoteBaseDirectory() == null ? "N/A" : overview.latestDeploymentRemoteBaseDirectory()
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
            throw new IllegalStateException("Failed to serialize platform overview", e);
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
