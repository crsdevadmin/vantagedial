package com.vantage.dialer.api.dto;

public record QuoteSnapshotSummaryResponse(
        String installationJobId,
        String customerId,
        int snapshotCount,
        QuoteSnapshotResponse latestSnapshot,
        QuoteSnapshotPreviousComparisonResponse latestComparison,
        Double minimumSuggestedSellPrice,
        Double maximumSuggestedSellPrice,
        Double latestSuggestedSellPrice,
        Double latestEstimatedCost,
        QuoteTrendMetricsResponse trendMetrics) {
}
