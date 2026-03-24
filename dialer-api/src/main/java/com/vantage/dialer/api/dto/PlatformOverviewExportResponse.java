package com.vantage.dialer.api.dto;

import java.time.Instant;

public record PlatformOverviewExportResponse(
        String exportDirectory,
        String overviewJsonPath,
        String overviewHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
