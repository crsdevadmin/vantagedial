package com.vantage.dialer.api.dto;

import java.time.Instant;

public record OperationalDashboardBundleResponse(
        String campaignId,
        Instant from,
        Instant to,
        String bundleDirectoryPath,
        String dashboardJsonPath,
        String dashboardCsvPath,
        String dashboardMarkdownPath,
        String dashboardHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
