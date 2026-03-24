package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record AsteriskDeploymentPackageResponse(
        String packageId,
        String packageType,
        String clientType,
        String packageDirectory,
        String pjsipConfigPath,
        String reloadScriptPath,
        String manifestPath,
        Instant generatedAt,
        List<String> agentIds,
        List<String> bundledFiles) {
}
