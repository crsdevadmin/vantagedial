package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageExportResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformDeliveryPackageService {

    private final PlatformControlCenterService platformControlCenterService;
    private final PlatformWorkspaceService platformWorkspaceService;
    private final PlatformHealthService platformHealthService;
    private final PlatformArtifactCatalogService platformArtifactCatalogService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public PlatformDeliveryPackageService(PlatformControlCenterService platformControlCenterService,
                                          PlatformWorkspaceService platformWorkspaceService,
                                          PlatformHealthService platformHealthService,
                                          @Lazy
                                          PlatformArtifactCatalogService platformArtifactCatalogService,
                                          ObjectMapper objectMapper,
                                          @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.platformControlCenterService = platformControlCenterService;
        this.platformWorkspaceService = platformWorkspaceService;
        this.platformHealthService = platformHealthService;
        this.platformArtifactCatalogService = platformArtifactCatalogService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("platform-delivery-package");
    }

    public PlatformDeliveryPackageResponse generate() {
        PlatformDeliveryPackageDetailResponse detail = detail();
        Instant generatedAt = detail.generatedAt();
        try {
            Path packageDirectory = exportRoot.resolve("bundle");
            Files.createDirectories(packageDirectory);

            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("generatedAt", generatedAt);
            manifest.put("health", detail.health());
            manifest.put("controlCenter", detail.controlCenter());
            manifest.put("workspace", detail.workspace());
            manifest.put("healthyCustomers", detail.healthyCustomers());
            manifest.put("customersWithReports", detail.customersWithReports());
            manifest.put("customersWithArtifactCatalog", detail.customersWithArtifactCatalog());
            manifest.put("recentDeploymentProviders", detail.recentDeploymentProviders());
            manifest.put("recentDeployments", detail.recentDeployments());
            manifest.put("latestDeploymentSummary", detail.latestDeploymentSummary());
            manifest.put("deploymentSnapshot", detail.deploymentSnapshot());
            manifest.put("recentDeploymentHistory", detail.recentDeploymentHistory());
            manifest.put("deploymentStatusCounts", detail.deploymentStatusCounts());
            manifest.put("latestDeploymentDetail", detail.latestDeploymentDetail());
            manifest.put("deploymentOverview", detail.deploymentOverview());
            manifest.put("latestDeploymentJobId", detail.latestDeploymentJobId());
            manifest.put("latestDeploymentProvider", detail.latestDeploymentProvider());
            manifest.put("latestDeploymentStatus", detail.latestDeploymentStatus());
            manifest.put("latestDeploymentAt", detail.latestDeploymentAt());
            manifest.put("artifactCatalog", detail.artifactCatalog());

            List<String> files = new ArrayList<>();
            files.add(write(packageDirectory, "platform-delivery-package.json", json(manifest)));
            files.add(write(packageDirectory, "README.txt", buildReadme(detail)));

            return new PlatformDeliveryPackageResponse(
                    packageDirectory.toAbsolutePath().toString(),
                    packageDirectory.resolve("platform-delivery-package.json").toAbsolutePath().toString(),
                    packageDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate platform delivery package", e);
        }
    }

    public PlatformDeliveryPackageDetailResponse detail() {
        var health = platformHealthService.health();
        return new PlatformDeliveryPackageDetailResponse(
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
                platformArtifactCatalogService.catalog()
        );
    }

    public PlatformDeliveryPackageExportResponse export() {
        PlatformDeliveryPackageDetailResponse detail = detail();
        Instant generatedAt = detail.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve("export");
            Files.createDirectories(exportDirectory);

            Path packageJsonPath = exportDirectory.resolve("platform-delivery-package.json");
            Path packageHtmlPath = exportDirectory.resolve("platform-delivery-package.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(packageJsonPath, json(detail));
            Files.writeString(packageHtmlPath, buildHtml(detail));
            Files.writeString(readmePath, buildExportReadme(detail));

            return new PlatformDeliveryPackageExportResponse(
                    exportDirectory.toAbsolutePath().toString(),
                    packageJsonPath.toAbsolutePath().toString(),
                    packageHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export platform delivery package", e);
        }
    }

    private String buildHtml(PlatformDeliveryPackageDetailResponse detail) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Platform Delivery Package</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f5efe7; color: #1f2933; }
                    .page { max-width: 1180px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #dccfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 28px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff8f0; border: 1px solid #ead9c8; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #786a5d; }
                    .value { font-size: 26px; font-weight: 700; color: #8b4c1f; margin-top: 8px; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Platform Delivery Package</h1>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Healthy Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Deployments</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Health</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Catalog Ready</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Reports Ready</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Deployment Summary</h2>
                      <ul>
                        <li>Recent deployments: %s</li>
                        <li>Recent deployment providers: %s</li>
                        <li>Latest deployment summary: %s</li>
                        <li>Latest deployment detail: %s</li>
                        <li>Latest deployment provider: %s</li>
                        <li>Deployment overview: %s</li>
                        <li>Deployment snapshot: %s</li>
                        <li>Recent deployment history: %s</li>
                        <li>Deployment status counts: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(detail.generatedAt().toString()),
                escapeHtml(detail.statusMessage()),
                escapeHtml(String.valueOf(detail.controlCenter().customerCommandCenter().totalCustomers())),
                escapeHtml(String.valueOf(detail.healthyCustomers())),
                escapeHtml(String.valueOf(detail.controlCenter().deploymentAuditSummary().totalDeployments())),
                escapeHtml(detail.health().healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(String.valueOf(detail.customersWithArtifactCatalog())),
                escapeHtml(String.valueOf(detail.customersWithReports())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentDeployments(detail.recentDeployments())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentProviders())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestSummary(detail.latestDeploymentSummary())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestDetail(detail.latestDeploymentDetail())),
                escapeHtml(detail.latestDeploymentProvider() == null ? "N/A" : detail.latestDeploymentProvider()),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatOverview(detail.deploymentOverview())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatSnapshot(detail.deploymentSnapshot())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentHistory(detail.recentDeploymentHistory())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatStatusCounts(detail.deploymentStatusCounts()))
        );
    }

    private String buildReadme(PlatformDeliveryPackageDetailResponse detail) {
        return """
                Platform delivery package
                ========================

                Generated at: %s

                Included:
                - platform health
                - platform control center
                - platform workspace
                - platform artifact catalog
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
                - platform-delivery-package.json
                - README.txt
                """.formatted(
                detail.generatedAt(),
                detail.healthyCustomers(),
                detail.customersWithReports(),
                detail.customersWithArtifactCatalog(),
                detail.healthy(),
                detail.statusMessage(),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentJobIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentPackageIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentStatuses()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentProviders()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentClientTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentHosts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentPorts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentPackageTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentTargetDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentRemotePackageDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentRemoteBaseDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentDryRuns()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentDeployedFlags()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentAgentCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentBundledFileCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentCommandCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentExecutedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentGeneratedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentCreatedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentMessages()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.recentDeploymentErrorMessages()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(detail.recentDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(detail.recentDeploymentBundledFiles()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(detail.recentDeploymentCommands()),
                PlatformDeploymentSummaryFormatter.formatRecentDeployments(detail.recentDeployments()),
                PlatformDeploymentSummaryFormatter.formatLatestDetail(detail.latestDeploymentDetail()),
                PlatformDeploymentSummaryFormatter.formatOverview(detail.deploymentOverview()),
                PlatformDeploymentSummaryFormatter.formatSnapshot(detail.deploymentSnapshot()),
                PlatformDeploymentSummaryFormatter.formatRecentHistory(detail.recentDeploymentHistory()),
                PlatformDeploymentSummaryFormatter.formatStatusCounts(detail.deploymentStatusCounts()),
                detail.latestDeploymentJobId() == null ? "None" : detail.latestDeploymentJobId(),
                detail.latestDeploymentPackageId() == null ? "N/A" : detail.latestDeploymentPackageId(),
                detail.latestDeploymentDryRun() == null ? "N/A" : detail.latestDeploymentDryRun(),
                detail.latestDeploymentDeployed() == null ? "N/A" : detail.latestDeploymentDeployed(),
                detail.latestDeploymentAgentCount() == null ? "N/A" : detail.latestDeploymentAgentCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.latestDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.latestDeploymentBundledFiles()),
                detail.latestDeploymentBundledFileCount() == null ? "N/A" : detail.latestDeploymentBundledFileCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(detail.latestDeploymentCommands()),
                detail.latestDeploymentCommandCount() == null ? "N/A" : detail.latestDeploymentCommandCount(),
                detail.latestDeploymentExecutedAt() == null ? "N/A" : detail.latestDeploymentExecutedAt(),
                detail.latestDeploymentGeneratedAt() == null ? "N/A" : detail.latestDeploymentGeneratedAt(),
                detail.latestDeploymentCreatedAt() == null ? "N/A" : detail.latestDeploymentCreatedAt(),
                detail.latestDeploymentStatus() == null ? "N/A" : detail.latestDeploymentStatus(),
                detail.latestDeploymentAt() == null ? "N/A" : detail.latestDeploymentAt(),
                detail.latestDeploymentMessage() == null ? "N/A" : detail.latestDeploymentMessage(),
                detail.latestDeploymentErrorMessage() == null ? "N/A" : detail.latestDeploymentErrorMessage(),
                detail.latestDeploymentHost() == null ? "N/A" : detail.latestDeploymentHost(),
                detail.latestDeploymentPort() == null ? "N/A" : detail.latestDeploymentPort(),
                detail.latestDeploymentProvider() == null ? "N/A" : detail.latestDeploymentProvider(),
                detail.latestDeploymentClientType() == null ? "N/A" : detail.latestDeploymentClientType(),
                detail.latestDeploymentPackageType() == null ? "N/A" : detail.latestDeploymentPackageType(),
                detail.latestDeploymentTargetDirectory() == null ? "N/A" : detail.latestDeploymentTargetDirectory(),
                detail.latestDeploymentRemotePackageDirectory() == null ? "N/A" : detail.latestDeploymentRemotePackageDirectory(),
                detail.latestDeploymentRemoteBaseDirectory() == null ? "N/A" : detail.latestDeploymentRemoteBaseDirectory()
        );
    }

    private String buildExportReadme(PlatformDeliveryPackageDetailResponse detail) {
        return """
                Platform delivery package export
                ===============================

                Generated at: %s

                Files:
                - platform-delivery-package.json
                - platform-delivery-package.html
                - README.txt
                """.formatted(detail.generatedAt());
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
            throw new IllegalStateException("Failed to serialize platform delivery package", e);
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
