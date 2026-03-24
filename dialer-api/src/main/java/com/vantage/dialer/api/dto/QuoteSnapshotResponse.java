package com.vantage.dialer.api.dto;

import java.time.Instant;

public record QuoteSnapshotResponse(
        String quoteSnapshotId,
        String installationJobId,
        String customerId,
        String configurationId,
        String filePath,
        Instant createdAt,
        InstallationQuoteSummaryResponse summary) {
}
