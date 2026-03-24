package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record TelephonyDeploymentAuditResponse(
        String provider,
        String deploymentJobId,
        String packageId,
        String packageType,
        String clientType,
        String status,
        boolean dryRun,
        boolean deployed,
        String host,
        int port,
        String remoteBaseDirectory,
        String remotePackageDirectory,
        String targetDirectory,
        List<String> commands,
        List<String> bundledFiles,
        List<String> agentIds,
        Instant generatedAt,
        Instant executedAt,
        Instant createdAt,
        String message,
        String errorMessage) {
}
