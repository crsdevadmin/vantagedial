package com.vantage.dialer.api.dto;

import java.time.Instant;

public record QuoteSnapshotBundleResponse(
        String quoteSnapshotId,
        String bundleDirectoryPath,
        String summaryJsonPath,
        String assumptionsJsonPath,
        String requestJsonPath,
        String csvPath,
        String readmePath,
        Instant generatedAt) {
}
