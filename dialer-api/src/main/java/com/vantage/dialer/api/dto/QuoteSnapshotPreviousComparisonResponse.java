package com.vantage.dialer.api.dto;

import java.time.Instant;

public record QuoteSnapshotPreviousComparisonResponse(
        String quoteSnapshotId,
        String previousQuoteSnapshotId,
        Instant comparedAt,
        boolean comparisonAvailable,
        String message,
        QuoteSnapshotComparisonResponse comparison) {
}
