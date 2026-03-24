package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationTimelineExportResponse(
        String customerId,
        String exportDirectory,
        String timelineJsonPath,
        String timelineCsvPath,
        String timelineHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
