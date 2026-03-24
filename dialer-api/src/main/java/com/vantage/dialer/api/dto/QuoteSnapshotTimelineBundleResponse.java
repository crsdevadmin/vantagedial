package com.vantage.dialer.api.dto;

import java.time.Instant;

public record QuoteSnapshotTimelineBundleResponse(
        String installationJobId,
        String customerId,
        String bundleDirectoryPath,
        String timelineJsonPath,
        String timelineCsvPath,
        String summaryMarkdownPath,
        String readmePath,
        Instant generatedAt) {
}
