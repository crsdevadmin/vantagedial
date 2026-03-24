package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record InstallationDashboardResponse(
        String customerId,
        Instant generatedAt,
        int totalInstallations,
        int completedInstallations,
        int failedInstallations,
        int dryRunInstallations,
        int pendingInstallations,
        int totalProvisionedAgents,
        CustomerInstallationResponse latestInstallation,
        List<CustomerInstallationResponse> installations) {
}
