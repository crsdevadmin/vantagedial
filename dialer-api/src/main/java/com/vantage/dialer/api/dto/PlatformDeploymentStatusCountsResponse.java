package com.vantage.dialer.api.dto;

public record PlatformDeploymentStatusCountsResponse(
        int totalDeployments,
        int pendingDeployments,
        int dryRunDeployments,
        int successfulDeployments,
        int failedDeployments
) {
}
