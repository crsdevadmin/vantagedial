package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerCommandCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioBundleResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioExportResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterBundleResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterExportResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageExportResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageResponse;
import com.vantage.dialer.api.dto.PlatformHealthBundleResponse;
import com.vantage.dialer.api.dto.PlatformHealthExportResponse;
import com.vantage.dialer.api.dto.PlatformOverviewBundleResponse;
import com.vantage.dialer.api.dto.PlatformOverviewExportResponse;
import com.vantage.dialer.api.dto.PlatformReportBundleResponse;
import com.vantage.dialer.api.dto.PlatformReportExportResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceExportResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlatformArtifactCatalogService {

    private final PlatformControlCenterService platformControlCenterService;
    private final PlatformWorkspaceService platformWorkspaceService;
    private final PlatformHealthService platformHealthService;
    private final PlatformOverviewService platformOverviewService;
    private final PlatformDeliveryPackageService platformDeliveryPackageService;
    private final PlatformReportService platformReportService;
    private final CustomerPortfolioService customerPortfolioService;
    private final CustomerCommandCenterService customerCommandCenterService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public PlatformArtifactCatalogService(PlatformControlCenterService platformControlCenterService,
                                          PlatformWorkspaceService platformWorkspaceService,
                                          PlatformHealthService platformHealthService,
                                          PlatformOverviewService platformOverviewService,
                                          PlatformDeliveryPackageService platformDeliveryPackageService,
                                          PlatformReportService platformReportService,
                                          CustomerPortfolioService customerPortfolioService,
                                          CustomerCommandCenterService customerCommandCenterService,
                                          ObjectMapper objectMapper,
                                          @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.platformControlCenterService = platformControlCenterService;
        this.platformWorkspaceService = platformWorkspaceService;
        this.platformHealthService = platformHealthService;
        this.platformOverviewService = platformOverviewService;
        this.platformDeliveryPackageService = platformDeliveryPackageService;
        this.platformReportService = platformReportService;
        this.customerPortfolioService = customerPortfolioService;
        this.customerCommandCenterService = customerCommandCenterService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("platform-artifacts");
    }

    public PlatformArtifactCatalogResponse catalog() {
        Instant generatedAt = Instant.now();
        var customerCommandCenter = customerCommandCenterService.commandCenter();
        var platformHealth = platformHealthService.health();
        PlatformControlCenterExportResponse controlCenterExport = platformControlCenterService.export();
        PlatformControlCenterBundleResponse controlCenterBundle = platformControlCenterService.generateBundle();
        PlatformWorkspaceExportResponse workspaceExport = platformWorkspaceService.export();
        PlatformWorkspaceBundleResponse workspaceBundle = platformWorkspaceService.generateBundle();
        PlatformHealthExportResponse healthExport = platformHealthService.export();
        PlatformHealthBundleResponse healthBundle = platformHealthService.generateBundle();
        PlatformOverviewExportResponse overviewExport = platformOverviewService.export();
        PlatformOverviewBundleResponse overviewBundle = platformOverviewService.generateBundle();
        PlatformDeliveryPackageExportResponse deliveryPackageExport = platformDeliveryPackageService.export();
        PlatformDeliveryPackageResponse deliveryPackageBundle = platformDeliveryPackageService.generate();
        PlatformReportExportResponse reportExport = platformReportService.export();
        PlatformReportBundleResponse reportBundle = platformReportService.generateBundle();
        CustomerPortfolioExportResponse customerPortfolioExport = customerPortfolioService.export();
        CustomerPortfolioBundleResponse customerPortfolioBundle = customerPortfolioService.generateBundle();
        CustomerCommandCenterExportResponse customerCommandCenterExport = customerCommandCenterService.export();
        CustomerCommandCenterBundleResponse customerCommandCenterBundle = customerCommandCenterService.generateBundle();

        return new PlatformArtifactCatalogResponse(
                generatedAt,
                customerCommandCenter.totalCustomers(),
                customerCommandCenter.healthyCustomers(),
                customerCommandCenter.customersWithReport(),
                customerCommandCenter.customersWithArtifactCatalog(),
                platformHealth.recentDeploymentJobIds(),
                platformHealth.recentDeploymentPackageIds(),
                platformHealth.recentDeploymentStatuses(),
                platformHealth.recentDeploymentProviders(),
                platformHealth.recentDeploymentClientTypes(),
                platformHealth.recentDeploymentHosts(),
                platformHealth.recentDeploymentPorts(),
                platformHealth.recentDeploymentPackageTypes(),
                platformHealth.recentDeploymentTargetDirectories(),
                platformHealth.recentDeploymentRemotePackageDirectories(),
                platformHealth.recentDeploymentRemoteBaseDirectories(),
                platformHealth.recentDeploymentDryRuns(),
                platformHealth.recentDeploymentDeployedFlags(),
                platformHealth.recentDeploymentAgentCounts(),
                platformHealth.recentDeploymentBundledFileCounts(),
                platformHealth.recentDeploymentCommandCounts(),
                platformHealth.recentDeploymentExecutedAts(),
                platformHealth.recentDeploymentGeneratedAts(),
                platformHealth.recentDeploymentCreatedAts(),
                platformHealth.recentDeploymentAts(),
                platformHealth.recentDeploymentMessages(),
                platformHealth.recentDeploymentErrorMessages(),
                platformHealth.recentDeploymentAgentIds(),
                platformHealth.recentDeploymentBundledFiles(),
                platformHealth.recentDeploymentCommands(),
                platformHealth.recentDeployments(),
                platformHealth.latestDeploymentSummary(),
                platformHealth.deploymentSnapshot(),
                platformHealth.recentDeploymentHistory(),
                platformHealth.deploymentStatusCounts(),
                platformHealth.latestDeploymentDetail(),
                platformHealth.deploymentOverview(),
                platformHealth.healthy(),
                platformHealth.statusMessage(),
                platformHealth.latestDeployment() == null ? null : platformHealth.latestDeployment().deploymentJobId(),
                platformHealth.latestDeploymentPackageId(),
                platformHealth.latestDeploymentDryRun(),
                platformHealth.latestDeploymentDeployed(),
                platformHealth.latestDeploymentAgentCount(),
                platformHealth.latestDeploymentAgentIds(),
                platformHealth.latestDeploymentBundledFiles(),
                platformHealth.latestDeploymentBundledFileCount(),
                platformHealth.latestDeploymentCommands(),
                platformHealth.latestDeploymentCommandCount(),
                platformHealth.latestDeploymentExecutedAt(),
                platformHealth.latestDeploymentGeneratedAt(),
                platformHealth.latestDeploymentCreatedAt(),
                platformHealth.latestDeployment() == null ? null : platformHealth.latestDeployment().status(),
                platformHealth.latestDeploymentAt(),
                platformHealth.latestDeploymentMessage(),
                platformHealth.latestDeploymentErrorMessage(),
                platformHealth.latestDeploymentHost(),
                platformHealth.latestDeploymentPort(),
                platformHealth.latestDeploymentProvider(),
                platformHealth.latestDeploymentClientType(),
                platformHealth.latestDeploymentPackageType(),
                platformHealth.latestDeploymentTargetDirectory(),
                platformHealth.latestDeploymentRemotePackageDirectory(),
                platformHealth.latestDeploymentRemoteBaseDirectory(),
                controlCenterExport,
                controlCenterBundle,
                workspaceExport,
                workspaceBundle,
                healthExport,
                healthBundle,
                overviewExport,
                overviewBundle,
                deliveryPackageExport,
                deliveryPackageBundle,
                reportExport,
                reportBundle,
                customerPortfolioExport,
                customerPortfolioBundle,
                customerCommandCenterExport,
                customerCommandCenterBundle
        );
    }

    public PlatformArtifactCatalogExportResponse export() {
        PlatformArtifactCatalogResponse catalog = catalog();
        Instant generatedAt = catalog.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve("export");
            Files.createDirectories(exportDirectory);

            Path catalogJsonPath = exportDirectory.resolve("platform-artifact-catalog.json");
            Path catalogHtmlPath = exportDirectory.resolve("platform-artifact-catalog.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(catalogJsonPath, json(catalog));
            Files.writeString(catalogHtmlPath, buildHtml(catalog));
            Files.writeString(readmePath, buildReadme(catalog));

            return new PlatformArtifactCatalogExportResponse(
                    exportDirectory.toAbsolutePath().toString(),
                    catalogJsonPath.toAbsolutePath().toString(),
                    catalogHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export platform artifact catalog", e);
        }
    }

    public PlatformArtifactCatalogBundleResponse generateBundle() {
        PlatformArtifactCatalogResponse catalog = catalog();
        Instant generatedAt = catalog.generatedAt();
        try {
            Path bundleDirectory = exportRoot.resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "platform-artifact-catalog.json", json(catalog)));
            files.add(write(bundleDirectory, "platform-artifact-catalog.html", buildHtml(catalog)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(catalog)));

            return new PlatformArtifactCatalogBundleResponse(
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-artifact-catalog.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-artifact-catalog.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate platform artifact catalog bundle", e);
        }
    }

    private String buildHtml(PlatformArtifactCatalogResponse catalog) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Platform Artifact Catalog</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f5efe7; color: #1f2933; }
                    .page { max-width: 1180px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #dccfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 28px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.8; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Platform Artifact Catalog</h1>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <ul>
                        <li>Total customers: %s</li>
                        <li>Healthy customers: %s</li>
                        <li>Customers with reports: %s</li>
                        <li>Customers with artifact catalog: %s</li>
                        <li>Recent deployment job ids: %s</li>
                        <li>Recent deployment package ids: %s</li>
                        <li>Recent deployment statuses: %s</li>
                        <li>Recent deployment providers: %s</li>
                        <li>Recent deployment client types: %s</li>
                        <li>Recent deployment hosts: %s</li>
                        <li>Recent deployment ports: %s</li>
                        <li>Recent deployment package types: %s</li>
                        <li>Recent deployment target directories: %s</li>
                        <li>Recent deployment remote package directories: %s</li>
                        <li>Recent deployment remote base directories: %s</li>
                        <li>Recent deployment dry runs: %s</li>
                        <li>Recent deployment deployed flags: %s</li>
                        <li>Recent deployment agent counts: %s</li>
                        <li>Recent deployment bundled file counts: %s</li>
                        <li>Recent deployment command counts: %s</li>
                        <li>Recent deployment executed ats: %s</li>
                        <li>Recent deployment generated ats: %s</li>
                        <li>Recent deployment created ats: %s</li>
                        <li>Recent deployment ats: %s</li>
                        <li>Recent deployment messages: %s</li>
                        <li>Recent deployment error messages: %s</li>
                        <li>Recent deployment agent ids: %s</li>
                        <li>Recent deployment bundled files: %s</li>
                        <li>Recent deployment commands: %s</li>
                        <li>Recent deployments: %s</li>
                        <li>Latest deployment summary: %s</li>
                        <li>Latest deployment detail: %s</li>
                        <li>Deployment overview: %s</li>
                        <li>Deployment snapshot: %s</li>
                        <li>Recent deployment history: %s</li>
                        <li>Deployment status counts: %s</li>
                        <li>Latest deployment: %s</li>
                        <li>Latest deployment package id: %s</li>
                        <li>Latest deployment dry run: %s</li>
                        <li>Latest deployment deployed: %s</li>
                        <li>Latest deployment agent count: %s</li>
                        <li>Latest deployment agent ids: %s</li>
                        <li>Latest deployment bundled files: %s</li>
                        <li>Latest deployment bundled file count: %s</li>
                        <li>Latest deployment commands: %s</li>
                        <li>Latest deployment command count: %s</li>
                        <li>Latest deployment executed at: %s</li>
                        <li>Latest deployment generated at: %s</li>
                        <li>Latest deployment created at: %s</li>
                        <li>Latest deployment status: %s</li>
                        <li>Latest deployment at: %s</li>
                        <li>Latest deployment message: %s</li>
                        <li>Latest deployment error message: %s</li>
                        <li>Latest deployment host: %s</li>
                        <li>Latest deployment port: %s</li>
                        <li>Latest deployment provider: %s</li>
                        <li>Latest deployment client type: %s</li>
                        <li>Latest deployment package type: %s</li>
                        <li>Latest deployment target directory: %s</li>
                        <li>Latest deployment remote package directory: %s</li>
                        <li>Latest deployment remote base directory: %s</li>
                      </ul>
                    </section>
                    <section class="section">
                      <h2>Included Exports</h2>
                      <ul>
                        <li>Platform control center export: %s</li>
                        <li>Platform workspace export: %s</li>
                        <li>Platform health export: %s</li>
                        <li>Platform overview export: %s</li>
                        <li>Platform delivery package export: %s</li>
                        <li>Platform report export: %s</li>
                        <li>Customer portfolio export: %s</li>
                        <li>Customer command center export: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(catalog.generatedAt().toString()),
                escapeHtml(catalog.statusMessage()),
                escapeHtml(String.valueOf(catalog.totalCustomers())),
                escapeHtml(String.valueOf(catalog.healthyCustomers())),
                escapeHtml(String.valueOf(catalog.customersWithReports())),
                escapeHtml(String.valueOf(catalog.customersWithArtifactCatalog())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentJobIds())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentPackageIds())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentStatuses())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentProviders())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentClientTypes())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentHosts())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentPorts())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentPackageTypes())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentTargetDirectories())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentRemotePackageDirectories())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentRemoteBaseDirectories())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentDryRuns())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentDeployedFlags())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentAgentCounts())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentBundledFileCounts())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentCommandCounts())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentExecutedAts())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentGeneratedAts())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentCreatedAts())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentAts())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentMessages())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentErrorMessages())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatGroupedStringLists(catalog.recentDeploymentAgentIds())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatGroupedStringLists(catalog.recentDeploymentBundledFiles())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatGroupedStringLists(catalog.recentDeploymentCommands())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentDeployments(catalog.recentDeployments())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestSummary(catalog.latestDeploymentSummary())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestDetail(catalog.latestDeploymentDetail())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatOverview(catalog.deploymentOverview())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatSnapshot(catalog.deploymentSnapshot())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentHistory(catalog.recentDeploymentHistory())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatStatusCounts(catalog.deploymentStatusCounts())),
                escapeHtml(catalog.latestDeploymentJobId() == null ? "None" : catalog.latestDeploymentJobId()),
                escapeHtml(catalog.latestDeploymentPackageId() == null ? "N/A" : catalog.latestDeploymentPackageId()),
                escapeHtml(catalog.latestDeploymentDryRun() == null ? "N/A" : String.valueOf(catalog.latestDeploymentDryRun())),
                escapeHtml(catalog.latestDeploymentDeployed() == null ? "N/A" : String.valueOf(catalog.latestDeploymentDeployed())),
                escapeHtml(catalog.latestDeploymentAgentCount() == null ? "N/A" : String.valueOf(catalog.latestDeploymentAgentCount())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.latestDeploymentAgentIds())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.latestDeploymentBundledFiles())),
                escapeHtml(catalog.latestDeploymentBundledFileCount() == null ? "N/A" : String.valueOf(catalog.latestDeploymentBundledFileCount())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatFlatList(catalog.latestDeploymentCommands())),
                escapeHtml(catalog.latestDeploymentCommandCount() == null ? "N/A" : String.valueOf(catalog.latestDeploymentCommandCount())),
                escapeHtml(catalog.latestDeploymentExecutedAt() == null ? "N/A" : String.valueOf(catalog.latestDeploymentExecutedAt())),
                escapeHtml(catalog.latestDeploymentGeneratedAt() == null ? "N/A" : String.valueOf(catalog.latestDeploymentGeneratedAt())),
                escapeHtml(catalog.latestDeploymentCreatedAt() == null ? "N/A" : String.valueOf(catalog.latestDeploymentCreatedAt())),
                escapeHtml(catalog.latestDeploymentStatus() == null ? "N/A" : catalog.latestDeploymentStatus()),
                escapeHtml(catalog.latestDeploymentAt() == null ? "N/A" : catalog.latestDeploymentAt().toString()),
                escapeHtml(catalog.latestDeploymentMessage() == null ? "N/A" : catalog.latestDeploymentMessage()),
                escapeHtml(catalog.latestDeploymentErrorMessage() == null ? "N/A" : catalog.latestDeploymentErrorMessage()),
                escapeHtml(catalog.latestDeploymentHost() == null ? "N/A" : catalog.latestDeploymentHost()),
                escapeHtml(catalog.latestDeploymentPort() == null ? "N/A" : String.valueOf(catalog.latestDeploymentPort())),
                escapeHtml(catalog.latestDeploymentProvider() == null ? "N/A" : catalog.latestDeploymentProvider()),
                escapeHtml(catalog.latestDeploymentClientType() == null ? "N/A" : catalog.latestDeploymentClientType()),
                escapeHtml(catalog.latestDeploymentPackageType() == null ? "N/A" : catalog.latestDeploymentPackageType()),
                escapeHtml(catalog.latestDeploymentTargetDirectory() == null ? "N/A" : catalog.latestDeploymentTargetDirectory()),
                escapeHtml(catalog.latestDeploymentRemotePackageDirectory() == null ? "N/A" : catalog.latestDeploymentRemotePackageDirectory()),
                escapeHtml(catalog.latestDeploymentRemoteBaseDirectory() == null ? "N/A" : catalog.latestDeploymentRemoteBaseDirectory()),
                escapeHtml(catalog.controlCenterExport().exportDirectory()),
                escapeHtml(catalog.workspaceExport().exportDirectory()),
                escapeHtml(catalog.healthExport().exportDirectory()),
                escapeHtml(catalog.overviewExport().exportDirectory()),
                escapeHtml(catalog.deliveryPackageExport().exportDirectory()),
                escapeHtml(catalog.reportExport().exportDirectory()),
                escapeHtml(catalog.customerPortfolioExport().exportDirectory()),
                escapeHtml(catalog.customerCommandCenterExport().exportDirectory())
        );
    }

    private String buildReadme(PlatformArtifactCatalogResponse catalog) {
        return """
                Platform artifact catalog
                ========================

                Generated at: %s
                Total customers: %s
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
                - platform-artifact-catalog.json
                - platform-artifact-catalog.html
                - README.txt
                """.formatted(
                catalog.generatedAt(),
                catalog.totalCustomers(),
                catalog.healthyCustomers(),
                catalog.customersWithReports(),
                catalog.customersWithArtifactCatalog(),
                catalog.healthy(),
                catalog.statusMessage(),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentJobIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentPackageIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentStatuses()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentProviders()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentClientTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentHosts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentPorts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentPackageTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentTargetDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentRemotePackageDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentRemoteBaseDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentDryRuns()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentDeployedFlags()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentAgentCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentBundledFileCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentCommandCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentExecutedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentGeneratedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentCreatedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentMessages()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.recentDeploymentErrorMessages()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(catalog.recentDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(catalog.recentDeploymentBundledFiles()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(catalog.recentDeploymentCommands()),
                PlatformDeploymentSummaryFormatter.formatRecentDeployments(catalog.recentDeployments()),
                PlatformDeploymentSummaryFormatter.formatLatestSummary(catalog.latestDeploymentSummary()),
                PlatformDeploymentSummaryFormatter.formatLatestDetail(catalog.latestDeploymentDetail()),
                PlatformDeploymentSummaryFormatter.formatOverview(catalog.deploymentOverview()),
                PlatformDeploymentSummaryFormatter.formatSnapshot(catalog.deploymentSnapshot()),
                PlatformDeploymentSummaryFormatter.formatRecentHistory(catalog.recentDeploymentHistory()),
                PlatformDeploymentSummaryFormatter.formatStatusCounts(catalog.deploymentStatusCounts()),
                catalog.latestDeploymentJobId() == null ? "None" : catalog.latestDeploymentJobId(),
                catalog.latestDeploymentPackageId() == null ? "N/A" : catalog.latestDeploymentPackageId(),
                catalog.latestDeploymentDryRun() == null ? "N/A" : catalog.latestDeploymentDryRun(),
                catalog.latestDeploymentDeployed() == null ? "N/A" : catalog.latestDeploymentDeployed(),
                catalog.latestDeploymentAgentCount() == null ? "N/A" : catalog.latestDeploymentAgentCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.latestDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.latestDeploymentBundledFiles()),
                catalog.latestDeploymentBundledFileCount() == null ? "N/A" : catalog.latestDeploymentBundledFileCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(catalog.latestDeploymentCommands()),
                catalog.latestDeploymentCommandCount() == null ? "N/A" : catalog.latestDeploymentCommandCount(),
                catalog.latestDeploymentExecutedAt() == null ? "N/A" : catalog.latestDeploymentExecutedAt(),
                catalog.latestDeploymentGeneratedAt() == null ? "N/A" : catalog.latestDeploymentGeneratedAt(),
                catalog.latestDeploymentCreatedAt() == null ? "N/A" : catalog.latestDeploymentCreatedAt(),
                catalog.latestDeploymentStatus() == null ? "N/A" : catalog.latestDeploymentStatus(),
                catalog.latestDeploymentAt() == null ? "N/A" : catalog.latestDeploymentAt(),
                catalog.latestDeploymentMessage() == null ? "N/A" : catalog.latestDeploymentMessage(),
                catalog.latestDeploymentErrorMessage() == null ? "N/A" : catalog.latestDeploymentErrorMessage(),
                catalog.latestDeploymentHost() == null ? "N/A" : catalog.latestDeploymentHost(),
                catalog.latestDeploymentPort() == null ? "N/A" : catalog.latestDeploymentPort(),
                catalog.latestDeploymentProvider() == null ? "N/A" : catalog.latestDeploymentProvider(),
                catalog.latestDeploymentClientType() == null ? "N/A" : catalog.latestDeploymentClientType(),
                catalog.latestDeploymentPackageType() == null ? "N/A" : catalog.latestDeploymentPackageType(),
                catalog.latestDeploymentTargetDirectory() == null ? "N/A" : catalog.latestDeploymentTargetDirectory(),
                catalog.latestDeploymentRemotePackageDirectory() == null ? "N/A" : catalog.latestDeploymentRemotePackageDirectory(),
                catalog.latestDeploymentRemoteBaseDirectory() == null ? "N/A" : catalog.latestDeploymentRemoteBaseDirectory()
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
            throw new IllegalStateException("Failed to serialize platform artifact catalog", e);
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
