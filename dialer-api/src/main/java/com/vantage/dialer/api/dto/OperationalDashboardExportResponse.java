package com.vantage.dialer.api.dto;

import java.time.Instant;

public record OperationalDashboardExportResponse(
        String campaignId,
        Instant from,
        Instant to,
        String exportDirectoryPath,
        String dashboardJsonPath,
        String dashboardCsvPath,
        String dashboardMarkdownPath,
        String readmePath,
        Instant generatedAt) {
}
