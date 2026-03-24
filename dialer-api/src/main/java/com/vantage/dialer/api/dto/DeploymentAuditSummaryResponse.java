package com.vantage.dialer.api.dto;

public record DeploymentAuditSummaryResponse(
        int totalDeployments,
        int pendingDeployments,
        int dryRunDeployments,
        int successfulDeployments,
        int failedDeployments,
        TelephonyDeploymentAuditResponse latestDeployment) {
}
