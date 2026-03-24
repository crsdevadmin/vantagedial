package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record InstallationOverviewBundleResponse(
        String customerId,
        String bundleDirectory,
        String overviewJsonPath,
        String dashboardPath,
        String healthPath,
        String reportPath,
        String overviewMarkdownPath,
        String overviewHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
