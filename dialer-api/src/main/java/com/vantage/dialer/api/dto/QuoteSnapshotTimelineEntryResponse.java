package com.vantage.dialer.api.dto;

public record QuoteSnapshotTimelineEntryResponse(
        QuoteSnapshotResponse snapshot,
        String previousQuoteSnapshotId,
        boolean comparisonAvailable,
        CommercialAssumptionsDeltaResponse assumptionsDelta,
        CostEstimateDeltaResponse estimateDelta) {
}
