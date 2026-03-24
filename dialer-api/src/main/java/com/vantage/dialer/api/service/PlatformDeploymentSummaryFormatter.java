package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.PlatformDeploymentOverviewResponse;
import com.vantage.dialer.api.dto.PlatformDeploymentSnapshotResponse;
import com.vantage.dialer.api.dto.PlatformDeploymentStatusCountsResponse;
import com.vantage.dialer.api.dto.PlatformLatestDeploymentDetailResponse;
import com.vantage.dialer.api.dto.PlatformRecentDeploymentHistoryResponse;
import com.vantage.dialer.api.dto.RecentDeploymentSummaryResponse;

import java.util.List;
import java.util.stream.Collectors;

final class PlatformDeploymentSummaryFormatter {

    private PlatformDeploymentSummaryFormatter() {
    }

    static String formatLatestDetail(PlatformLatestDeploymentDetailResponse detail) {
        return detail == null
                ? "N/A"
                : joinFields(
                field("provider", detail.provider()),
                field("job", detail.deploymentJobId()),
                field("status", detail.status()),
                field("at", detail.deploymentAt()));
    }

    static String formatLatestSummary(RecentDeploymentSummaryResponse summary) {
        return summary == null
                ? "N/A"
                : joinFields(
                field("provider", summary.provider()),
                field("job", summary.deploymentJobId()),
                field("status", summary.status()),
                field("at", summary.deploymentAt()));
    }

    static String formatRecentDeployments(List<RecentDeploymentSummaryResponse> deployments) {
        return deployments == null || deployments.isEmpty()
                ? "N/A"
                : deployments.stream()
                .map(PlatformDeploymentSummaryFormatter::formatLatestSummary)
                .reduce((left, right) -> left + " ; " + right)
                .orElse("N/A");
    }

    static String formatOverview(PlatformDeploymentOverviewResponse overview) {
        return overview == null
                ? "N/A"
                : "%s, %s, %s".formatted(
                formatOverviewLatest(overview),
                formatOverviewRecent(overview),
                formatOverviewMostRecent(overview));
    }

    static String formatSnapshot(PlatformDeploymentSnapshotResponse snapshot) {
        return snapshot == null
                ? "N/A"
                : joinFields(
                field("latest", snapshot.latestDeploymentSummary() == null
                        ? null
                        : snapshot.latestDeploymentSummary().deploymentJobId()),
                field("recent", snapshot.recentDeploymentCount()),
                field("at", snapshot.latestDeploymentAt()),
                field("provider", snapshot.latestDeploymentProvider()),
                flatListField("recentProviders", snapshot.recentDeploymentProviders()),
                field("status", snapshot.latestDeploymentStatus()));
    }

    static String formatRecentHistory(PlatformRecentDeploymentHistoryResponse history) {
        return history == null
                ? "N/A"
                : joinFields(
                field("count", history.recentDeploymentCount()),
                flatListField("providers", history.recentDeploymentProviders()));
    }

    static String formatStatusCounts(PlatformDeploymentStatusCountsResponse counts) {
        return counts == null
                ? "N/A"
                : joinFields(
                field("total", counts.totalDeployments()),
                field("pending", counts.pendingDeployments()),
                field("dryRun", counts.dryRunDeployments()),
                field("success", counts.successfulDeployments()),
                field("failed", counts.failedDeployments()));
    }

    static String formatFlatList(List<?> values) {
        return values == null || values.isEmpty()
                ? "N/A"
                : values.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    static String formatGroupedStringLists(List<List<String>> groups) {
        return groups == null || groups.isEmpty()
                ? "N/A"
                : groups.stream()
                .map(group -> group == null || group.isEmpty() ? "N/A" : String.join(",", group))
                .collect(Collectors.joining(" ; "));
    }

    private static String formatOverviewLatest(PlatformDeploymentOverviewResponse overview) {
        return joinFields(
                field("latest", overview.latestDeploymentJobId()),
                field("package", overview.latestDeploymentPackageId()),
                field("provider", overview.latestDeploymentProvider()),
                field("clientType", overview.latestDeploymentClientType()),
                field("packageType", overview.latestDeploymentPackageType()),
                field("dryRun", overview.latestDeploymentDryRun()),
                field("deployed", overview.latestDeploymentDeployed()),
                field("host", overview.latestDeploymentHost()),
                field("port", overview.latestDeploymentPort()),
                field("target", overview.latestDeploymentTargetDirectory()),
                field("remotePackage", overview.latestDeploymentRemotePackageDirectory()),
                field("remoteBase", overview.latestDeploymentRemoteBaseDirectory()),
                field("agents", overview.latestDeploymentAgentCount()),
                flatListField("agentIds", overview.latestDeploymentAgentIds()),
                field("files", overview.latestDeploymentBundledFileCount()),
                flatListField("bundledFiles", overview.latestDeploymentBundledFiles()),
                field("commands", overview.latestDeploymentCommandCount()),
                flatListField("commandList", overview.latestDeploymentCommands()),
                field("executedAt", overview.latestDeploymentExecutedAt()),
                field("generatedAt", overview.latestDeploymentGeneratedAt()),
                field("createdAt", overview.latestDeploymentCreatedAt()),
                field("message", overview.latestDeploymentMessage()),
                field("error", overview.latestDeploymentErrorMessage()),
                field("status", overview.latestDeploymentStatus()),
                field("at", overview.latestDeploymentAt())
        );
    }

    private static String formatOverviewRecent(PlatformDeploymentOverviewResponse overview) {
        return joinFields(
                field("recent", overview.recentDeploymentCount()),
                flatListField("recentJobIds", overview.recentDeploymentJobIds()),
                flatListField("recentPackageIds", overview.recentDeploymentPackageIds()),
                flatListField("recentStatuses", overview.recentDeploymentStatuses()),
                flatListField("recentClientTypes", overview.recentDeploymentClientTypes()),
                flatListField("recentHosts", overview.recentDeploymentHosts()),
                flatListField("recentPorts", overview.recentDeploymentPorts()),
                flatListField("recentPackageTypes", overview.recentDeploymentPackageTypes()),
                flatListField("recentTargets", overview.recentDeploymentTargetDirectories()),
                flatListField("recentRemotePackages", overview.recentDeploymentRemotePackageDirectories()),
                flatListField("recentRemoteBases", overview.recentDeploymentRemoteBaseDirectories()),
                flatListField("recentDryRuns", overview.recentDeploymentDryRuns()),
                flatListField("recentDeployed", overview.recentDeploymentDeployedFlags()),
                flatListField("recentAgentCounts", overview.recentDeploymentAgentCounts()),
                flatListField("recentBundledFileCounts", overview.recentDeploymentBundledFileCounts()),
                flatListField("recentCommandCounts", overview.recentDeploymentCommandCounts()),
                flatListField("recentExecutedAts", overview.recentDeploymentExecutedAts()),
                flatListField("recentGeneratedAts", overview.recentDeploymentGeneratedAts()),
                flatListField("recentCreatedAts", overview.recentDeploymentCreatedAts()),
                flatListField("recentAts", overview.recentDeploymentAts()),
                flatListField("recentMessages", overview.recentDeploymentMessages()),
                flatListField("recentErrors", overview.recentDeploymentErrorMessages()),
                groupedListField("recentAgentIds", overview.recentDeploymentAgentIds()),
                groupedListField("recentBundledFiles", overview.recentDeploymentBundledFiles()),
                groupedListField("recentCommands", overview.recentDeploymentCommands()),
                field("recentDeployments", formatRecentDeployments(overview.recentDeployments())),
                flatListField("recentProviders", overview.recentDeploymentProviders())
        );
    }

    private static String formatOverviewMostRecent(PlatformDeploymentOverviewResponse overview) {
        return joinFields(
                field("mostRecentJob", overview.mostRecentDeploymentJobId()),
                field("mostRecentStatus", overview.mostRecentDeploymentStatus()),
                field("mostRecentProvider", overview.mostRecentDeploymentProvider()),
                field("mostRecentPackage", overview.mostRecentDeploymentPackageId()),
                field("mostRecentClientType", overview.mostRecentDeploymentClientType()),
                field("mostRecentPackageType", overview.mostRecentDeploymentPackageType()),
                field("mostRecentHost", overview.mostRecentDeploymentHost()),
                field("mostRecentPort", overview.mostRecentDeploymentPort()),
                field("mostRecentTarget", overview.mostRecentDeploymentTargetDirectory()),
                field("mostRecentRemotePackage", overview.mostRecentDeploymentRemotePackageDirectory()),
                field("mostRecentRemoteBase", overview.mostRecentDeploymentRemoteBaseDirectory()),
                field("mostRecentDryRun", overview.mostRecentDeploymentDryRun()),
                field("mostRecentDeployed", overview.mostRecentDeploymentDeployed()),
                field("mostRecentAgents", overview.mostRecentDeploymentAgentCount()),
                field("mostRecentFiles", overview.mostRecentDeploymentBundledFileCount()),
                field("mostRecentCommands", overview.mostRecentDeploymentCommandCount()),
                field("mostRecentExecutedAt", overview.mostRecentDeploymentExecutedAt()),
                field("mostRecentGeneratedAt", overview.mostRecentDeploymentGeneratedAt()),
                field("mostRecentCreatedAt", overview.mostRecentDeploymentCreatedAt()),
                field("mostRecentMessage", overview.mostRecentDeploymentMessage()),
                field("mostRecentError", overview.mostRecentDeploymentErrorMessage()),
                flatListField("mostRecentAgentIds", overview.mostRecentDeploymentAgentIds()),
                flatListField("mostRecentBundledFiles", overview.mostRecentDeploymentBundledFiles()),
                flatListField("mostRecentCommandsList", overview.mostRecentDeploymentCommands()),
                field("mostRecentAt", overview.mostRecentDeploymentAt())
        );
    }

    private static String joinFields(String... fields) {
        return String.join(", ", fields);
    }

    private static String field(String label, Object value) {
        return "%s=%s".formatted(label, valueOrNA(value));
    }

    private static String flatListField(String label, List<?> values) {
        return "%s=%s".formatted(label, formatFlatList(values));
    }

    private static String groupedListField(String label, List<List<String>> values) {
        return "%s=%s".formatted(label, formatGroupedStringLists(values));
    }

    private static String valueOrNA(Object value) {
        return value == null ? "N/A" : String.valueOf(value);
    }
}
