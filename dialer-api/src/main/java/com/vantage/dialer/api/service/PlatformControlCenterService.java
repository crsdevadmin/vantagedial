package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerCommandCenterResponse;
import com.vantage.dialer.api.dto.DeploymentAuditSummaryResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterBundleResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterExportResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterResponse;
import com.vantage.dialer.api.dto.PlatformDeploymentOverviewResponse;
import com.vantage.dialer.api.dto.PlatformLatestDeploymentDetailResponse;
import com.vantage.dialer.api.dto.PlatformRecentDeploymentHistoryResponse;
import com.vantage.dialer.api.dto.PlatformDeploymentSnapshotResponse;
import com.vantage.dialer.api.dto.PlatformDeploymentStatusCountsResponse;
import com.vantage.dialer.api.dto.RecentDeploymentSummaryResponse;
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
public class PlatformControlCenterService {
    private static final PlatformLatestDeploymentDetailResponse EMPTY_LATEST_DEPLOYMENT_DETAIL =
            new PlatformLatestDeploymentDetailResponse(
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null
            );

    private final CustomerCommandCenterService customerCommandCenterService;
    private final TelephonyDeploymentAuditService deploymentAuditService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public PlatformControlCenterService(CustomerCommandCenterService customerCommandCenterService,
                                        TelephonyDeploymentAuditService deploymentAuditService,
                                        ObjectMapper objectMapper,
                                        @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.customerCommandCenterService = customerCommandCenterService;
        this.deploymentAuditService = deploymentAuditService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("platform-control-center");
    }

    public PlatformControlCenterResponse controlCenter() {
        return buildControlCenterResponse(buildControlCenterResponseSections());
    }

    public PlatformControlCenterBundleResponse generateBundle() {
        PlatformControlCenterResponse controlCenter = controlCenter();
        Instant generatedAt = controlCenter.generatedAt();
        try {
            Path bundleDirectory = exportRoot.resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "platform-control-center.json", json(controlCenter)));
            files.add(write(bundleDirectory, "platform-control-center.html", buildHtml(controlCenter)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(controlCenter)));

            return new PlatformControlCenterBundleResponse(
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-control-center.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("platform-control-center.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate platform control center bundle", e);
        }
    }

    public PlatformControlCenterExportResponse export() {
        PlatformControlCenterResponse controlCenter = controlCenter();
        Instant generatedAt = controlCenter.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve("export");
            Files.createDirectories(exportDirectory);

            Path controlCenterJsonPath = exportDirectory.resolve("platform-control-center.json");
            Path controlCenterHtmlPath = exportDirectory.resolve("platform-control-center.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(controlCenterJsonPath, json(controlCenter));
            Files.writeString(controlCenterHtmlPath, buildHtml(controlCenter));
            Files.writeString(readmePath, buildExportReadme(controlCenter));

            return new PlatformControlCenterExportResponse(
                    exportDirectory.toAbsolutePath().toString(),
                    controlCenterJsonPath.toAbsolutePath().toString(),
                    controlCenterHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export platform control center", e);
        }
    }

    private int countByStatus(List<TelephonyDeploymentAuditResponse> deployments, String status) {
        return (int) deployments.stream().filter(deployment -> status.equalsIgnoreCase(deployment.status())).count();
    }

    private DeploymentAuditSummaryResponse buildDeploymentAuditSummary(List<TelephonyDeploymentAuditResponse> deployments,
                                                                      TelephonyDeploymentAuditResponse latestDeployment) {
        return new DeploymentAuditSummaryResponse(
                deployments.size(),
                countByStatus(deployments, "PENDING"),
                countByStatus(deployments, "DRY_RUN"),
                countByStatus(deployments, "DEPLOYED"),
                countByStatus(deployments, "FAILED"),
                latestDeployment
        );
    }

    private ControlCenterResponseSections buildControlCenterResponseSections() {
        CustomerCommandCenterResponse customerCommandCenter = customerCommandCenterService.commandCenter();
        List<TelephonyDeploymentAuditResponse> deployments = deploymentAuditService.listDeploymentAudits(null);
        List<TelephonyDeploymentAuditResponse> recentDeploymentAudits = deployments.stream()
                .limit(10)
                .toList();
        TelephonyDeploymentAuditResponse latestDeployment = deployments.isEmpty() ? null : deployments.get(0);
        DeploymentAuditSummaryResponse deploymentAuditSummary = buildDeploymentAuditSummary(deployments, latestDeployment);
        boolean healthy = deploymentAuditSummary.failedDeployments() == 0
                && deploymentAuditSummary.pendingDeployments() == 0
                && customerCommandCenter.healthyCustomers() == customerCommandCenter.totalCustomers()
                && customerCommandCenter.totalCustomers() > 0;
        String statusMessage;
        if (customerCommandCenter.totalCustomers() == 0) {
            statusMessage = "No customers provisioned yet";
        } else if (deploymentAuditSummary.failedDeployments() > 0) {
            statusMessage = "Deployment failures require attention";
        } else if (deploymentAuditSummary.pendingDeployments() > 0) {
            statusMessage = "Deployments are still in progress";
        } else if (customerCommandCenter.healthyCustomers() < customerCommandCenter.totalCustomers()) {
            statusMessage = "Some customers still need attention";
        } else {
            statusMessage = "Platform control center is healthy";
        }
        PlatformRecentDeploymentHistoryResponse recentDeploymentHistory = buildRecentDeploymentHistory(recentDeploymentAudits);
        DeploymentResponseProjection deploymentResponseProjection = buildDeploymentResponseProjection(
                latestDeployment,
                recentDeploymentHistory,
                deploymentAuditSummary
        );
        return new ControlCenterResponseSections(
                customerCommandCenter,
                deploymentResponseProjection,
                Instant.now(),
                healthy,
                statusMessage,
                deploymentAuditSummary
        );
    }

    private PlatformRecentDeploymentHistoryResponse buildRecentDeploymentHistory(
            List<TelephonyDeploymentAuditResponse> recentDeploymentAudits) {
        RecentDeploymentAccumulator accumulator = new RecentDeploymentAccumulator();
        for (TelephonyDeploymentAuditResponse deployment : recentDeploymentAudits) {
            accumulator.add(deployment);
        }
        return accumulator.toRecentDeploymentHistory();
    }

    private PlatformDeploymentSnapshotResponse buildDeploymentSnapshot(RecentDeploymentSummaryResponse latestDeploymentSummary,
                                                                      PlatformRecentDeploymentHistoryResponse recentDeploymentHistory,
                                                                      PlatformLatestDeploymentDetailResponse latestDeploymentDetail) {
        return new PlatformDeploymentSnapshotResponse(
                latestDeploymentSummary,
                recentDeploymentHistory.recentDeployments(),
                recentDeploymentHistory.recentDeploymentCount(),
                latestDeploymentDetail == null ? null : latestDeploymentDetail.deploymentAt(),
                latestDeploymentDetail == null ? null : latestDeploymentDetail.provider(),
                recentDeploymentHistory.recentDeploymentProviders(),
                latestDeploymentDetail == null ? null : latestDeploymentDetail.status()
        );
    }

    private PlatformDeploymentStatusCountsResponse buildDeploymentStatusCounts(DeploymentAuditSummaryResponse deploymentAuditSummary) {
        return new PlatformDeploymentStatusCountsResponse(
                deploymentAuditSummary.totalDeployments(),
                deploymentAuditSummary.pendingDeployments(),
                deploymentAuditSummary.dryRunDeployments(),
                deploymentAuditSummary.successfulDeployments(),
                deploymentAuditSummary.failedDeployments()
        );
    }

    private PlatformDeploymentOverviewResponse buildDeploymentOverview(
            RecentDeploymentSummaryResponse latestDeploymentSummary,
            PlatformLatestDeploymentDetailResponse latestDeploymentDetail,
            PlatformDeploymentSnapshotResponse deploymentSnapshot,
            PlatformRecentDeploymentHistoryResponse recentDeploymentHistory,
            PlatformDeploymentStatusCountsResponse deploymentStatusCounts) {
        PlatformLatestDeploymentDetailResponse latest = latestDeploymentDetail;
        PlatformRecentDeploymentHistoryResponse recent = recentDeploymentHistory;
        return new PlatformDeploymentOverviewResponse(
                latestDeploymentSummary,
                latest.deploymentJobId(),
                latest.packageId(),
                latest.provider(),
                latest.clientType(),
                latest.packageType(),
                latest.dryRun(),
                latest.deployed(),
                latest.host(),
                latest.port(),
                latest.targetDirectory(),
                latest.remotePackageDirectory(),
                latest.remoteBaseDirectory(),
                latest.agentCount(),
                latest.agentIds(),
                latest.bundledFileCount(),
                latest.bundledFiles(),
                latest.commandCount(),
                latest.commands(),
                latest.executedAt(),
                latest.generatedAt(),
                latest.createdAt(),
                latest.message(),
                latest.errorMessage(),
                recent.recentDeploymentJobIds(),
                recent.recentDeploymentPackageIds(),
                recent.recentDeploymentStatuses(),
                recent.recentDeploymentClientTypes(),
                recent.recentDeploymentHosts(),
                recent.recentDeploymentPorts(),
                recent.recentDeploymentPackageTypes(),
                recent.recentDeploymentTargetDirectories(),
                recent.recentDeploymentRemotePackageDirectories(),
                recent.recentDeploymentRemoteBaseDirectories(),
                recent.recentDeploymentDryRuns(),
                recent.recentDeploymentDeployedFlags(),
                recent.recentDeploymentAgentCounts(),
                recent.recentDeploymentBundledFileCounts(),
                recent.recentDeploymentCommandCounts(),
                recent.recentDeploymentExecutedAts(),
                recent.recentDeploymentGeneratedAts(),
                recent.recentDeploymentCreatedAts(),
                recent.recentDeploymentAts(),
                recent.recentDeploymentMessages(),
                recent.recentDeploymentErrorMessages(),
                recent.recentDeploymentAgentIds(),
                recent.recentDeploymentBundledFiles(),
                recent.recentDeploymentCommands(),
                recent.recentDeployments(),
                recent.recentDeploymentProviders(),
                firstOrNull(recent.recentDeploymentJobIds()),
                firstOrNull(recent.recentDeploymentStatuses()),
                firstOrNull(recent.recentDeploymentProviders()),
                firstOrNull(recent.recentDeploymentPackageIds()),
                firstOrNull(recent.recentDeploymentClientTypes()),
                firstOrNull(recent.recentDeploymentPackageTypes()),
                firstOrNull(recent.recentDeploymentHosts()),
                firstOrNull(recent.recentDeploymentPorts()),
                firstOrNull(recent.recentDeploymentTargetDirectories()),
                firstOrNull(recent.recentDeploymentRemotePackageDirectories()),
                firstOrNull(recent.recentDeploymentRemoteBaseDirectories()),
                firstOrNull(recent.recentDeploymentDryRuns()),
                firstOrNull(recent.recentDeploymentDeployedFlags()),
                firstOrNull(recent.recentDeploymentAgentCounts()),
                firstOrNull(recent.recentDeploymentBundledFileCounts()),
                firstOrNull(recent.recentDeploymentCommandCounts()),
                firstOrNull(recent.recentDeploymentExecutedAts()),
                firstOrNull(recent.recentDeploymentGeneratedAts()),
                firstOrNull(recent.recentDeploymentCreatedAts()),
                firstOrNull(recent.recentDeploymentMessages()),
                firstOrNull(recent.recentDeploymentErrorMessages()),
                firstOrNull(recent.recentDeploymentAgentIds()),
                firstOrNull(recent.recentDeploymentBundledFiles()),
                firstOrNull(recent.recentDeploymentCommands()),
                firstOrNull(recent.recentDeploymentAts()),
                latest.status(),
                latest.deploymentAt(),
                recent.recentDeploymentCount(),
                latestDeploymentDetail,
                deploymentSnapshot,
                recentDeploymentHistory,
                deploymentStatusCounts
        );
    }

    private PlatformControlCenterResponse buildControlCenterResponse(ControlCenterResponseSections sections) {
        CustomerCommandCenterResponse customerCommandCenter = sections.customerCommandCenter();
        DeploymentResponseProjection structuredDeployment = sections.structuredDeployment();
        PlatformRecentDeploymentHistoryResponse recent = structuredDeployment.recentDeploymentHistory();
        PlatformLatestDeploymentDetailResponse latest = structuredDeployment.latestDeploymentDetail();
        return new PlatformControlCenterResponse(
                sections.generatedAt(),
                customerCommandCenter,
                customerCommandCenter.healthyCustomers(),
                customerCommandCenter.customersWithReport(),
                customerCommandCenter.customersWithArtifactCatalog(),
                recent.recentDeploymentJobIds(),
                recent.recentDeploymentPackageIds(),
                recent.recentDeploymentStatuses(),
                recent.recentDeploymentProviders(),
                recent.recentDeploymentClientTypes(),
                recent.recentDeploymentHosts(),
                recent.recentDeploymentPorts(),
                recent.recentDeploymentPackageTypes(),
                recent.recentDeploymentTargetDirectories(),
                recent.recentDeploymentRemotePackageDirectories(),
                recent.recentDeploymentRemoteBaseDirectories(),
                recent.recentDeploymentDryRuns(),
                recent.recentDeploymentDeployedFlags(),
                recent.recentDeploymentAgentCounts(),
                recent.recentDeploymentBundledFileCounts(),
                recent.recentDeploymentCommandCounts(),
                recent.recentDeploymentExecutedAts(),
                recent.recentDeploymentGeneratedAts(),
                recent.recentDeploymentCreatedAts(),
                recent.recentDeploymentAts(),
                recent.recentDeploymentMessages(),
                recent.recentDeploymentErrorMessages(),
                recent.recentDeploymentAgentIds(),
                recent.recentDeploymentBundledFiles(),
                recent.recentDeploymentCommands(),
                recent.recentDeployments(),
                structuredDeployment.latestDeploymentSummary(),
                structuredDeployment.deploymentSnapshot(),
                structuredDeployment.recentDeploymentHistory(),
                structuredDeployment.deploymentStatusCounts(),
                structuredDeployment.latestDeploymentDetail(),
                structuredDeployment.deploymentOverview(),
                sections.healthy(),
                sections.statusMessage(),
                latest.deploymentJobId(),
                latest.packageId(),
                latest.dryRun(),
                latest.deployed(),
                latest.agentCount(),
                latest.agentIds(),
                latest.bundledFiles(),
                latest.bundledFileCount(),
                latest.commands(),
                latest.commandCount(),
                latest.executedAt(),
                latest.generatedAt(),
                latest.createdAt(),
                latest.status(),
                latest.deploymentAt(),
                latest.message(),
                latest.errorMessage(),
                latest.host(),
                latest.port(),
                latest.provider(),
                latest.clientType(),
                latest.packageType(),
                latest.targetDirectory(),
                latest.remotePackageDirectory(),
                latest.remoteBaseDirectory(),
                sections.deploymentAuditSummary()
        );
    }

    private DeploymentResponseProjection buildDeploymentResponseProjection(
            TelephonyDeploymentAuditResponse latestDeployment,
            PlatformRecentDeploymentHistoryResponse recentDeploymentHistory,
            DeploymentAuditSummaryResponse deploymentAuditSummary) {
        RecentDeploymentSummaryResponse latestSummary =
                latestDeployment == null ? null : toRecentDeploymentSummary(latestDeployment);
        PlatformLatestDeploymentDetailResponse latestDetail =
                latestDeployment == null ? EMPTY_LATEST_DEPLOYMENT_DETAIL : toLatestDeploymentDetail(latestDeployment);
        PlatformDeploymentSnapshotResponse deploymentSnapshot = buildDeploymentSnapshot(
                latestSummary,
                recentDeploymentHistory,
                latestDetail
        );
        PlatformDeploymentStatusCountsResponse deploymentStatusCounts = buildDeploymentStatusCounts(deploymentAuditSummary);
        PlatformDeploymentOverviewResponse deploymentOverview = buildDeploymentOverview(
                latestSummary,
                latestDetail,
                deploymentSnapshot,
                recentDeploymentHistory,
                deploymentStatusCounts
        );
        return new DeploymentResponseProjection(
                latestSummary,
                deploymentSnapshot,
                recentDeploymentHistory,
                deploymentStatusCounts,
                latestDetail,
                deploymentOverview
        );
    }

    private RecentDeploymentSummaryResponse toRecentDeploymentSummary(TelephonyDeploymentAuditResponse deployment) {
        return new RecentDeploymentSummaryResponse(
                deployment.provider(),
                deployment.deploymentJobId(),
                deployment.packageId(),
                deployment.status(),
                deployment.clientType(),
                deployment.packageType(),
                deployment.host(),
                deployment.port(),
                deployment.targetDirectory(),
                deployment.dryRun(),
                deployment.deployed(),
                agentCount(deployment),
                bundledFileCount(deployment),
                commandCount(deployment),
                deploymentAt(deployment),
                deploymentMessage(deployment)
        );
    }

    private PlatformLatestDeploymentDetailResponse toLatestDeploymentDetail(TelephonyDeploymentAuditResponse deployment) {
        return new PlatformLatestDeploymentDetailResponse(
                deployment.provider(),
                deployment.deploymentJobId(),
                deployment.packageId(),
                deployment.packageType(),
                deployment.clientType(),
                deployment.status(),
                deployment.dryRun(),
                deployment.deployed(),
                deployment.host(),
                deployment.port(),
                deployment.remoteBaseDirectory(),
                deployment.remotePackageDirectory(),
                deployment.targetDirectory(),
                deployment.commands(),
                deployment.bundledFiles(),
                deployment.agentIds(),
                agentCount(deployment),
                bundledFileCount(deployment),
                commandCount(deployment),
                deployment.generatedAt(),
                deployment.executedAt(),
                deployment.createdAt(),
                deploymentAt(deployment),
                deployment.message(),
                deployment.errorMessage()
        );
    }

    private Instant deploymentAt(TelephonyDeploymentAuditResponse deployment) {
        return deployment.executedAt() != null
                ? deployment.executedAt()
                : deployment.generatedAt() != null
                ? deployment.generatedAt()
                : deployment.createdAt();
    }

    private String deploymentMessage(TelephonyDeploymentAuditResponse deployment) {
        return deployment.message() != null && !deployment.message().isBlank()
                ? deployment.message()
                : deployment.errorMessage();
    }

    private record ControlCenterResponseSections(
            CustomerCommandCenterResponse customerCommandCenter,
            DeploymentResponseProjection structuredDeployment,
            Instant generatedAt,
            boolean healthy,
            String statusMessage,
            DeploymentAuditSummaryResponse deploymentAuditSummary) {
    }

    private record DeploymentResponseProjection(
            RecentDeploymentSummaryResponse latestDeploymentSummary,
            PlatformDeploymentSnapshotResponse deploymentSnapshot,
            PlatformRecentDeploymentHistoryResponse recentDeploymentHistory,
            PlatformDeploymentStatusCountsResponse deploymentStatusCounts,
            PlatformLatestDeploymentDetailResponse latestDeploymentDetail,
            PlatformDeploymentOverviewResponse deploymentOverview) {
    }

    private int agentCount(TelephonyDeploymentAuditResponse deployment) {
        return deployment.agentIds() == null ? 0 : deployment.agentIds().size();
    }

    private int bundledFileCount(TelephonyDeploymentAuditResponse deployment) {
        return deployment.bundledFiles() == null ? 0 : deployment.bundledFiles().size();
    }

    private int commandCount(TelephonyDeploymentAuditResponse deployment) {
        return deployment.commands() == null ? 0 : deployment.commands().size();
    }

    private List<String> agentIds(TelephonyDeploymentAuditResponse deployment) {
        return deployment.agentIds() == null ? List.of() : deployment.agentIds();
    }

    private List<String> bundledFiles(TelephonyDeploymentAuditResponse deployment) {
        return deployment.bundledFiles() == null ? List.of() : deployment.bundledFiles();
    }

    private List<String> commands(TelephonyDeploymentAuditResponse deployment) {
        return deployment.commands() == null ? List.of() : deployment.commands();
    }

    private <T> T firstOrNull(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private final class RecentDeploymentAccumulator {
        private final List<String> deploymentJobIds = new ArrayList<>();
        private final List<String> packageIds = new ArrayList<>();
        private final List<String> statuses = new ArrayList<>();
        private final List<String> providers = new ArrayList<>();
        private final List<String> clientTypes = new ArrayList<>();
        private final List<String> hosts = new ArrayList<>();
        private final List<Integer> ports = new ArrayList<>();
        private final List<String> packageTypes = new ArrayList<>();
        private final List<String> targetDirectories = new ArrayList<>();
        private final List<String> remotePackageDirectories = new ArrayList<>();
        private final List<String> remoteBaseDirectories = new ArrayList<>();
        private final List<Boolean> dryRuns = new ArrayList<>();
        private final List<Boolean> deployedFlags = new ArrayList<>();
        private final List<Integer> agentCounts = new ArrayList<>();
        private final List<Integer> bundledFileCounts = new ArrayList<>();
        private final List<Integer> commandCounts = new ArrayList<>();
        private final List<Instant> executedAts = new ArrayList<>();
        private final List<Instant> generatedAts = new ArrayList<>();
        private final List<Instant> createdAts = new ArrayList<>();
        private final List<Instant> deploymentAts = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();
        private final List<String> errorMessages = new ArrayList<>();
        private final List<List<String>> agentIds = new ArrayList<>();
        private final List<List<String>> bundledFiles = new ArrayList<>();
        private final List<List<String>> commands = new ArrayList<>();
        private final List<RecentDeploymentSummaryResponse> deployments = new ArrayList<>();

        private void add(TelephonyDeploymentAuditResponse deployment) {
            deploymentJobIds.add(deployment.deploymentJobId());
            packageIds.add(deployment.packageId());
            statuses.add(deployment.status());
            providers.add(deployment.provider());
            clientTypes.add(deployment.clientType());
            hosts.add(deployment.host());
            ports.add(deployment.port());
            packageTypes.add(deployment.packageType());
            targetDirectories.add(deployment.targetDirectory());
            remotePackageDirectories.add(deployment.remotePackageDirectory());
            remoteBaseDirectories.add(deployment.remoteBaseDirectory());
            dryRuns.add(deployment.dryRun());
            deployedFlags.add(deployment.deployed());
            agentCounts.add(agentCount(deployment));
            bundledFileCounts.add(bundledFileCount(deployment));
            commandCounts.add(commandCount(deployment));
            executedAts.add(deployment.executedAt());
            generatedAts.add(deployment.generatedAt());
            createdAts.add(deployment.createdAt());
            deploymentAts.add(deploymentAt(deployment));
            messages.add(deploymentMessage(deployment));
            errorMessages.add(deployment.errorMessage());
            agentIds.add(agentIds(deployment));
            bundledFiles.add(bundledFiles(deployment));
            commands.add(commands(deployment));
            deployments.add(toRecentDeploymentSummary(deployment));
        }

        private PlatformRecentDeploymentHistoryResponse toRecentDeploymentHistory() {
            return new PlatformRecentDeploymentHistoryResponse(
                    deploymentJobIds,
                    packageIds,
                    statuses,
                    providers,
                    clientTypes,
                    hosts,
                    ports,
                    packageTypes,
                    targetDirectories,
                    remotePackageDirectories,
                    remoteBaseDirectories,
                    dryRuns,
                    deployedFlags,
                    agentCounts,
                    bundledFileCounts,
                    commandCounts,
                    executedAts,
                    generatedAts,
                    createdAts,
                    deploymentAts,
                    messages,
                    errorMessages,
                    agentIds,
                    bundledFiles,
                    commands,
                    deployments,
                    deployments.size()
            );
        }
    }

    private String buildHtml(PlatformControlCenterResponse controlCenter) {
        RecentDeploymentRenderValues recent = renderRecentDeployments(controlCenter);
        StructuredDeploymentRenderValues structured = renderStructuredDeployments(controlCenter);
        LatestDeploymentRenderValues latest = renderLatestDeployment(controlCenter);
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Platform Control Center</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe6; color: #1f2933; }
                    .page { max-width: 1180px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #dccfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 30px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff9f2; border: 1px solid #eadcca; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #786b5d; }
                    .value { font-size: 26px; font-weight: 700; color: #8b4c1f; margin-top: 8px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Platform Control Center</h1>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Healthy Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Deployments</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failed Deployments</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery Ready</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Reports Ready</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Platform Snapshot</h2>
                      <ul>
                        <li>Customers with installs: %s</li>
                        <li>Customers with quotes: %s</li>
                        <li>Customers with artifact catalog: %s</li>
                        <li>Pending deployments: %s</li>
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
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(controlCenter.generatedAt().toString()),
                escapeHtml(controlCenter.statusMessage()),
                escapeHtml(String.valueOf(controlCenter.customerCommandCenter().totalCustomers())),
                escapeHtml(String.valueOf(controlCenter.healthyCustomers())),
                escapeHtml(String.valueOf(controlCenter.deploymentAuditSummary().totalDeployments())),
                escapeHtml(String.valueOf(controlCenter.deploymentAuditSummary().failedDeployments())),
                escapeHtml(String.valueOf(controlCenter.customerCommandCenter().customersWithDeliveryPackage())),
                escapeHtml(String.valueOf(controlCenter.customersWithReports())),
                escapeHtml(String.valueOf(controlCenter.customerCommandCenter().customersWithInstallations())),
                escapeHtml(String.valueOf(controlCenter.customerCommandCenter().customersWithQuotes())),
                escapeHtml(String.valueOf(controlCenter.customersWithArtifactCatalog())),
                escapeHtml(String.valueOf(controlCenter.deploymentAuditSummary().pendingDeployments())),
                escapeHtml(recent.deploymentJobIds()),
                escapeHtml(recent.packageIds()),
                escapeHtml(recent.statuses()),
                escapeHtml(recent.providers()),
                escapeHtml(recent.clientTypes()),
                escapeHtml(recent.hosts()),
                escapeHtml(recent.ports()),
                escapeHtml(recent.packageTypes()),
                escapeHtml(recent.targetDirectories()),
                escapeHtml(recent.remotePackageDirectories()),
                escapeHtml(recent.remoteBaseDirectories()),
                escapeHtml(recent.dryRuns()),
                escapeHtml(recent.deployedFlags()),
                escapeHtml(recent.agentCounts()),
                escapeHtml(recent.bundledFileCounts()),
                escapeHtml(recent.commandCounts()),
                escapeHtml(recent.executedAts()),
                escapeHtml(recent.generatedAts()),
                escapeHtml(recent.createdAts()),
                escapeHtml(recent.deploymentAts()),
                escapeHtml(recent.messages()),
                escapeHtml(recent.errorMessages()),
                escapeHtml(recent.agentIds()),
                escapeHtml(recent.bundledFiles()),
                escapeHtml(recent.commands()),
                escapeHtml(structured.recentDeployments()),
                escapeHtml(structured.latestDeploymentSummary()),
                escapeHtml(structured.latestDeploymentDetail()),
                escapeHtml(structured.deploymentOverview()),
                escapeHtml(structured.deploymentSnapshot()),
                escapeHtml(structured.recentDeploymentHistory()),
                escapeHtml(structured.deploymentStatusCounts()),
                escapeHtml(latest.deployment()),
                escapeHtml(latest.packageId()),
                escapeHtml(latest.dryRun()),
                escapeHtml(latest.deployed()),
                escapeHtml(latest.agentCount()),
                escapeHtml(latest.agentIds()),
                escapeHtml(latest.bundledFiles()),
                escapeHtml(latest.bundledFileCount()),
                escapeHtml(latest.commands()),
                escapeHtml(latest.commandCount()),
                escapeHtml(latest.executedAt()),
                escapeHtml(latest.generatedAt()),
                escapeHtml(latest.createdAt()),
                escapeHtml(latest.status()),
                escapeHtml(latest.deploymentAt()),
                escapeHtml(latest.message()),
                escapeHtml(latest.errorMessage()),
                escapeHtml(latest.host()),
                escapeHtml(latest.port()),
                escapeHtml(latest.provider()),
                escapeHtml(latest.clientType()),
                escapeHtml(latest.packageType()),
                escapeHtml(latest.targetDirectory()),
                escapeHtml(latest.remotePackageDirectory()),
                escapeHtml(latest.remoteBaseDirectory())
        );
    }

    private String buildReadme(PlatformControlCenterResponse controlCenter) {
        RecentDeploymentRenderValues recent = renderRecentDeployments(controlCenter);
        StructuredDeploymentRenderValues structured = renderStructuredDeployments(controlCenter);
        LatestDeploymentRenderValues latest = renderLatestDeployment(controlCenter);
        return """
                Platform control center bundle
                =============================

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
                - platform-control-center.json
                - platform-control-center.html
                - README.txt
                """.formatted(
                controlCenter.generatedAt(),
                controlCenter.healthy(),
                controlCenter.statusMessage(),
                recent.deploymentJobIds(),
                recent.packageIds(),
                recent.statuses(),
                recent.providers(),
                recent.clientTypes(),
                recent.hosts(),
                recent.ports(),
                recent.packageTypes(),
                recent.targetDirectories(),
                recent.remotePackageDirectories(),
                recent.remoteBaseDirectories(),
                recent.dryRuns(),
                recent.deployedFlags(),
                recent.agentCounts(),
                recent.bundledFileCounts(),
                recent.commandCounts(),
                recent.executedAts(),
                recent.generatedAts(),
                recent.createdAts(),
                recent.deploymentAts(),
                recent.messages(),
                recent.errorMessages(),
                recent.agentIds(),
                recent.bundledFiles(),
                recent.commands(),
                structured.recentDeployments(),
                structured.latestDeploymentSummary(),
                structured.latestDeploymentDetail(),
                structured.deploymentOverview(),
                structured.deploymentSnapshot(),
                structured.recentDeploymentHistory(),
                structured.deploymentStatusCounts(),
                latest.deployment(),
                latest.packageId(),
                latest.dryRun(),
                latest.deployed(),
                latest.agentCount(),
                latest.agentIds(),
                latest.bundledFiles(),
                latest.bundledFileCount(),
                latest.commands(),
                latest.commandCount(),
                latest.executedAt(),
                latest.generatedAt(),
                latest.createdAt(),
                latest.status(),
                latest.deploymentAt(),
                latest.message(),
                latest.errorMessage(),
                latest.host(),
                latest.port(),
                latest.provider(),
                latest.clientType(),
                latest.packageType(),
                latest.targetDirectory(),
                latest.remotePackageDirectory(),
                latest.remoteBaseDirectory()
        );
    }

    private String buildExportReadme(PlatformControlCenterResponse controlCenter) {
        return """
                Platform control center export
                =============================

                Generated at: %s
                Status: %s

                Files:
                - platform-control-center.json
                - platform-control-center.html
                - README.txt
                """.formatted(
                controlCenter.generatedAt(),
                controlCenter.statusMessage()
        );
    }

    private RecentDeploymentRenderValues renderRecentDeployments(PlatformControlCenterResponse controlCenter) {
        return new RecentDeploymentRenderValues(
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentJobIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentPackageIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentStatuses()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentProviders()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentClientTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentHosts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentPorts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentPackageTypes()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentTargetDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentRemotePackageDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentRemoteBaseDirectories()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentDryRuns()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentDeployedFlags()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentAgentCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentBundledFileCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentCommandCounts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentExecutedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentGeneratedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentCreatedAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentAts()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentMessages()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.recentDeploymentErrorMessages()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(controlCenter.recentDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(controlCenter.recentDeploymentBundledFiles()),
                PlatformDeploymentSummaryFormatter.formatGroupedStringLists(controlCenter.recentDeploymentCommands())
        );
    }

    private StructuredDeploymentRenderValues renderStructuredDeployments(PlatformControlCenterResponse controlCenter) {
        return new StructuredDeploymentRenderValues(
                PlatformDeploymentSummaryFormatter.formatRecentDeployments(controlCenter.recentDeployments()),
                PlatformDeploymentSummaryFormatter.formatLatestSummary(controlCenter.latestDeploymentSummary()),
                PlatformDeploymentSummaryFormatter.formatLatestDetail(controlCenter.latestDeploymentDetail()),
                PlatformDeploymentSummaryFormatter.formatOverview(controlCenter.deploymentOverview()),
                PlatformDeploymentSummaryFormatter.formatSnapshot(controlCenter.deploymentSnapshot()),
                PlatformDeploymentSummaryFormatter.formatRecentHistory(controlCenter.recentDeploymentHistory()),
                PlatformDeploymentSummaryFormatter.formatStatusCounts(controlCenter.deploymentStatusCounts())
        );
    }

    private LatestDeploymentRenderValues renderLatestDeployment(PlatformControlCenterResponse controlCenter) {
        return new LatestDeploymentRenderValues(
                controlCenter.latestDeploymentJobId() == null ? "None" : controlCenter.latestDeploymentJobId(),
                renderValue(controlCenter.latestDeploymentPackageId()),
                renderValue(controlCenter.latestDeploymentDryRun()),
                renderValue(controlCenter.latestDeploymentDeployed()),
                renderValue(controlCenter.latestDeploymentAgentCount()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.latestDeploymentAgentIds()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.latestDeploymentBundledFiles()),
                renderValue(controlCenter.latestDeploymentBundledFileCount()),
                PlatformDeploymentSummaryFormatter.formatFlatList(controlCenter.latestDeploymentCommands()),
                renderValue(controlCenter.latestDeploymentCommandCount()),
                renderValue(controlCenter.latestDeploymentExecutedAt()),
                renderValue(controlCenter.latestDeploymentGeneratedAt()),
                renderValue(controlCenter.latestDeploymentCreatedAt()),
                renderValue(controlCenter.latestDeploymentStatus()),
                renderValue(controlCenter.latestDeploymentAt()),
                renderValue(controlCenter.latestDeploymentMessage()),
                renderValue(controlCenter.latestDeploymentErrorMessage()),
                renderValue(controlCenter.latestDeploymentHost()),
                renderValue(controlCenter.latestDeploymentPort()),
                renderValue(controlCenter.latestDeploymentProvider()),
                renderValue(controlCenter.latestDeploymentClientType()),
                renderValue(controlCenter.latestDeploymentPackageType()),
                renderValue(controlCenter.latestDeploymentTargetDirectory()),
                renderValue(controlCenter.latestDeploymentRemotePackageDirectory()),
                renderValue(controlCenter.latestDeploymentRemoteBaseDirectory())
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
            throw new IllegalStateException("Failed to serialize platform control center", e);
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

    private String renderValue(Object value) {
        return value == null ? "N/A" : String.valueOf(value);
    }

    private record RecentDeploymentRenderValues(
            String deploymentJobIds,
            String packageIds,
            String statuses,
            String providers,
            String clientTypes,
            String hosts,
            String ports,
            String packageTypes,
            String targetDirectories,
            String remotePackageDirectories,
            String remoteBaseDirectories,
            String dryRuns,
            String deployedFlags,
            String agentCounts,
            String bundledFileCounts,
            String commandCounts,
            String executedAts,
            String generatedAts,
            String createdAts,
            String deploymentAts,
            String messages,
            String errorMessages,
            String agentIds,
            String bundledFiles,
            String commands) {
    }

    private record StructuredDeploymentRenderValues(
            String recentDeployments,
            String latestDeploymentSummary,
            String latestDeploymentDetail,
            String deploymentOverview,
            String deploymentSnapshot,
            String recentDeploymentHistory,
            String deploymentStatusCounts) {
    }

    private record LatestDeploymentRenderValues(
            String deployment,
            String packageId,
            String dryRun,
            String deployed,
            String agentCount,
            String agentIds,
            String bundledFiles,
            String bundledFileCount,
            String commands,
            String commandCount,
            String executedAt,
            String generatedAt,
            String createdAt,
            String status,
            String deploymentAt,
            String message,
            String errorMessage,
            String host,
            String port,
            String provider,
            String clientType,
            String packageType,
            String targetDirectory,
            String remotePackageDirectory,
            String remoteBaseDirectory) {
    }
}
