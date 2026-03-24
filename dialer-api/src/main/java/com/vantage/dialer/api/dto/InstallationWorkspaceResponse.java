package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationWorkspaceResponse(
        String customerId,
        Instant generatedAt,
        InstallationDashboardResponse dashboard,
        InstallationTimelineBundleResponse timelineBundle,
        InstallationHealthResponse health,
        InstallationReportResponse report,
        InstallationOverviewResponse overview) {
}
