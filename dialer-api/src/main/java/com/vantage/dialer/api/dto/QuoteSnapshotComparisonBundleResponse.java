package com.vantage.dialer.api.dto;

import java.time.Instant;

public record QuoteSnapshotComparisonBundleResponse(
        String quoteSnapshotId,
        String previousQuoteSnapshotId,
        boolean comparisonAvailable,
        String bundleDirectoryPath,
        String comparisonJsonPath,
        String summaryMarkdownPath,
        String readmePath,
        Instant generatedAt) {
}
