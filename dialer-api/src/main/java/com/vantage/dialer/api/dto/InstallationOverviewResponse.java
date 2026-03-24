package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationOverviewResponse(
        String customerId,
        Instant generatedAt,
        InstallationDashboardResponse dashboard,
        InstallationHealthResponse health,
        InstallationReportResponse report) {
}
