package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record InstallationHandoffBundleResponse(
        String installationJobId,
        String installationName,
        String bundleDirectory,
        String installationPath,
        String bootstrapBundleMetadataPath,
        String quoteSummaryPath,
        String quoteDashboardPath,
        String handoffMarkdownPath,
        String handoffHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
