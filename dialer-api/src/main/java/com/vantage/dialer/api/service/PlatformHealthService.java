package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.PlatformHealthBundleResponse;
import com.vantage.dialer.api.dto.PlatformHealthExportResponse;
import com.vantage.dialer.api.dto.PlatformHealthResponse;
import com.vantage.dialer.api.dto.TelephonyDeploymentAuditResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlatformHealthService {

    private final PlatformControlCenterService platformControlCenterService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public PlatformHealthService(PlatformControlCenterService platformControlCenterService,
                                 ObjectMapper objectMapper,
                                 @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.platformControlCenterService = platformControlCenterService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("platform-health");
    }

    public PlatformHealthResponse health() {
        var controlCenter = platformControlCenterService.controlCenter();
        var customers = controlCenter.customerCommandCenter();
        var deployments = controlCenter.deploymentAuditSummary();

        boolean healthy = deployments.failedDeployments() == 0
                && deployments.pendingDeployments() == 0
                && customers.totalCustomers() > 0;

        String statusMessage;
        if (customers.totalCustomers() == 0) {
            statusMessage = "No customers provisioned yet";
        } else if (deployments.failedDeployments() > 0) {
            statusMessage = "Deployment failures require attention";
        } else if (deployments.pendingDeployments() > 0) {
            statusMessage = "Deployments are still in progress";
        } else {
            statusMessage = "Platform is healthy";
        }
        TelephonyDeploymentAuditResponse latestDeployment = deployments.latestDeployment();
        List<String> recentDeploymentJobIds = controlCenter.recentDeploymentJobIds();
        List<String> recentDeploymentPackageIds = controlCenter.recentDeploymentPackageIds();
        List<String> recentDeploymentStatuses = controlCenter.recentDeploymentStatuses();
        List<String> recentDeploymentProviders = controlCenter.recentDeploymentProviders();
        List<String> recentDeploymentClientTypes = controlCenter.recentDeploymentClientTypes();
        List<String> recentDeploymentHosts = controlCenter.recentDeploymentHosts();
        List<Integer> recentDeploymentPorts = controlCenter.recentDeploymentPorts();
        List<String> recentDeploymentPackageTypes = controlCenter.recentDeploymentPackageTypes();
        List<String> recentDeploymentTargetDirectories = controlCenter.recentDeploymentTargetDirectories();
        List<String> recentDeploymentRemotePackageDirectories = controlCenter.recentDeploymentRemotePackageDirectories();
        List<String> recentDeploymentRemoteBaseDirectories = controlCenter.recentDeploymentRemoteBaseDirectories();
        List<Boolean> recentDeploymentDryRuns = controlCenter.recentDeploymentDryRuns();
        List<Boolean> recentDeploymentDeployedFlags = controlCenter.recentDeploymentDeployedFlags();
        List<Integer> recentDeploymentAgentCounts = controlCenter.recentDeploymentAgentCounts();
        List<Integer> recentDeploymentBundledFileCounts = controlCenter.recentDeploymentBundledFileCounts();
        List<Integer> recentDeploymentCommandCounts = controlCenter.recentDeploymentCommandCounts();
        List<Instant> recentDeploymentExecutedAts = controlCenter.recentDeploymentExecutedAts();
        List<Instant> recentDeploymentGeneratedAts = controlCenter.recentDeploymentGeneratedAts();
        List<Instant> recentDeploymentCreatedAts = controlCenter.recentDeploymentCreatedAts();
        List<Instant> recentDeploymentAts = controlCenter.recentDeploymentAts();
        List<String> recentDeploymentMessages = controlCenter.recentDeploymentMessages();
        List<String> recentDeploymentErrorMessages = controlCenter.recentDeploymentErrorMessages();
        List<List<String>> recentDeploymentAgentIds = controlCenter.recentDeploymentAgentIds();
        List<List<String>> recentDeploymentBundledFiles = controlCenter.recentDeploymentBundledFiles();
        List<List<String>> recentDeploymentCommands = controlCenter.recentDeploymentCommands();
        Instant latestDeploymentAt = latestDeployment == null
                ? null
                : latestDeployment.executedAt() != null
                ? latestDeployment.executedAt()
                : latestDeployment.generatedAt() != null
                ? latestDeployment.generatedAt()
                : latestDeployment.createdAt();
        String latestDeploymentMessage = latestDeployment == null
                ? null
                : latestDeployment.message() != null && !latestDeployment.message().isBlank()
                ? latestDeployment.message()
                : latestDeployment.errorMessage();
        String latestDeploymentHost = latestDeployment == null ? null : latestDeployment.host();
        Integer latestDeploymentPort = latestDeployment == null ? null : latestDeployment.port();
        String latestDeploymentProvider = latestDeployment == null ? null : latestDeployment.provider();
        String latestDeploymentClientType = latestDeployment == null ? null : latestDeployment.clientType();
        String latestDeploymentPackageType = latestDeployment == null ? null : latestDeployment.packageType();
        String latestDeploymentTargetDirectory = latestDeployment == null ? null : latestDeployment.targetDirectory();
        String latestDeploymentRemotePackageDirectory = latestDeployment == null ? null : latestDeployment.remotePackageDirectory();
        String latestDeploymentRemoteBaseDirectory = latestDeployment == null ? null : latestDeployment.remoteBaseDirectory();

        return new PlatformHealthResponse(
                Instant.now(),
                customers.totalCustomers(),
                customers.healthyCustomers(),
                customers.customersWithInstallations(),
                customers.customersWithQuotes(),
                customers.customersWithDeliveryPackage(),
                customers.customersWithReport(),
                customers.customersWithArtifactCatalog(),
                deployments.totalDeployments(),
                deployments.pendingDeployments(),
                deployments.failedDeployments(),
                recentDeploymentJobIds,
                recentDeploymentPackageIds,
                recentDeploymentStatuses,
                recentDeploymentProviders,
                recentDeploymentClientTypes,
                recentDeploymentHosts,
                recentDeploymentPorts,
                recentDeploymentPackageTypes,
                recentDeploymentTargetDirectories,
                recentDeploymentRemotePackageDirectories,
                recentDeploymentRemoteBaseDirectories,
                recentDeploymentDryRuns,
                recentDeploymentDeployedFlags,
                recentDeploymentAgentCounts,
                recentDeploymentBundledFileCounts,
                recentDeploymentCommandCounts,
                recentDeploymentExecutedAts,
                recentDeploymentGeneratedAts,
                recentDeploymentCreatedAts,
                recentDeploymentAts,
                recentDeploymentMessages,
                recentDeploymentErrorMessages,
                recentDeploymentAgentIds,
                recentDeploymentBundledFiles,
                recentDeploymentCommands,
                controlCenter.recentDeployments(),
                controlCenter.latestDeploymentSummary(),
                controlCenter.deploymentSnapshot(),
                controlCenter.recentDeploymentHistory(),
                controlCenter.deploymentStatusCounts(),
                controlCenter.latestDeploymentDetail(),
                controlCenter.deploymentOverview(),
                healthy,
                statusMessage,
                latestDeployment == null ? null : latestDeployment.packageId(),
                latestDeployment == null ? null : latestDeployment.dryRun(),
                latestDeployment == null ? null : latestDeployment.deployed(),
                latestDeployment == null || latestDeployment.agentIds() == null ? null : latestDeployment.agentIds().size(),
                latestDeployment == null ? null : latestDeployment.agentIds(),
                latestDeployment == null ? null : latestDeployment.bundledFiles(),
                latestDeployment == null || latestDeployment.bundledFiles() == null ? null : latestDeployment.bundledFiles().size(),
                latestDeployment == null ? null : latestDeployment.commands(),
                latestDeployment == null || latestDeployment.commands() == null ? null : latestDeployment.commands().size(),
                latestDeployment == null ? null : latestDeployment.executedAt(),
                latestDeployment == null ? null : latestDeployment.generatedAt(),
                latestDeployment == null ? null : latestDeployment.createdAt(),
                latestDeploymentAt,
                latestDeploymentMessage,
                latestDeployment == null ? null : latestDeployment.errorMessage(),
                latestDeploymentHost,
                latestDeploymentPort,
                latestDeploymentProvider,
                latestDeploymentClientType,
                latestDeploymentPackageType,
                latestDeploymentTargetDirectory,
                latestDeploymentRemotePackageDirectory,
                latestDeploymentRemoteBaseDirectory,
                latestDeployment
        );
    }

    public PlatformHealthExportResponse export() {
        PlatformHealthResponse health = health();
        Instant generatedAt = health.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve("export");
            Files.createDirectories(exportDirectory);

            Path healthJsonPath = exportDirectory.resolve("platform-health.json");
            Path healthHtmlPath = exportDirectory.resolve("platform-health.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(healthJsonPath, json(health));
            Files.writeString(healthHtmlPath, buildHtml(health));
            Files.writeString(readmePath, buildReadme(health));

            return new PlatformHealthExportResponse(
                    exportDirectory.toAbsolutePath().toString(),
                    healthJsonPath.toAbsolutePath().toString(),
                    healthHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export platform health", e);
        }
    }

    public PlatformHealthBundleResponse generateBundle() {
        PlatformHealthResponse health = health();
        Instant generatedAt = health.generatedAt();
        try {
            Path bundleDirectory = exportRoot.resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "platform-health.json", json(health)));
            files.add(write(bundleDirectory, "platform-health.html", buildHtml(health)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(health)));

            return new PlatformHealthBundleResponse(
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-health.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-health.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate platform health bundle", e);
        }
    }

    private String buildHtml(PlatformHealthResponse health) {
        String latestDeployment = health.latestDeployment() == null ? "None" : health.latestDeployment().deploymentJobId();
        String recentDeploymentJobIds = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentJobIds());
        String recentDeploymentPackageIds = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentPackageIds());
        String recentDeploymentStatuses = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentStatuses());
        String recentDeploymentProviders = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentProviders());
        String recentDeploymentClientTypes = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentClientTypes());
        String recentDeploymentHosts = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentHosts());
        String recentDeploymentPorts = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentPorts());
        String recentDeploymentPackageTypes = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentPackageTypes());
        String recentDeploymentTargetDirectories = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentTargetDirectories());
        String recentDeploymentRemotePackageDirectories = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentRemotePackageDirectories());
        String recentDeploymentRemoteBaseDirectories = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentRemoteBaseDirectories());
        String recentDeploymentDryRuns = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentDryRuns());
        String recentDeploymentDeployedFlags = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentDeployedFlags());
        String recentDeploymentAgentCounts = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentAgentCounts());
        String recentDeploymentBundledFileCounts = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentBundledFileCounts());
        String recentDeploymentCommandCounts = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentCommandCounts());
        String recentDeploymentExecutedAts = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentExecutedAts());
        String recentDeploymentGeneratedAts = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentGeneratedAts());
        String recentDeploymentCreatedAts = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentCreatedAts());
        String recentDeploymentAts = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentAts());
        String recentDeploymentMessages = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentMessages());
        String recentDeploymentErrorMessages = PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentErrorMessages());
        String recentDeploymentAgentIds = PlatformDeploymentSummaryFormatter.formatGroupedStringLists(health.recentDeploymentAgentIds());
        String recentDeploymentBundledFiles = PlatformDeploymentSummaryFormatter.formatGroupedStringLists(health.recentDeploymentBundledFiles());
        String recentDeploymentCommands = PlatformDeploymentSummaryFormatter.formatGroupedStringLists(health.recentDeploymentCommands());
        String recentDeployments = PlatformDeploymentSummaryFormatter.formatRecentDeployments(health.recentDeployments());
        String latestDeploymentAgentIds = PlatformDeploymentSummaryFormatter.formatFlatList(health.latestDeploymentAgentIds());
        String latestDeploymentBundledFiles = PlatformDeploymentSummaryFormatter.formatFlatList(health.latestDeploymentBundledFiles());
        String latestDeploymentCommands = PlatformDeploymentSummaryFormatter.formatFlatList(health.latestDeploymentCommands());
        String latestDeploymentGeneratedAt = health.latestDeploymentGeneratedAt() == null ? "N/A" : String.valueOf(health.latestDeploymentGeneratedAt());
        String latestDeploymentCreatedAt = health.latestDeploymentCreatedAt() == null ? "N/A" : String.valueOf(health.latestDeploymentCreatedAt());
        String latestDeploymentAt = health.latestDeploymentAt() == null ? "N/A" : health.latestDeploymentAt().toString();
        String latestDeploymentMessage = health.latestDeploymentMessage() == null ? "N/A" : health.latestDeploymentMessage();
        String latestDeploymentErrorMessage = health.latestDeploymentErrorMessage() == null ? "N/A" : health.latestDeploymentErrorMessage();
        String latestDeploymentHost = health.latestDeploymentHost() == null ? "N/A" : health.latestDeploymentHost();
        String latestDeploymentPort = health.latestDeploymentPort() == null ? "N/A" : String.valueOf(health.latestDeploymentPort());
        String latestDeploymentProvider = health.latestDeploymentProvider() == null ? "N/A" : health.latestDeploymentProvider();
        String latestDeploymentClientType = health.latestDeploymentClientType() == null ? "N/A" : health.latestDeploymentClientType();
        String latestDeploymentPackageType = health.latestDeploymentPackageType() == null ? "N/A" : health.latestDeploymentPackageType();
        String latestDeploymentTargetDirectory = health.latestDeploymentTargetDirectory() == null ? "N/A" : health.latestDeploymentTargetDirectory();
        String latestDeploymentRemotePackageDirectory = health.latestDeploymentRemotePackageDirectory() == null ? "N/A" : health.latestDeploymentRemotePackageDirectory();
        String latestDeploymentRemoteBaseDirectory = health.latestDeploymentRemoteBaseDirectory() == null ? "N/A" : health.latestDeploymentRemoteBaseDirectory();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Platform Health</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe6; color: #1f2933; }
                    .page { max-width: 1100px; margin: 0 auto; padding: 28px; }
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
                      <h1>Platform Health</h1>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Healthy</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Deployments</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Pending</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Reports Ready</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Health Notes</h2>
                      <ul>
                        <li>%s</li>
                        <li>Customers with delivery package: %s</li>
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
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(health.generatedAt().toString()),
                escapeHtml(health.healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(String.valueOf(health.totalCustomers())),
                escapeHtml(String.valueOf(health.healthyCustomers())),
                escapeHtml(String.valueOf(health.totalDeployments())),
                escapeHtml(String.valueOf(health.failedDeployments())),
                escapeHtml(String.valueOf(health.pendingDeployments())),
                escapeHtml(String.valueOf(health.customersWithReports())),
                escapeHtml(health.statusMessage()),
                escapeHtml(String.valueOf(health.customersWithDeliveryPackage())),
                escapeHtml(String.valueOf(health.customersWithArtifactCatalog())),
                escapeHtml(recentDeploymentJobIds),
                escapeHtml(recentDeploymentPackageIds),
                escapeHtml(recentDeploymentStatuses),
                escapeHtml(recentDeploymentProviders),
                escapeHtml(recentDeploymentClientTypes),
                escapeHtml(recentDeploymentHosts),
                escapeHtml(recentDeploymentPorts),
                escapeHtml(recentDeploymentPackageTypes),
                escapeHtml(recentDeploymentTargetDirectories),
                escapeHtml(recentDeploymentRemotePackageDirectories),
                escapeHtml(recentDeploymentRemoteBaseDirectories),
                escapeHtml(recentDeploymentDryRuns),
                escapeHtml(recentDeploymentDeployedFlags),
                escapeHtml(recentDeploymentAgentCounts),
                escapeHtml(recentDeploymentBundledFileCounts),
                escapeHtml(recentDeploymentCommandCounts),
                escapeHtml(recentDeploymentExecutedAts),
                escapeHtml(recentDeploymentGeneratedAts),
                escapeHtml(recentDeploymentCreatedAts),
                escapeHtml(recentDeploymentAts),
                escapeHtml(recentDeploymentMessages),
                escapeHtml(recentDeploymentErrorMessages),
                escapeHtml(recentDeploymentAgentIds),
                escapeHtml(recentDeploymentBundledFiles),
                escapeHtml(recentDeploymentCommands),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentDeployments(health.recentDeployments())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestSummary(health.latestDeploymentSummary())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestDetail(health.latestDeploymentDetail())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatOverview(health.deploymentOverview())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatSnapshot(health.deploymentSnapshot())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentHistory(health.recentDeploymentHistory())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatStatusCounts(health.deploymentStatusCounts())),
                escapeHtml(latestDeployment),
                escapeHtml(health.latestDeploymentPackageId() == null ? "N/A" : health.latestDeploymentPackageId()),
                escapeHtml(health.latestDeploymentDryRun() == null ? "N/A" : String.valueOf(health.latestDeploymentDryRun())),
                escapeHtml(health.latestDeploymentDeployed() == null ? "N/A" : String.valueOf(health.latestDeploymentDeployed())),
                escapeHtml(health.latestDeploymentAgentCount() == null ? "N/A" : String.valueOf(health.latestDeploymentAgentCount())),
                escapeHtml(latestDeploymentAgentIds),
                escapeHtml(latestDeploymentBundledFiles),
                escapeHtml(health.latestDeploymentBundledFileCount() == null ? "N/A" : String.valueOf(health.latestDeploymentBundledFileCount())),
                escapeHtml(latestDeploymentCommands),
                escapeHtml(health.latestDeploymentCommandCount() == null ? "N/A" : String.valueOf(health.latestDeploymentCommandCount())),
                escapeHtml(health.latestDeploymentExecutedAt() == null ? "N/A" : String.valueOf(health.latestDeploymentExecutedAt())),
                escapeHtml(latestDeploymentGeneratedAt),
                escapeHtml(latestDeploymentCreatedAt),
                escapeHtml(latestDeploymentAt),
                escapeHtml(latestDeploymentMessage),
                escapeHtml(latestDeploymentErrorMessage),
                escapeHtml(latestDeploymentHost),
                escapeHtml(latestDeploymentPort),
                escapeHtml(latestDeploymentProvider),
                escapeHtml(latestDeploymentClientType),
                escapeHtml(latestDeploymentPackageType),
                escapeHtml(latestDeploymentTargetDirectory),
                escapeHtml(latestDeploymentRemotePackageDirectory),
                escapeHtml(latestDeploymentRemoteBaseDirectory)
        );
    }

    private String buildReadme(PlatformHealthResponse health) {
        return """
                Platform health
                ===============

                Generated at: %s
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
                - platform-health.json
                - platform-health.html
                - README.txt
                """.formatted(
                health.generatedAt(),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentJobIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentPackageIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentStatuses()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentProviders()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentClientTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentHosts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentPorts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentPackageTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentTargetDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentRemotePackageDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentRemoteBaseDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentDryRuns()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentDeployedFlags()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentAgentCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentBundledFileCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentCommandCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentExecutedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentGeneratedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentCreatedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentMessages()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.recentDeploymentErrorMessages()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(health.recentDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(health.recentDeploymentBundledFiles()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(health.recentDeploymentCommands()),
                PlatformDeploymentSummaryFormatter.formatRecentDeployments(health.recentDeployments()),
                PlatformDeploymentSummaryFormatter.formatLatestSummary(health.latestDeploymentSummary()),
                PlatformDeploymentSummaryFormatter.formatLatestDetail(health.latestDeploymentDetail()),
                PlatformDeploymentSummaryFormatter.formatOverview(health.deploymentOverview()),
                PlatformDeploymentSummaryFormatter.formatSnapshot(health.deploymentSnapshot()),
                PlatformDeploymentSummaryFormatter.formatRecentHistory(health.recentDeploymentHistory()),
                PlatformDeploymentSummaryFormatter.formatStatusCounts(health.deploymentStatusCounts()),
                health.latestDeployment() == null ? "None" : health.latestDeployment().deploymentJobId(),
                health.latestDeploymentPackageId() == null ? "N/A" : health.latestDeploymentPackageId(),
                health.latestDeploymentDryRun() == null ? "N/A" : health.latestDeploymentDryRun(),
                health.latestDeploymentDeployed() == null ? "N/A" : health.latestDeploymentDeployed(),
                health.latestDeploymentAgentCount() == null ? "N/A" : health.latestDeploymentAgentCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.latestDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.latestDeploymentBundledFiles()),
                health.latestDeploymentBundledFileCount() == null ? "N/A" : health.latestDeploymentBundledFileCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(health.latestDeploymentCommands()),
                health.latestDeploymentCommandCount() == null ? "N/A" : health.latestDeploymentCommandCount(),
                health.latestDeploymentExecutedAt() == null ? "N/A" : health.latestDeploymentExecutedAt(),
                health.latestDeploymentGeneratedAt() == null ? "N/A" : health.latestDeploymentGeneratedAt(),
                health.latestDeploymentCreatedAt() == null ? "N/A" : health.latestDeploymentCreatedAt(),
                health.latestDeploymentAt() == null ? "N/A" : health.latestDeploymentAt(),
                health.latestDeploymentMessage() == null ? "N/A" : health.latestDeploymentMessage(),
                health.latestDeploymentErrorMessage() == null ? "N/A" : health.latestDeploymentErrorMessage(),
                health.latestDeploymentHost() == null ? "N/A" : health.latestDeploymentHost(),
                health.latestDeploymentPort() == null ? "N/A" : health.latestDeploymentPort(),
                health.latestDeploymentProvider() == null ? "N/A" : health.latestDeploymentProvider(),
                health.latestDeploymentClientType() == null ? "N/A" : health.latestDeploymentClientType(),
                health.latestDeploymentPackageType() == null ? "N/A" : health.latestDeploymentPackageType(),
                health.latestDeploymentTargetDirectory() == null ? "N/A" : health.latestDeploymentTargetDirectory(),
                health.latestDeploymentRemotePackageDirectory() == null ? "N/A" : health.latestDeploymentRemotePackageDirectory(),
                health.latestDeploymentRemoteBaseDirectory() == null ? "N/A" : health.latestDeploymentRemoteBaseDirectory()
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
            throw new IllegalStateException("Failed to serialize platform health", e);
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
