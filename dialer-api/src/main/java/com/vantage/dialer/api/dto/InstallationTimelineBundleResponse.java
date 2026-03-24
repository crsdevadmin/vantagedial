package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record InstallationTimelineBundleResponse(
        String customerId,
        String bundleDirectory,
        String timelineJsonPath,
        String timelineCsvPath,
        String timelineMarkdownPath,
        String timelineHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
