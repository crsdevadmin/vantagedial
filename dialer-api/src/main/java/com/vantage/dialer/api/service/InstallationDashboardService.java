package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.InstallationDashboardBundleResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.InstallationDashboardExportResponse;
import com.vantage.dialer.api.dto.InstallationDashboardResponse;
import com.vantage.dialer.api.dto.InstallationHealthBundleResponse;
import com.vantage.dialer.api.dto.InstallationHealthExportResponse;
import com.vantage.dialer.api.dto.InstallationHealthResponse;
import com.vantage.dialer.api.dto.InstallationOverviewBundleResponse;
import com.vantage.dialer.api.dto.InstallationOverviewExportResponse;
import com.vantage.dialer.api.dto.InstallationOverviewResponse;
import com.vantage.dialer.api.dto.InstallationReportBundleResponse;
import com.vantage.dialer.api.dto.InstallationReportExportResponse;
import com.vantage.dialer.api.dto.InstallationReportResponse;
import com.vantage.dialer.api.dto.InstallationTimelineBundleResponse;
import com.vantage.dialer.api.dto.InstallationTimelineEntryResponse;
import com.vantage.dialer.api.dto.InstallationTimelineExportResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceExportResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceResponse;
import org.springframework.beans.factory.annotation.Value;
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
public class InstallationDashboardService {

    private final CustomerInstallationService installationService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public InstallationDashboardService(CustomerInstallationService installationService,
                                        ObjectMapper objectMapper,
                                        @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.installationService = installationService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("installations");
    }

    public InstallationDashboardResponse dashboard(String customerId) {
        List<CustomerInstallationResponse> installations = installationService.list(customerId);
        return new InstallationDashboardResponse(
                customerId,
                Instant.now(),
                installations.size(),
                countByStatus(installations, "COMPLETED"),
                countByStatus(installations, "FAILED"),
                countByStatus(installations, "DRY_RUN"),
                countByStatus(installations, "PENDING"),
                installations.stream().mapToInt(CustomerInstallationResponse::agentCount).sum(),
                installations.isEmpty() ? null : installations.get(0),
                installations
        );
    }

    public InstallationDashboardBundleResponse generateBundle(String customerId) {
        InstallationDashboardResponse dashboard = dashboard(customerId);
        Instant generatedAt = dashboard.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path bundleDirectory = exportRoot.resolve(scope).resolve("dashboard");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "dashboard.json", json(dashboard)));
            files.add(write(bundleDirectory, "dashboard.md", buildMarkdown(dashboard)));
            files.add(write(bundleDirectory, "dashboard.html", buildHtml(dashboard)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(dashboard)));

            return new InstallationDashboardBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("dashboard.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("dashboard.md").toAbsolutePath().toString(),
                    bundleDirectory.resolve("dashboard.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate installation dashboard bundle", e);
        }
    }

    public InstallationDashboardExportResponse exportDashboard(String customerId) {
        InstallationDashboardResponse dashboard = dashboard(customerId);
        Instant generatedAt = dashboard.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path exportDirectory = exportRoot.resolve(scope).resolve("dashboard-export");
            Files.createDirectories(exportDirectory);

            Path dashboardJsonPath = exportDirectory.resolve("dashboard.json");
            Path dashboardCsvPath = exportDirectory.resolve("dashboard.csv");
            Path dashboardHtmlPath = exportDirectory.resolve("dashboard.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(dashboardJsonPath, json(dashboard));
            Files.writeString(dashboardCsvPath, buildDashboardCsv(dashboard));
            Files.writeString(dashboardHtmlPath, buildHtml(dashboard));
            Files.writeString(readmePath, buildDashboardExportReadme(dashboard));

            return new InstallationDashboardExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    dashboardJsonPath.toAbsolutePath().toString(),
                    dashboardCsvPath.toAbsolutePath().toString(),
                    dashboardHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export installation dashboard", e);
        }
    }

    public List<InstallationTimelineEntryResponse> timeline(String customerId) {
        return installationService.list(customerId).stream()
                .map(InstallationTimelineEntryResponse::from)
                .toList();
    }

    public InstallationTimelineBundleResponse generateTimelineBundle(String customerId) {
        List<InstallationTimelineEntryResponse> timeline = timeline(customerId);
        Instant generatedAt = Instant.now();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path bundleDirectory = exportRoot.resolve(scope).resolve("timeline");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "timeline.json", json(timeline)));
            files.add(write(bundleDirectory, "timeline.csv", buildTimelineCsv(timeline)));
            files.add(write(bundleDirectory, "timeline.md", buildTimelineMarkdown(customerId, timeline, generatedAt)));
            files.add(write(bundleDirectory, "timeline.html", buildTimelineHtml(customerId, timeline, generatedAt)));
            files.add(write(bundleDirectory, "README.txt", buildTimelineReadme(customerId, generatedAt)));

            return new InstallationTimelineBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("timeline.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("timeline.csv").toAbsolutePath().toString(),
                    bundleDirectory.resolve("timeline.md").toAbsolutePath().toString(),
                    bundleDirectory.resolve("timeline.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate installation timeline bundle", e);
        }
    }

    public InstallationTimelineExportResponse exportTimeline(String customerId) {
        List<InstallationTimelineEntryResponse> timeline = timeline(customerId);
        Instant generatedAt = Instant.now();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path exportDirectory = exportRoot.resolve(scope).resolve("timeline-export");
            Files.createDirectories(exportDirectory);

            Path timelineJsonPath = exportDirectory.resolve("timeline.json");
            Path timelineCsvPath = exportDirectory.resolve("timeline.csv");
            Path timelineHtmlPath = exportDirectory.resolve("timeline.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(timelineJsonPath, json(timeline));
            Files.writeString(timelineCsvPath, buildTimelineCsv(timeline));
            Files.writeString(timelineHtmlPath, buildTimelineHtml(customerId, timeline, generatedAt));
            Files.writeString(readmePath, buildTimelineExportReadme(customerId, generatedAt));

            return new InstallationTimelineExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    timelineJsonPath.toAbsolutePath().toString(),
                    timelineCsvPath.toAbsolutePath().toString(),
                    timelineHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export installation timeline", e);
        }
    }

    public InstallationReportBundleResponse generateReportBundle(String customerId) {
        InstallationReportResponse report = report(customerId);
        InstallationDashboardResponse dashboard = report.dashboard();
        List<InstallationTimelineEntryResponse> timeline = report.timeline();
        Instant generatedAt = report.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path bundleDirectory = exportRoot.resolve(scope).resolve("report");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "dashboard.json", json(dashboard)));
            files.add(write(bundleDirectory, "timeline.json", json(timeline)));
            files.add(write(bundleDirectory, "latest-installation.json", json(report.latestInstallation())));
            files.add(write(bundleDirectory, "report.md", buildReportMarkdown(dashboard, timeline, generatedAt)));
            files.add(write(bundleDirectory, "report.html", buildReportHtml(dashboard, timeline, generatedAt)));
            files.add(write(bundleDirectory, "README.txt", buildReportReadme(dashboard, generatedAt)));

            return new InstallationReportBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("dashboard.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("timeline.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("latest-installation.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("report.md").toAbsolutePath().toString(),
                    bundleDirectory.resolve("report.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate installation report bundle", e);
        }
    }

    public InstallationReportResponse report(String customerId) {
        InstallationDashboardResponse dashboard = dashboard(customerId);
        return new InstallationReportResponse(
                customerId,
                Instant.now(),
                dashboard,
                dashboard.latestInstallation(),
                timeline(customerId)
        );
    }

    public InstallationOverviewResponse overview(String customerId) {
        return new InstallationOverviewResponse(
                customerId,
                Instant.now(),
                dashboard(customerId),
                health(customerId),
                report(customerId)
        );
    }

    public InstallationReportExportResponse exportReport(String customerId) {
        InstallationReportResponse report = report(customerId);
        Instant generatedAt = report.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path exportDirectory = exportRoot.resolve(scope).resolve("report-export");
            Files.createDirectories(exportDirectory);

            Path reportJsonPath = exportDirectory.resolve("report.json");
            Path reportHtmlPath = exportDirectory.resolve("report.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(reportJsonPath, json(report));
            Files.writeString(reportHtmlPath, buildReportHtml(report.dashboard(), report.timeline(), generatedAt));
            Files.writeString(readmePath, buildReportExportReadme(report));

            return new InstallationReportExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    reportJsonPath.toAbsolutePath().toString(),
                    reportHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export installation report", e);
        }
    }

    public InstallationOverviewBundleResponse generateOverviewBundle(String customerId) {
        InstallationOverviewResponse overview = overview(customerId);
        Instant generatedAt = overview.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path bundleDirectory = exportRoot.resolve(scope).resolve("overview");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "overview.json", json(overview)));
            files.add(write(bundleDirectory, "dashboard.json", json(overview.dashboard())));
            files.add(write(bundleDirectory, "health.json", json(overview.health())));
            files.add(write(bundleDirectory, "report.json", json(overview.report())));
            files.add(write(bundleDirectory, "overview.md", buildOverviewMarkdown(overview)));
            files.add(write(bundleDirectory, "overview.html", buildOverviewHtml(overview)));
            files.add(write(bundleDirectory, "README.txt", buildOverviewReadme(overview)));

            return new InstallationOverviewBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("overview.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("dashboard.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("health.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("report.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("overview.md").toAbsolutePath().toString(),
                    bundleDirectory.resolve("overview.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate installation overview bundle", e);
        }
    }

    public InstallationOverviewExportResponse exportOverview(String customerId) {
        InstallationOverviewResponse overview = overview(customerId);
        Instant generatedAt = overview.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path exportDirectory = exportRoot.resolve(scope).resolve("overview-export");
            Files.createDirectories(exportDirectory);

            Path overviewJsonPath = exportDirectory.resolve("overview.json");
            Path overviewHtmlPath = exportDirectory.resolve("overview.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(overviewJsonPath, json(overview));
            Files.writeString(overviewHtmlPath, buildOverviewHtml(overview));
            Files.writeString(readmePath, buildOverviewExportReadme(overview));

            return new InstallationOverviewExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    overviewJsonPath.toAbsolutePath().toString(),
                    overviewHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export installation overview", e);
        }
    }

    public InstallationWorkspaceBundleResponse generateWorkspaceBundle(String customerId) {
        InstallationDashboardBundleResponse dashboardBundle = generateBundle(customerId);
        InstallationTimelineBundleResponse timelineBundle = generateTimelineBundle(customerId);
        InstallationHealthBundleResponse healthBundle = generateHealthBundle(customerId);
        InstallationReportBundleResponse reportBundle = generateReportBundle(customerId);
        InstallationOverviewBundleResponse overviewBundle = generateOverviewBundle(customerId);
        Instant generatedAt = Instant.now();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path bundleDirectory = exportRoot.resolve(scope).resolve("workspace");
            Files.createDirectories(bundleDirectory);

            java.util.Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("customerId", customerId);
            manifest.put("generatedAt", generatedAt);
            manifest.put("dashboardBundle", dashboardBundle);
            manifest.put("timelineBundle", timelineBundle);
            manifest.put("healthBundle", healthBundle);
            manifest.put("reportBundle", reportBundle);
            manifest.put("overviewBundle", overviewBundle);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "workspace-manifest.json", json(manifest)));
            files.add(write(bundleDirectory, "README.txt", buildWorkspaceReadme(customerId, generatedAt)));

            return new InstallationWorkspaceBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("workspace-manifest.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate installation workspace bundle", e);
        }
    }

    public InstallationWorkspaceResponse workspace(String customerId) {
        InstallationDashboardResponse dashboard = dashboard(customerId);
        InstallationTimelineBundleResponse timelineBundle = generateTimelineBundle(customerId);
        InstallationHealthResponse health = health(customerId);
        InstallationReportResponse report = report(customerId);
        InstallationOverviewResponse overview = overview(customerId);
        return new InstallationWorkspaceResponse(
                customerId,
                Instant.now(),
                dashboard,
                timelineBundle,
                health,
                report,
                overview
        );
    }

    public InstallationWorkspaceExportResponse exportWorkspace(String customerId) {
        InstallationWorkspaceResponse workspace = workspace(customerId);
        Instant generatedAt = workspace.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path exportDirectory = exportRoot.resolve(scope).resolve("workspace-export");
            Files.createDirectories(exportDirectory);

            Path workspaceJsonPath = exportDirectory.resolve("workspace.json");
            Path workspaceHtmlPath = exportDirectory.resolve("workspace.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(workspaceJsonPath, json(workspace));
            Files.writeString(workspaceHtmlPath, buildWorkspaceHtml(workspace));
            Files.writeString(readmePath, buildWorkspaceExportReadme(workspace));

            return new InstallationWorkspaceExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    workspaceJsonPath.toAbsolutePath().toString(),
                    workspaceHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export installation workspace", e);
        }
    }

    public InstallationArtifactCatalogResponse generateArtifactCatalog(String customerId) {
        return new InstallationArtifactCatalogResponse(
                customerId,
                Instant.now(),
                exportDashboard(customerId),
                exportTimeline(customerId),
                exportHealth(customerId),
                exportReport(customerId),
                exportOverview(customerId),
                exportWorkspace(customerId),
                generateWorkspaceBundle(customerId)
        );
    }

    public InstallationArtifactCatalogExportResponse exportArtifactCatalog(String customerId) {
        InstallationArtifactCatalogResponse catalog = generateArtifactCatalog(customerId);
        Instant generatedAt = catalog.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path exportDirectory = exportRoot.resolve(scope).resolve("artifact-catalog-export");
            Files.createDirectories(exportDirectory);

            Path catalogJsonPath = exportDirectory.resolve("artifact-catalog.json");
            Path catalogHtmlPath = exportDirectory.resolve("artifact-catalog.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(catalogJsonPath, json(catalog));
            Files.writeString(catalogHtmlPath, buildArtifactCatalogHtml(catalog));
            Files.writeString(readmePath, buildArtifactCatalogExportReadme(catalog));

            return new InstallationArtifactCatalogExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    catalogJsonPath.toAbsolutePath().toString(),
                    catalogHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export installation artifact catalog", e);
        }
    }

    public InstallationArtifactCatalogBundleResponse generateArtifactCatalogBundle(String customerId) {
        InstallationArtifactCatalogResponse catalog = generateArtifactCatalog(customerId);
        Instant generatedAt = catalog.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path bundleDirectory = exportRoot.resolve(scope).resolve("artifact-catalog");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "artifact-catalog.json", json(catalog)));
            files.add(write(bundleDirectory, "artifact-catalog.html", buildArtifactCatalogHtml(catalog)));
            files.add(write(bundleDirectory, "README.txt", buildArtifactCatalogReadme(catalog)));

            return new InstallationArtifactCatalogBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("artifact-catalog.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("artifact-catalog.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate installation artifact catalog bundle", e);
        }
    }

    public InstallationHealthResponse health(String customerId) {
        List<CustomerInstallationResponse> installations = installationService.list(customerId);
        Map<String, Integer> clientTypeCounts = new LinkedHashMap<>();
        for (CustomerInstallationResponse installation : installations) {
            clientTypeCounts.merge(installation.clientType(), 1, Integer::sum);
        }
        List<InstallationTimelineEntryResponse> recentFailures = installations.stream()
                .filter(installation -> "FAILED".equals(installation.status()))
                .limit(10)
                .map(InstallationTimelineEntryResponse::from)
                .toList();
        return new InstallationHealthResponse(
                customerId,
                Instant.now(),
                installations.size(),
                countByStatus(installations, "COMPLETED"),
                countByStatus(installations, "FAILED"),
                countByStatus(installations, "DRY_RUN"),
                countByStatus(installations, "PENDING"),
                clientTypeCounts,
                recentFailures
        );
    }

    public InstallationHealthBundleResponse generateHealthBundle(String customerId) {
        InstallationHealthResponse health = health(customerId);
        Instant generatedAt = health.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path bundleDirectory = exportRoot.resolve(scope).resolve("health");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "health.json", json(health)));
            files.add(write(bundleDirectory, "health.md", buildHealthMarkdown(health)));
            files.add(write(bundleDirectory, "health.html", buildHealthHtml(health)));
            files.add(write(bundleDirectory, "README.txt", buildHealthReadme(health)));

            return new InstallationHealthBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("health.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("health.md").toAbsolutePath().toString(),
                    bundleDirectory.resolve("health.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate installation health bundle", e);
        }
    }

    public InstallationHealthExportResponse exportHealth(String customerId) {
        InstallationHealthResponse health = health(customerId);
        Instant generatedAt = health.generatedAt();
        try {
            String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
            Path exportDirectory = exportRoot.resolve(scope).resolve("health-export");
            Files.createDirectories(exportDirectory);

            Path healthJsonPath = exportDirectory.resolve("health.json");
            Path healthHtmlPath = exportDirectory.resolve("health.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(healthJsonPath, json(health));
            Files.writeString(healthHtmlPath, buildHealthHtml(health));
            Files.writeString(readmePath, buildHealthExportReadme(health));

            return new InstallationHealthExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    healthJsonPath.toAbsolutePath().toString(),
                    healthHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export installation health", e);
        }
    }

    private int countByStatus(List<CustomerInstallationResponse> installations, String status) {
        return (int) installations.stream().filter(installation -> status.equals(installation.status())).count();
    }

    private String buildMarkdown(InstallationDashboardResponse dashboard) {
        return """
                # Installation Dashboard

                - Customer: %s
                - Generated at: %s
                - Total installations: %s
                - Completed: %s
                - Failed: %s
                - Dry run: %s
                - Pending: %s
                - Total provisioned agents: %s

                ## Latest Installation

                - Installation: %s
                - Status: %s
                - Client type: %s
                - Deployment job id: %s
                """.formatted(
                safe(dashboard.customerId(), "ALL"),
                dashboard.generatedAt(),
                dashboard.totalInstallations(),
                dashboard.completedInstallations(),
                dashboard.failedInstallations(),
                dashboard.dryRunInstallations(),
                dashboard.pendingInstallations(),
                dashboard.totalProvisionedAgents(),
                dashboard.latestInstallation() == null ? "<none>" : dashboard.latestInstallation().installationName(),
                dashboard.latestInstallation() == null ? "<none>" : dashboard.latestInstallation().status(),
                dashboard.latestInstallation() == null ? "<none>" : dashboard.latestInstallation().clientType(),
                dashboard.latestInstallation() == null ? "<none>" : safe(dashboard.latestInstallation().deploymentJobId(), "<not-generated>")
        );
    }

    private String buildDashboardCsv(InstallationDashboardResponse dashboard) {
        return String.join(System.lineSeparator(),
                "customerId,totalInstallations,completedInstallations,failedInstallations,dryRunInstallations,pendingInstallations,totalProvisionedAgents,latestInstallationId,latestInstallationName,latestStatus,latestClientType",
                String.join(",",
                        csv(dashboard.customerId()),
                        csv(String.valueOf(dashboard.totalInstallations())),
                        csv(String.valueOf(dashboard.completedInstallations())),
                        csv(String.valueOf(dashboard.failedInstallations())),
                        csv(String.valueOf(dashboard.dryRunInstallations())),
                        csv(String.valueOf(dashboard.pendingInstallations())),
                        csv(String.valueOf(dashboard.totalProvisionedAgents())),
                        csv(dashboard.latestInstallation() == null ? null : dashboard.latestInstallation().installationJobId()),
                        csv(dashboard.latestInstallation() == null ? null : dashboard.latestInstallation().installationName()),
                        csv(dashboard.latestInstallation() == null ? null : dashboard.latestInstallation().status()),
                        csv(dashboard.latestInstallation() == null ? null : dashboard.latestInstallation().clientType()))
        );
    }

    private String buildHtml(InstallationDashboardResponse dashboard) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Installation Dashboard</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f3efe8; color: #1f2933; }
                    .page { max-width: 1000px; margin: 0 auto; padding: 28px; }
                    .hero { background: white; border: 1px solid #d7cab9; border-radius: 24px; padding: 26px; box-shadow: 0 16px 36px rgba(0,0,0,0.08); }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 14px; margin-top: 20px; }
                    .card { background: #fffaf1; border: 1px solid #e0d4c2; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #7c7063; }
                    .value { font-size: 26px; margin-top: 8px; color: #8b4c1f; font-weight: 700; }
                    .section { background: white; border: 1px solid #d7cab9; border-radius: 20px; padding: 22px; margin-top: 18px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Installation Dashboard</h1>
                      <div>Customer scope: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Total</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Completed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Dry Run</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Pending</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Agents</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Latest Installation</h2>
                      <ul>
                        <li>Installation: %s</li>
                        <li>Status: %s</li>
                        <li>Client type: %s</li>
                        <li>Deployment job id: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(safe(dashboard.customerId(), "ALL")),
                escapeHtml(String.valueOf(dashboard.totalInstallations())),
                escapeHtml(String.valueOf(dashboard.completedInstallations())),
                escapeHtml(String.valueOf(dashboard.failedInstallations())),
                escapeHtml(String.valueOf(dashboard.dryRunInstallations())),
                escapeHtml(String.valueOf(dashboard.pendingInstallations())),
                escapeHtml(String.valueOf(dashboard.totalProvisionedAgents())),
                escapeHtml(dashboard.latestInstallation() == null ? "<none>" : dashboard.latestInstallation().installationName()),
                escapeHtml(dashboard.latestInstallation() == null ? "<none>" : dashboard.latestInstallation().status()),
                escapeHtml(dashboard.latestInstallation() == null ? "<none>" : dashboard.latestInstallation().clientType()),
                escapeHtml(dashboard.latestInstallation() == null ? "<none>" : safe(dashboard.latestInstallation().deploymentJobId(), "<not-generated>"))
        );
    }

    private String buildReadme(InstallationDashboardResponse dashboard) {
        return """
                Installation dashboard bundle
                ============================

                Customer scope: %s
                Generated at: %s

                Files:
                - dashboard.json
                - dashboard.md
                - dashboard.html
                - README.txt
                """.formatted(
                safe(dashboard.customerId(), "ALL"),
                dashboard.generatedAt()
        );
    }

    private String buildDashboardExportReadme(InstallationDashboardResponse dashboard) {
        return """
                Installation dashboard export
                ============================

                Customer scope: %s
                Generated at: %s

                Files:
                - dashboard.json
                - dashboard.csv
                - dashboard.html
                - README.txt
                """.formatted(
                safe(dashboard.customerId(), "ALL"),
                dashboard.generatedAt()
        );
    }

    private String buildTimelineCsv(List<InstallationTimelineEntryResponse> timeline) {
        List<String> lines = new ArrayList<>();
        lines.add("installationJobId,installationName,customerId,clientType,status,createdAt,completedAt,durationSeconds,agentCount,deploymentJobId");
        for (InstallationTimelineEntryResponse entry : timeline) {
            lines.add(String.join(",",
                    csv(entry.installationJobId()),
                    csv(entry.installationName()),
                    csv(entry.customerId()),
                    csv(entry.clientType()),
                    csv(entry.status()),
                    csv(entry.createdAt() == null ? null : entry.createdAt().toString()),
                    csv(entry.completedAt() == null ? null : entry.completedAt().toString()),
                    csv(entry.durationSeconds() == null ? null : String.valueOf(entry.durationSeconds())),
                    csv(String.valueOf(entry.agentCount())),
                    csv(entry.deploymentJobId())));
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildTimelineMarkdown(String customerId,
                                         List<InstallationTimelineEntryResponse> timeline,
                                         Instant generatedAt) {
        StringBuilder builder = new StringBuilder("""
                # Installation Timeline

                - Customer: %s
                - Generated at: %s
                - Entries: %s

                ## Entries

                """.formatted(safe(customerId, "ALL"), generatedAt, timeline.size()));
        for (InstallationTimelineEntryResponse entry : timeline) {
            builder.append("- ")
                    .append(entry.installationName())
                    .append(" | ")
                    .append(entry.status())
                    .append(" | ")
                    .append(entry.clientType())
                    .append(" | created ")
                    .append(entry.createdAt())
                    .append(" | duration ")
                    .append(entry.durationSeconds() == null ? "<in-progress>" : entry.durationSeconds() + "s")
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildTimelineHtml(String customerId,
                                     List<InstallationTimelineEntryResponse> timeline,
                                     Instant generatedAt) {
        StringBuilder rows = new StringBuilder();
        for (InstallationTimelineEntryResponse entry : timeline) {
            rows.append("""
                    <tr>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(entry.installationName()),
                    escapeHtml(entry.status()),
                    escapeHtml(entry.clientType()),
                    escapeHtml(entry.createdAt() == null ? "<none>" : entry.createdAt().toString()),
                    escapeHtml(entry.durationSeconds() == null ? "<in-progress>" : entry.durationSeconds() + "s"),
                    escapeHtml(entry.deploymentJobId() == null ? "<none>" : entry.deploymentJobId())
            ));
        }
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Installation Timeline</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe7; color: #1f2933; }
                    .page { max-width: 1100px; margin: 0 auto; padding: 28px; }
                    .shell { background: white; border: 1px solid #d9cdbf; border-radius: 24px; padding: 24px; box-shadow: 0 16px 36px rgba(0,0,0,0.08); }
                    table { width: 100%%; border-collapse: collapse; margin-top: 18px; }
                    th, td { text-align: left; padding: 12px; border-bottom: 1px solid #e8dccb; font-size: 14px; }
                    th { text-transform: uppercase; letter-spacing: 0.08em; font-size: 12px; color: #7b6d60; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <div class="shell">
                      <h1>Installation Timeline</h1>
                      <div>Customer scope: %s</div>
                      <div>Generated at: %s</div>
                      <table>
                        <thead>
                          <tr>
                            <th>Installation</th>
                            <th>Status</th>
                            <th>Client Type</th>
                            <th>Created</th>
                            <th>Duration</th>
                            <th>Deployment Job</th>
                          </tr>
                        </thead>
                        <tbody>
                          %s
                        </tbody>
                      </table>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(safe(customerId, "ALL")),
                escapeHtml(generatedAt.toString()),
                rows
        );
    }

    private String buildTimelineReadme(String customerId, Instant generatedAt) {
        return """
                Installation timeline bundle
                ===========================

                Customer scope: %s
                Generated at: %s

                Files:
                - timeline.json
                - timeline.csv
                - timeline.md
                - timeline.html
                - README.txt
                """.formatted(safe(customerId, "ALL"), generatedAt);
    }

    private String buildTimelineExportReadme(String customerId, Instant generatedAt) {
        return """
                Installation timeline export
                ===========================

                Customer scope: %s
                Generated at: %s

                Files:
                - timeline.json
                - timeline.csv
                - timeline.html
                - README.txt
                """.formatted(safe(customerId, "ALL"), generatedAt);
    }

    private String buildReportMarkdown(InstallationDashboardResponse dashboard,
                                       List<InstallationTimelineEntryResponse> timeline,
                                       Instant generatedAt) {
        CustomerInstallationResponse latest = dashboard.latestInstallation();
        return """
                # Installation Report

                - Customer: %s
                - Generated at: %s
                - Total installations: %s
                - Completed: %s
                - Failed: %s
                - Dry run: %s
                - Pending: %s
                - Total provisioned agents: %s

                ## Latest Installation

                - Installation: %s
                - Status: %s
                - Client type: %s
                - Deployment job id: %s
                - Message: %s

                ## Recent Timeline Entries

                %s
                """.formatted(
                safe(dashboard.customerId(), "ALL"),
                generatedAt,
                dashboard.totalInstallations(),
                dashboard.completedInstallations(),
                dashboard.failedInstallations(),
                dashboard.dryRunInstallations(),
                dashboard.pendingInstallations(),
                dashboard.totalProvisionedAgents(),
                latest == null ? "<none>" : latest.installationName(),
                latest == null ? "<none>" : latest.status(),
                latest == null ? "<none>" : latest.clientType(),
                latest == null ? "<none>" : safe(latest.deploymentJobId(), "<not-generated>"),
                latest == null ? "<none>" : safe(latest.message(), "<none>"),
                buildTimelineBullets(timeline)
        );
    }

    private String buildReportHtml(InstallationDashboardResponse dashboard,
                                   List<InstallationTimelineEntryResponse> timeline,
                                   Instant generatedAt) {
        CustomerInstallationResponse latest = dashboard.latestInstallation();
        StringBuilder timelineRows = new StringBuilder();
        int count = 0;
        for (InstallationTimelineEntryResponse entry : timeline) {
            if (count++ >= 10) {
                break;
            }
            timelineRows.append("""
                    <tr>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(entry.installationName()),
                    escapeHtml(entry.status()),
                    escapeHtml(entry.createdAt() == null ? "<none>" : entry.createdAt().toString()),
                    escapeHtml(entry.durationSeconds() == null ? "<in-progress>" : entry.durationSeconds() + "s")
            ));
        }
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Installation Report</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe7; color: #1f2933; }
                    .page { max-width: 1100px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: white; border: 1px solid #d9cdbf; border-radius: 24px; padding: 24px; box-shadow: 0 16px 36px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff9f0; border: 1px solid #e2d4c3; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #7b6d60; }
                    .value { font-size: 26px; margin-top: 8px; color: #8b4c1f; font-weight: 700; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 14px; }
                    th, td { text-align: left; padding: 12px; border-bottom: 1px solid #eadfce; }
                    th { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #7b6d60; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Installation Report</h1>
                      <div>Customer scope: %s</div>
                      <div>Generated at: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Total</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Completed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Agents</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Latest Installation</h2>
                      <ul>
                        <li>Installation: %s</li>
                        <li>Status: %s</li>
                        <li>Client type: %s</li>
                        <li>Deployment job id: %s</li>
                        <li>Message: %s</li>
                      </ul>
                    </section>
                    <section class="section">
                      <h2>Recent Timeline</h2>
                      <table>
                        <thead>
                          <tr>
                            <th>Installation</th>
                            <th>Status</th>
                            <th>Created</th>
                            <th>Duration</th>
                          </tr>
                        </thead>
                        <tbody>
                          %s
                        </tbody>
                      </table>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(safe(dashboard.customerId(), "ALL")),
                escapeHtml(generatedAt.toString()),
                escapeHtml(String.valueOf(dashboard.totalInstallations())),
                escapeHtml(String.valueOf(dashboard.completedInstallations())),
                escapeHtml(String.valueOf(dashboard.failedInstallations())),
                escapeHtml(String.valueOf(dashboard.totalProvisionedAgents())),
                escapeHtml(latest == null ? "<none>" : latest.installationName()),
                escapeHtml(latest == null ? "<none>" : latest.status()),
                escapeHtml(latest == null ? "<none>" : latest.clientType()),
                escapeHtml(latest == null ? "<none>" : safe(latest.deploymentJobId(), "<not-generated>")),
                escapeHtml(latest == null ? "<none>" : safe(latest.message(), "<none>")),
                timelineRows
        );
    }

    private String buildReportReadme(InstallationDashboardResponse dashboard, Instant generatedAt) {
        return """
                Installation report bundle
                ==========================

                Customer scope: %s
                Generated at: %s

                Files:
                - dashboard.json
                - timeline.json
                - latest-installation.json
                - report.md
                - report.html
                - README.txt
                """.formatted(safe(dashboard.customerId(), "ALL"), generatedAt);
    }

    private String buildReportExportReadme(InstallationReportResponse report) {
        return """
                Installation report export
                =========================

                Customer scope: %s
                Generated at: %s

                Files:
                - report.json
                - report.html
                - README.txt
                """.formatted(safe(report.customerId(), "ALL"), report.generatedAt());
    }

    private String buildOverviewMarkdown(InstallationOverviewResponse overview) {
        InstallationDashboardResponse dashboard = overview.dashboard();
        InstallationHealthResponse health = overview.health();
        InstallationReportResponse report = overview.report();
        return """
                # Installation Overview

                - Customer: %s
                - Generated at: %s
                - Total installations: %s
                - Completed: %s
                - Failed: %s
                - Pending: %s
                - Latest installation: %s

                ## Health

                - Failed installations: %s
                - Dry run installations: %s
                - Recent failures tracked: %s

                ## Report

                - Latest report installation: %s
                - Timeline entries: %s
                """.formatted(
                safe(overview.customerId(), "ALL"),
                overview.generatedAt(),
                dashboard.totalInstallations(),
                dashboard.completedInstallations(),
                dashboard.failedInstallations(),
                dashboard.pendingInstallations(),
                dashboard.latestInstallation() == null ? "<none>" : dashboard.latestInstallation().installationName(),
                health.failedInstallations(),
                health.dryRunInstallations(),
                health.recentFailures().size(),
                report.latestInstallation() == null ? "<none>" : report.latestInstallation().installationName(),
                report.timeline().size()
        );
    }

    private String buildOverviewHtml(InstallationOverviewResponse overview) {
        InstallationDashboardResponse dashboard = overview.dashboard();
        InstallationHealthResponse health = overview.health();
        InstallationReportResponse report = overview.report();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Installation Overview</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe7; color: #1f2933; }
                    .page { max-width: 1100px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: white; border: 1px solid #d9cdbf; border-radius: 24px; padding: 24px; box-shadow: 0 16px 36px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff9f0; border: 1px solid #e2d4c3; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #7b6d60; }
                    .value { font-size: 26px; margin-top: 8px; color: #8b4c1f; font-weight: 700; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Installation Overview</h1>
                      <div>Customer scope: %s</div>
                      <div>Generated at: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Total</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Completed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Pending</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Health</h2>
                      <ul>
                        <li>Failed installations: %s</li>
                        <li>Dry run installations: %s</li>
                        <li>Recent failures tracked: %s</li>
                      </ul>
                    </section>
                    <section class="section">
                      <h2>Latest Report Context</h2>
                      <ul>
                        <li>Latest installation: %s</li>
                        <li>Timeline entries: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(safe(overview.customerId(), "ALL")),
                escapeHtml(overview.generatedAt().toString()),
                escapeHtml(String.valueOf(dashboard.totalInstallations())),
                escapeHtml(String.valueOf(dashboard.completedInstallations())),
                escapeHtml(String.valueOf(dashboard.failedInstallations())),
                escapeHtml(String.valueOf(dashboard.pendingInstallations())),
                escapeHtml(String.valueOf(health.failedInstallations())),
                escapeHtml(String.valueOf(health.dryRunInstallations())),
                escapeHtml(String.valueOf(health.recentFailures().size())),
                escapeHtml(report.latestInstallation() == null ? "<none>" : report.latestInstallation().installationName()),
                escapeHtml(String.valueOf(report.timeline().size()))
        );
    }

    private String buildOverviewReadme(InstallationOverviewResponse overview) {
        return """
                Installation overview bundle
                ============================

                Customer scope: %s
                Generated at: %s

                Files:
                - overview.json
                - dashboard.json
                - health.json
                - report.json
                - overview.md
                - overview.html
                - README.txt
                """.formatted(
                safe(overview.customerId(), "ALL"),
                overview.generatedAt()
        );
    }

    private String buildOverviewExportReadme(InstallationOverviewResponse overview) {
        return """
                Installation overview export
                ===========================

                Customer scope: %s
                Generated at: %s

                Files:
                - overview.json
                - overview.html
                - README.txt
                """.formatted(
                safe(overview.customerId(), "ALL"),
                overview.generatedAt()
        );
    }

    private String buildWorkspaceReadme(String customerId, Instant generatedAt) {
        return """
                Installation workspace bundle
                ============================

                Customer scope: %s
                Generated at: %s

                This workspace manifest references the generated installation bundles for:
                - dashboard
                - timeline
                - health
                - report
                - overview

                Use `workspace-manifest.json` as the single entry point for delivery and ops handoff.
                """.formatted(
                safe(customerId, "ALL"),
                generatedAt
        );
    }

    private String buildWorkspaceHtml(InstallationWorkspaceResponse workspace) {
        InstallationDashboardResponse dashboard = workspace.dashboard();
        InstallationHealthResponse health = workspace.health();
        InstallationOverviewResponse overview = workspace.overview();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Installation Workspace</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe7; color: #1f2933; }
                    .page { max-width: 1120px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: white; border: 1px solid #d9cdbf; border-radius: 24px; padding: 24px; box-shadow: 0 16px 36px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff9f0; border: 1px solid #e2d4c3; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #7b6d60; }
                    .value { font-size: 26px; margin-top: 8px; color: #8b4c1f; font-weight: 700; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Installation Workspace</h1>
                      <div>Customer scope: %s</div>
                      <div>Generated at: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Installations</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Completed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Recent Failures</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Workspace Components</h2>
                      <ul>
                        <li>Dashboard latest installation: %s</li>
                        <li>Timeline bundle path: %s</li>
                        <li>Health failed installations: %s</li>
                        <li>Overview latest installation: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(safe(workspace.customerId(), "ALL")),
                escapeHtml(workspace.generatedAt().toString()),
                escapeHtml(String.valueOf(dashboard.totalInstallations())),
                escapeHtml(String.valueOf(dashboard.completedInstallations())),
                escapeHtml(String.valueOf(dashboard.failedInstallations())),
                escapeHtml(String.valueOf(health.recentFailures().size())),
                escapeHtml(dashboard.latestInstallation() == null ? "<none>" : dashboard.latestInstallation().installationName()),
                escapeHtml(workspace.timelineBundle().bundleDirectory()),
                escapeHtml(String.valueOf(health.failedInstallations())),
                escapeHtml(overview.report().latestInstallation() == null ? "<none>" : overview.report().latestInstallation().installationName())
        );
    }

    private String buildWorkspaceExportReadme(InstallationWorkspaceResponse workspace) {
        return """
                Installation workspace export
                ============================

                Customer scope: %s
                Generated at: %s

                Files:
                - workspace.json
                - workspace.html
                - README.txt
                """.formatted(
                safe(workspace.customerId(), "ALL"),
                workspace.generatedAt()
        );
    }

    private String buildArtifactCatalogHtml(InstallationArtifactCatalogResponse catalog) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Installation Artifact Catalog</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe7; color: #1f2933; }
                    .page { max-width: 1100px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: white; border: 1px solid #d9cdbf; border-radius: 24px; padding: 24px; box-shadow: 0 16px 36px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.8; }
                    code { background: #f8f1e5; padding: 2px 6px; border-radius: 8px; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Installation Artifact Catalog</h1>
                      <div>Customer scope: %s</div>
                      <div>Generated at: %s</div>
                    </section>
                    <section class="section">
                      <h2>Artifacts</h2>
                      <ul>
                        <li>Dashboard export: <code>%s</code></li>
                        <li>Timeline export: <code>%s</code></li>
                        <li>Health export: <code>%s</code></li>
                        <li>Report export: <code>%s</code></li>
                        <li>Overview export: <code>%s</code></li>
                        <li>Workspace export: <code>%s</code></li>
                        <li>Workspace bundle: <code>%s</code></li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(safe(catalog.customerId(), "ALL")),
                escapeHtml(catalog.generatedAt().toString()),
                escapeHtml(catalog.dashboardExport().exportDirectory()),
                escapeHtml(catalog.timelineExport().exportDirectory()),
                escapeHtml(catalog.healthExport().exportDirectory()),
                escapeHtml(catalog.reportExport().exportDirectory()),
                escapeHtml(catalog.overviewExport().exportDirectory()),
                escapeHtml(catalog.workspaceExport().exportDirectory()),
                escapeHtml(catalog.workspaceBundle().bundleDirectory())
        );
    }

    private String buildArtifactCatalogReadme(InstallationArtifactCatalogResponse catalog) {
        return """
                Installation artifact catalog bundle
                ====================================

                Customer scope: %s
                Generated at: %s

                Files:
                - artifact-catalog.json
                - artifact-catalog.html
                - README.txt

                This catalog references all generated installation exports and the workspace bundle.
                """.formatted(
                safe(catalog.customerId(), "ALL"),
                catalog.generatedAt()
        );
    }

    private String buildArtifactCatalogExportReadme(InstallationArtifactCatalogResponse catalog) {
        return """
                Installation artifact catalog export
                ====================================

                Customer scope: %s
                Generated at: %s

                Files:
                - artifact-catalog.json
                - artifact-catalog.html
                - README.txt
                """.formatted(
                safe(catalog.customerId(), "ALL"),
                catalog.generatedAt()
        );
    }

    private String buildHealthMarkdown(InstallationHealthResponse health) {
        return """
                # Installation Health

                - Customer: %s
                - Generated at: %s
                - Total installations: %s
                - Completed: %s
                - Failed: %s
                - Dry run: %s
                - Pending: %s

                ## Client Type Counts

                %s

                ## Recent Failures

                %s
                """.formatted(
                safe(health.customerId(), "ALL"),
                health.generatedAt(),
                health.totalInstallations(),
                health.completedInstallations(),
                health.failedInstallations(),
                health.dryRunInstallations(),
                health.pendingInstallations(),
                buildClientTypeBullets(health.clientTypeCounts()),
                buildFailureBullets(health.recentFailures())
        );
    }

    private String buildHealthHtml(InstallationHealthResponse health) {
        StringBuilder clientTypeRows = new StringBuilder();
        for (Map.Entry<String, Integer> entry : health.clientTypeCounts().entrySet()) {
            clientTypeRows.append("<li>")
                    .append(escapeHtml(entry.getKey()))
                    .append(": ")
                    .append(escapeHtml(String.valueOf(entry.getValue())))
                    .append("</li>");
        }
        StringBuilder failureRows = new StringBuilder();
        for (InstallationTimelineEntryResponse failure : health.recentFailures()) {
            failureRows.append("""
                    <tr>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(failure.installationName()),
                    escapeHtml(failure.createdAt() == null ? "<none>" : failure.createdAt().toString()),
                    escapeHtml(failure.errorMessage() == null ? "<none>" : failure.errorMessage())
            ));
        }
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Installation Health</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f5eee8; color: #1f2933; }
                    .page { max-width: 1080px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: white; border: 1px solid #d9cdbf; border-radius: 24px; padding: 24px; box-shadow: 0 16px 36px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff9f0; border: 1px solid #e2d4c3; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #7b6d60; }
                    .value { font-size: 26px; margin-top: 8px; color: #8b4c1f; font-weight: 700; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 14px; }
                    th, td { text-align: left; padding: 12px; border-bottom: 1px solid #eadfce; }
                    th { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #7b6d60; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Installation Health</h1>
                      <div>Customer scope: %s</div>
                      <div>Generated at: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Total</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Completed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Dry Run</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Pending</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Client Type Counts</h2>
                      <ul>%s</ul>
                    </section>
                    <section class="section">
                      <h2>Recent Failures</h2>
                      <table>
                        <thead>
                          <tr>
                            <th>Installation</th>
                            <th>Created</th>
                            <th>Error</th>
                          </tr>
                        </thead>
                        <tbody>%s</tbody>
                      </table>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(safe(health.customerId(), "ALL")),
                escapeHtml(health.generatedAt().toString()),
                escapeHtml(String.valueOf(health.totalInstallations())),
                escapeHtml(String.valueOf(health.completedInstallations())),
                escapeHtml(String.valueOf(health.failedInstallations())),
                escapeHtml(String.valueOf(health.dryRunInstallations())),
                escapeHtml(String.valueOf(health.pendingInstallations())),
                clientTypeRows,
                failureRows
        );
    }

    private String buildHealthReadme(InstallationHealthResponse health) {
        return """
                Installation health bundle
                ==========================

                Customer scope: %s
                Generated at: %s

                Files:
                - health.json
                - health.md
                - health.html
                - README.txt
                """.formatted(safe(health.customerId(), "ALL"), health.generatedAt());
    }

    private String buildHealthExportReadme(InstallationHealthResponse health) {
        return """
                Installation health export
                ==========================

                Customer scope: %s
                Generated at: %s

                Files:
                - health.json
                - health.html
                - README.txt
                """.formatted(safe(health.customerId(), "ALL"), health.generatedAt());
    }

    private String buildTimelineBullets(List<InstallationTimelineEntryResponse> timeline) {
        if (timeline.isEmpty()) {
            return "- <no entries>";
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (InstallationTimelineEntryResponse entry : timeline) {
            if (count++ >= 10) {
                break;
            }
            builder.append("- ")
                    .append(entry.installationName())
                    .append(" | ")
                    .append(entry.status())
                    .append(" | created ")
                    .append(entry.createdAt())
                    .append(" | duration ")
                    .append(entry.durationSeconds() == null ? "<in-progress>" : entry.durationSeconds() + "s")
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildClientTypeBullets(Map<String, Integer> clientTypeCounts) {
        if (clientTypeCounts.isEmpty()) {
            return "- <none>";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : clientTypeCounts.entrySet()) {
            builder.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String buildFailureBullets(List<InstallationTimelineEntryResponse> failures) {
        if (failures.isEmpty()) {
            return "- <none>";
        }
        StringBuilder builder = new StringBuilder();
        for (InstallationTimelineEntryResponse failure : failures) {
            builder.append("- ")
                    .append(failure.installationName())
                    .append(" | ")
                    .append(failure.createdAt())
                    .append(" | ")
                    .append(failure.errorMessage() == null ? "<no error message>" : failure.errorMessage())
                    .append(System.lineSeparator());
        }
        return builder.toString();
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
            throw new IllegalStateException("Failed to serialize installation dashboard payload", e);
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
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
