package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record InstallationReportBundleResponse(
        String customerId,
        String bundleDirectory,
        String dashboardPath,
        String timelinePath,
        String latestInstallationPath,
        String reportMarkdownPath,
        String reportHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
