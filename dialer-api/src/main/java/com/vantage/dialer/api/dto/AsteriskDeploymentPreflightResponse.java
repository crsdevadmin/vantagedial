package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record AsteriskDeploymentPreflightResponse(
        boolean ready,
        boolean remoteChecksExecuted,
        boolean deploymentEnabled,
        String host,
        int port,
        String user,
        String remoteBaseDirectory,
        String targetDirectory,
        List<AsteriskPreflightCheckResponse> checks,
        List<String> commands,
        Instant checkedAt) {
}
