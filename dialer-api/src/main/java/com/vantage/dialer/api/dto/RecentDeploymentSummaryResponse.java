package com.vantage.dialer.api.dto;

import java.time.Instant;

public record RecentDeploymentSummaryResponse(
        String provider,
        String deploymentJobId,
        String packageId,
        String status,
        String clientType,
        String packageType,
        String host,
        Integer port,
        String targetDirectory,
        Boolean dryRun,
        Boolean deployed,
        Integer agentCount,
        Integer bundledFileCount,
        Integer commandCount,
        Instant deploymentAt,
        String message
) {
}
