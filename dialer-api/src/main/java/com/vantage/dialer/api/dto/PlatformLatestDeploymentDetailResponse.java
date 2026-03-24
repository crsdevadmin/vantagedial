package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record PlatformLatestDeploymentDetailResponse(
        String provider,
        String deploymentJobId,
        String packageId,
        String packageType,
        String clientType,
        String status,
        Boolean dryRun,
        Boolean deployed,
        String host,
        Integer port,
        String remoteBaseDirectory,
        String remotePackageDirectory,
        String targetDirectory,
        List<String> commands,
        List<String> bundledFiles,
        List<String> agentIds,
        Integer agentCount,
        Integer bundledFileCount,
        Integer commandCount,
        Instant generatedAt,
        Instant executedAt,
        Instant createdAt,
        Instant deploymentAt,
        String message,
        String errorMessage) {
}
