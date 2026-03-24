package com.vantage.dialer.api.dto;

import java.time.Instant;

public record QuoteSnapshotDashboardBundleResponse(
        String installationJobId,
        String customerId,
        String bundleDirectoryPath,
        String summaryJsonPath,
        String timelineJsonPath,
        String dashboardMarkdownPath,
        String dashboardHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
