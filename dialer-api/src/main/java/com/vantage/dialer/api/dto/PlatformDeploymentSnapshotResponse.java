package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record PlatformDeploymentSnapshotResponse(
        RecentDeploymentSummaryResponse latestDeploymentSummary,
        List<RecentDeploymentSummaryResponse> recentDeployments,
        Integer recentDeploymentCount,
        Instant latestDeploymentAt,
        String latestDeploymentProvider,
        List<String> recentDeploymentProviders,
        String latestDeploymentStatus
) {
}
