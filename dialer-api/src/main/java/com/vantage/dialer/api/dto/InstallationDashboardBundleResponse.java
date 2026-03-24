package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record InstallationDashboardBundleResponse(
        String customerId,
        String bundleDirectory,
        String dashboardJsonPath,
        String dashboardMarkdownPath,
        String dashboardHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
