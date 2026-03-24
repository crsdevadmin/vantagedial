package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record PlatformRecentDeploymentHistoryResponse(
        List<String> recentDeploymentJobIds,
        List<String> recentDeploymentPackageIds,
        List<String> recentDeploymentStatuses,
        List<String> recentDeploymentProviders,
        List<String> recentDeploymentClientTypes,
        List<String> recentDeploymentHosts,
        List<Integer> recentDeploymentPorts,
        List<String> recentDeploymentPackageTypes,
        List<String> recentDeploymentTargetDirectories,
        List<String> recentDeploymentRemotePackageDirectories,
        List<String> recentDeploymentRemoteBaseDirectories,
        List<Boolean> recentDeploymentDryRuns,
        List<Boolean> recentDeploymentDeployedFlags,
        List<Integer> recentDeploymentAgentCounts,
        List<Integer> recentDeploymentBundledFileCounts,
        List<Integer> recentDeploymentCommandCounts,
        List<Instant> recentDeploymentExecutedAts,
        List<Instant> recentDeploymentGeneratedAts,
        List<Instant> recentDeploymentCreatedAts,
        List<Instant> recentDeploymentAts,
        List<String> recentDeploymentMessages,
        List<String> recentDeploymentErrorMessages,
        List<List<String>> recentDeploymentAgentIds,
        List<List<String>> recentDeploymentBundledFiles,
        List<List<String>> recentDeploymentCommands,
        List<RecentDeploymentSummaryResponse> recentDeployments,
        Integer recentDeploymentCount) {
}
