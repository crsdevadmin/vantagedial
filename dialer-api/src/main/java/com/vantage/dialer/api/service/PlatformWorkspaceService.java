package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerPortfolioResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceExportResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceResponse;
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
public class PlatformWorkspaceService {

    private final PlatformControlCenterService platformControlCenterService;
    private final CustomerPortfolioService customerPortfolioService;
    private final TelephonyDeploymentAuditService deploymentAuditService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public PlatformWorkspaceService(PlatformControlCenterService platformControlCenterService,
                                    CustomerPortfolioService customerPortfolioService,
                                    TelephonyDeploymentAuditService deploymentAuditService,
                                    ObjectMapper objectMapper,
                                    @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.platformControlCenterService = platformControlCenterService;
        this.customerPortfolioService = customerPortfolioService;
        this.deploymentAuditService = deploymentAuditService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("platform-workspace");
    }

    public PlatformWorkspaceResponse workspace() {
        PlatformControlCenterResponse controlCenter = platformControlCenterService.controlCenter();
        CustomerPortfolioResponse customerPortfolio = customerPortfolioService.portfolio();
        List<TelephonyDeploymentAuditResponse> recentDeployments = deploymentAuditService.listDeploymentAudits(null).stream()
                .limit(10)
                .toList();
        return new PlatformWorkspaceResponse(
                Instant.now(),
                controlCenter,
                customerPortfolio,
                controlCenter.healthyCustomers(),
                controlCenter.customersWithReports(),
                controlCenter.customersWithArtifactCatalog(),
                controlCenter.recentDeploymentJobIds(),
                controlCenter.recentDeploymentPackageIds(),
                controlCenter.recentDeploymentStatuses(),
                controlCenter.recentDeploymentProviders(),
                controlCenter.recentDeploymentClientTypes(),
                controlCenter.recentDeploymentHosts(),
                controlCenter.recentDeploymentPorts(),
                controlCenter.recentDeploymentPackageTypes(),
                controlCenter.recentDeploymentTargetDirectories(),
                controlCenter.recentDeploymentRemotePackageDirectories(),
                controlCenter.recentDeploymentRemoteBaseDirectories(),
                controlCenter.recentDeploymentDryRuns(),
                controlCenter.recentDeploymentDeployedFlags(),
                controlCenter.recentDeploymentAgentCounts(),
                controlCenter.recentDeploymentBundledFileCounts(),
                controlCenter.recentDeploymentCommandCounts(),
                controlCenter.recentDeploymentExecutedAts(),
                controlCenter.recentDeploymentGeneratedAts(),
                controlCenter.recentDeploymentCreatedAts(),
                controlCenter.recentDeploymentAts(),
                controlCenter.recentDeploymentMessages(),
                controlCenter.recentDeploymentErrorMessages(),
                controlCenter.recentDeploymentAgentIds(),
                controlCenter.recentDeploymentBundledFiles(),
                controlCenter.recentDeploymentCommands(),
                controlCenter.recentDeployments(),
                controlCenter.latestDeploymentSummary(),
                controlCenter.deploymentSnapshot(),
                controlCenter.recentDeploymentHistory(),
                controlCenter.deploymentStatusCounts(),
                controlCenter.latestDeploymentDetail(),
                controlCenter.deploymentOverview(),
                controlCenter.healthy(),
                controlCenter.statusMessage(),
                controlCenter.latestDeploymentJobId(),
                controlCenter.latestDeploymentPackageId(),
                controlCenter.latestDeploymentDryRun(),
                controlCenter.latestDeploymentDeployed(),
                controlCenter.latestDeploymentAgentCount(),
                controlCenter.latestDeploymentAgentIds(),
                controlCenter.latestDeploymentBundledFiles(),
                controlCenter.latestDeploymentBundledFileCount(),
                controlCenter.latestDeploymentCommands(),
                controlCenter.latestDeploymentCommandCount(),
                controlCenter.latestDeploymentExecutedAt(),
                controlCenter.latestDeploymentGeneratedAt(),
                controlCenter.latestDeploymentCreatedAt(),
                controlCenter.latestDeploymentStatus(),
                controlCenter.latestDeploymentAt(),
                controlCenter.latestDeploymentMessage(),
                controlCenter.latestDeploymentErrorMessage(),
                controlCenter.latestDeploymentHost(),
                controlCenter.latestDeploymentPort(),
                controlCenter.latestDeploymentProvider(),
                controlCenter.latestDeploymentClientType(),
                controlCenter.latestDeploymentPackageType(),
                controlCenter.latestDeploymentTargetDirectory(),
                controlCenter.latestDeploymentRemotePackageDirectory(),
                controlCenter.latestDeploymentRemoteBaseDirectory(),
                recentDeployments
        );
    }

    public PlatformWorkspaceBundleResponse generateBundle() {
        PlatformWorkspaceResponse workspace = workspace();
        Instant generatedAt = workspace.generatedAt();
        try {
            Path bundleDirectory = exportRoot.resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "platform-workspace.json", json(workspace)));
            files.add(write(bundleDirectory, "platform-workspace.html", buildHtml(workspace)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(workspace)));

            return new PlatformWorkspaceBundleResponse(
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-workspace.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-workspace.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate platform workspace bundle", e);
        }
    }

    public PlatformWorkspaceExportResponse export() {
        PlatformWorkspaceResponse workspace = workspace();
        Instant generatedAt = workspace.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve("export");
            Files.createDirectories(exportDirectory);

            Path workspaceJsonPath = exportDirectory.resolve("platform-workspace.json");
            Path workspaceHtmlPath = exportDirectory.resolve("platform-workspace.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(workspaceJsonPath, json(workspace));
            Files.writeString(workspaceHtmlPath, buildHtml(workspace));
            Files.writeString(readmePath, buildExportReadme(workspace));

            return new PlatformWorkspaceExportResponse(
                    exportDirectory.toAbsolutePath().toString(),
                    workspaceJsonPath.toAbsolutePath().toString(),
                    workspaceHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export platform workspace", e);
        }
    }

    private String buildHtml(PlatformWorkspaceResponse workspace) {
        String latestDeployment = workspace.latestDeploymentJobId() == null ? "None" : workspace.latestDeploymentJobId();
        String recentDeploymentJobIds = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentJobIds());
        String recentDeploymentPackageIds = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentPackageIds());
        String recentDeploymentStatuses = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentStatuses());
        String recentDeploymentProviders = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentProviders());
        String recentDeploymentClientTypes = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentClientTypes());
        String recentDeploymentHosts = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentHosts());
        String recentDeploymentPorts = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentPorts());
        String recentDeploymentPackageTypes = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentPackageTypes());
        String recentDeploymentTargetDirectories = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentTargetDirectories());
        String recentDeploymentRemotePackageDirectories = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentRemotePackageDirectories());
        String recentDeploymentRemoteBaseDirectories = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentRemoteBaseDirectories());
        String recentDeploymentDryRuns = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentDryRuns());
        String recentDeploymentDeployedFlags = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentDeployedFlags());
        String recentDeploymentAgentCounts = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentAgentCounts());
        String recentDeploymentBundledFileCounts = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentBundledFileCounts());
        String recentDeploymentCommandCounts = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentCommandCounts());
        String recentDeploymentExecutedAts = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentExecutedAts());
        String recentDeploymentGeneratedAts = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentGeneratedAts());
        String recentDeploymentCreatedAts = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentCreatedAts());
        String recentDeploymentAts = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentAts());
        String recentDeploymentMessages = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentMessages());
        String recentDeploymentErrorMessages = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentErrorMessages());
        String recentDeploymentAgentIds = PlatformDeploymentSummaryFormatter.formatGroupedStringLists(workspace.recentDeploymentAgentIds());
        String recentDeploymentBundledFiles = PlatformDeploymentSummaryFormatter.formatGroupedStringLists(workspace.recentDeploymentBundledFiles());
        String recentDeploymentCommands = PlatformDeploymentSummaryFormatter.formatGroupedStringLists(workspace.recentDeploymentCommands());
        String recentDeployments = PlatformDeploymentSummaryFormatter.formatRecentDeployments(workspace.recentDeployments());
        String latestDeploymentPackageId = workspace.latestDeploymentPackageId() == null ? "N/A" : workspace.latestDeploymentPackageId();
        String latestDeploymentDryRun = workspace.latestDeploymentDryRun() == null ? "N/A" : String.valueOf(workspace.latestDeploymentDryRun());
        String latestDeploymentDeployed = workspace.latestDeploymentDeployed() == null ? "N/A" : String.valueOf(workspace.latestDeploymentDeployed());
        String latestDeploymentAgentCount = workspace.latestDeploymentAgentCount() == null ? "N/A" : String.valueOf(workspace.latestDeploymentAgentCount());
        String latestDeploymentAgentIds = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.latestDeploymentAgentIds());
        String latestDeploymentBundledFiles = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.latestDeploymentBundledFiles());
        String latestDeploymentCommands = PlatformDeploymentSummaryFormatter.formatFlatList(workspace.latestDeploymentCommands());
        String latestDeploymentBundledFileCount = workspace.latestDeploymentBundledFileCount() == null ? "N/A" : String.valueOf(workspace.latestDeploymentBundledFileCount());
        String latestDeploymentCommandCount = workspace.latestDeploymentCommandCount() == null ? "N/A" : String.valueOf(workspace.latestDeploymentCommandCount());
        String latestDeploymentExecutedAt = workspace.latestDeploymentExecutedAt() == null ? "N/A" : String.valueOf(workspace.latestDeploymentExecutedAt());
        String latestDeploymentGeneratedAt = workspace.latestDeploymentGeneratedAt() == null ? "N/A" : String.valueOf(workspace.latestDeploymentGeneratedAt());
        String latestDeploymentCreatedAt = workspace.latestDeploymentCreatedAt() == null ? "N/A" : String.valueOf(workspace.latestDeploymentCreatedAt());
        String latestDeploymentStatus = workspace.latestDeploymentStatus() == null ? "N/A" : workspace.latestDeploymentStatus();
        String latestDeploymentAt = workspace.latestDeploymentAt() == null ? "N/A" : workspace.latestDeploymentAt().toString();
        String latestDeploymentMessage = workspace.latestDeploymentMessage() == null ? "N/A" : workspace.latestDeploymentMessage();
        String latestDeploymentErrorMessage = workspace.latestDeploymentErrorMessage() == null ? "N/A" : workspace.latestDeploymentErrorMessage();
        String latestDeploymentHost = workspace.latestDeploymentHost() == null ? "N/A" : workspace.latestDeploymentHost();
        String latestDeploymentPort = workspace.latestDeploymentPort() == null ? "N/A" : String.valueOf(workspace.latestDeploymentPort());
        String latestDeploymentProvider = workspace.latestDeploymentProvider() == null ? "N/A" : workspace.latestDeploymentProvider();
        String latestDeploymentClientType = workspace.latestDeploymentClientType() == null ? "N/A" : workspace.latestDeploymentClientType();
        String latestDeploymentPackageType = workspace.latestDeploymentPackageType() == null ? "N/A" : workspace.latestDeploymentPackageType();
        String latestDeploymentTargetDirectory = workspace.latestDeploymentTargetDirectory() == null ? "N/A" : workspace.latestDeploymentTargetDirectory();
        String latestDeploymentRemotePackageDirectory = workspace.latestDeploymentRemotePackageDirectory() == null ? "N/A" : workspace.latestDeploymentRemotePackageDirectory();
        String latestDeploymentRemoteBaseDirectory = workspace.latestDeploymentRemoteBaseDirectory() == null ? "N/A" : workspace.latestDeploymentRemoteBaseDirectory();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Platform Workspace</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe7; color: #1f2933; }
                    .page { max-width: 1180px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #dcccbb; border-radius: 24px; padding: 24px; box-shadow: 0 14px 28px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff8f0; border: 1px solid #ead9c8; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #786a5d; }
                    .value { font-size: 26px; font-weight: 700; color: #8b4c1f; margin-top: 8px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Platform Workspace</h1>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Healthy</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Deployments</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Recent Deployments</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Latest Deployment</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Reports Ready</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Workspace Summary</h2>
                      <ul>
                        <li>Customers with delivery package: %s</li>
                        <li>Customers with artifact catalog: %s</li>
                        <li>Failed deployments: %s</li>
                        <li>Recent deployment list size: %s</li>
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
                        <li>Latest deployment status: %s</li>
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
                escapeHtml(workspace.generatedAt().toString()),
                escapeHtml(workspace.statusMessage()),
                escapeHtml(String.valueOf(workspace.customerPortfolio().totalCustomers())),
                escapeHtml(String.valueOf(workspace.healthyCustomers())),
                escapeHtml(String.valueOf(workspace.controlCenter().deploymentAuditSummary().totalDeployments())),
                escapeHtml(String.valueOf(workspace.recentDeployments().size())),
                escapeHtml(latestDeployment),
                escapeHtml(String.valueOf(workspace.customersWithReports())),
                escapeHtml(String.valueOf(workspace.controlCenter().customerCommandCenter().customersWithDeliveryPackage())),
                escapeHtml(String.valueOf(workspace.customersWithArtifactCatalog())),
                escapeHtml(String.valueOf(workspace.controlCenter().deploymentAuditSummary().failedDeployments())),
                escapeHtml(String.valueOf(workspace.recentDeployments().size())),
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
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentDeployments(workspace.recentDeployments())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestSummary(workspace.latestDeploymentSummary())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatLatestDetail(workspace.latestDeploymentDetail())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatOverview(workspace.deploymentOverview())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatSnapshot(workspace.deploymentSnapshot())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatRecentHistory(workspace.recentDeploymentHistory())),
                escapeHtml(PlatformDeploymentSummaryFormatter.formatStatusCounts(workspace.deploymentStatusCounts())),
                escapeHtml(latestDeploymentStatus),
                escapeHtml(latestDeploymentPackageId),
                escapeHtml(latestDeploymentDryRun),
                escapeHtml(latestDeploymentDeployed),
                escapeHtml(latestDeploymentAgentCount),
                escapeHtml(latestDeploymentAgentIds),
                escapeHtml(latestDeploymentBundledFiles),
                escapeHtml(latestDeploymentBundledFileCount),
                escapeHtml(latestDeploymentCommands),
                escapeHtml(latestDeploymentCommandCount),
                escapeHtml(latestDeploymentExecutedAt),
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

    private String buildReadme(PlatformWorkspaceResponse workspace) {
        return """
                Platform workspace bundle
                ========================

                Generated at: %s
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
                - platform-workspace.json
                - platform-workspace.html
                - README.txt
                """.formatted(
                workspace.generatedAt(),
                workspace.healthy(),
                workspace.statusMessage(),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentJobIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentPackageIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentStatuses()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentProviders()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentClientTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentHosts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentPorts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentPackageTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentTargetDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentRemotePackageDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentRemoteBaseDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentDryRuns()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentDeployedFlags()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentAgentCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentBundledFileCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentCommandCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentExecutedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentGeneratedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentCreatedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentMessages()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.recentDeploymentErrorMessages()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(workspace.recentDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(workspace.recentDeploymentBundledFiles()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(workspace.recentDeploymentCommands()),
                PlatformDeploymentSummaryFormatter.formatRecentDeployments(workspace.recentDeployments()),
                PlatformDeploymentSummaryFormatter.formatLatestSummary(workspace.latestDeploymentSummary()),
                PlatformDeploymentSummaryFormatter.formatLatestDetail(workspace.latestDeploymentDetail()),
                PlatformDeploymentSummaryFormatter.formatOverview(workspace.deploymentOverview()),
                PlatformDeploymentSummaryFormatter.formatSnapshot(workspace.deploymentSnapshot()),
                PlatformDeploymentSummaryFormatter.formatRecentHistory(workspace.recentDeploymentHistory()),
                PlatformDeploymentSummaryFormatter.formatStatusCounts(workspace.deploymentStatusCounts()),
                workspace.latestDeploymentJobId() == null ? "None" : workspace.latestDeploymentJobId(),
                workspace.latestDeploymentPackageId() == null ? "N/A" : workspace.latestDeploymentPackageId(),
                workspace.latestDeploymentDryRun() == null ? "N/A" : workspace.latestDeploymentDryRun(),
                workspace.latestDeploymentDeployed() == null ? "N/A" : workspace.latestDeploymentDeployed(),
                workspace.latestDeploymentAgentCount() == null ? "N/A" : workspace.latestDeploymentAgentCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.latestDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.latestDeploymentBundledFiles()),
                workspace.latestDeploymentBundledFileCount() == null ? "N/A" : workspace.latestDeploymentBundledFileCount(),
                PlatformDeploymentSummaryFormatter.formatFlatList(workspace.latestDeploymentCommands()),
                workspace.latestDeploymentCommandCount() == null ? "N/A" : workspace.latestDeploymentCommandCount(),
                workspace.latestDeploymentExecutedAt() == null ? "N/A" : workspace.latestDeploymentExecutedAt(),
                workspace.latestDeploymentGeneratedAt() == null ? "N/A" : workspace.latestDeploymentGeneratedAt(),
                workspace.latestDeploymentCreatedAt() == null ? "N/A" : workspace.latestDeploymentCreatedAt(),
                workspace.latestDeploymentStatus() == null ? "N/A" : workspace.latestDeploymentStatus(),
                workspace.latestDeploymentAt() == null ? "N/A" : workspace.latestDeploymentAt(),
                workspace.latestDeploymentMessage() == null ? "N/A" : workspace.latestDeploymentMessage(),
                workspace.latestDeploymentErrorMessage() == null ? "N/A" : workspace.latestDeploymentErrorMessage(),
                workspace.latestDeploymentHost() == null ? "N/A" : workspace.latestDeploymentHost(),
                workspace.latestDeploymentPort() == null ? "N/A" : workspace.latestDeploymentPort(),
                workspace.latestDeploymentProvider() == null ? "N/A" : workspace.latestDeploymentProvider(),
                workspace.latestDeploymentClientType() == null ? "N/A" : workspace.latestDeploymentClientType(),
                workspace.latestDeploymentPackageType() == null ? "N/A" : workspace.latestDeploymentPackageType(),
                workspace.latestDeploymentTargetDirectory() == null ? "N/A" : workspace.latestDeploymentTargetDirectory(),
                workspace.latestDeploymentRemotePackageDirectory() == null ? "N/A" : workspace.latestDeploymentRemotePackageDirectory(),
                workspace.latestDeploymentRemoteBaseDirectory() == null ? "N/A" : workspace.latestDeploymentRemoteBaseDirectory()
        );
    }

    private String buildExportReadme(PlatformWorkspaceResponse workspace) {
        return """
                Platform workspace export
                ========================

                Generated at: %s
                Status: %s

                Files:
                - platform-workspace.json
                - platform-workspace.html
                - README.txt
                """.formatted(
                workspace.generatedAt(),
                workspace.statusMessage()
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
            throw new IllegalStateException("Failed to serialize platform workspace", e);
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
