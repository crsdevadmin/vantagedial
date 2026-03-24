package com.vantage.dialer.api.dto;

import java.time.Instant;

public record QuoteSnapshotDashboardExportResponse(
        String installationJobId,
        String customerId,
        String exportDirectoryPath,
        String dashboardJsonPath,
        String dashboardCsvPath,
        String dashboardHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
