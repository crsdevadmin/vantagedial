package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record PlatformOverviewBundleResponse(
        String bundleDirectory,
        String overviewJsonPath,
        String overviewHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
