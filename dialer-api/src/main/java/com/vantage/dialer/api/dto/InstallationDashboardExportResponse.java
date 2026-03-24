package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationDashboardExportResponse(
        String customerId,
        String exportDirectory,
        String dashboardJsonPath,
        String dashboardCsvPath,
        String dashboardHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
