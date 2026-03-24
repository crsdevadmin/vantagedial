package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record AsteriskDeploymentExecutionResponse(
        String deploymentJobId,
        String packageId,
        String packageType,
        String clientType,
        boolean dryRun,
        boolean deployed,
        String host,
        int port,
        String remoteBaseDirectory,
        String remotePackageDirectory,
        String targetDirectory,
        List<String> commands,
        List<String> bundledFiles,
        Instant generatedAt,
        Instant executedAt,
        String message) {
}
