package com.vantage.dialer.api.dto;

import java.time.Instant;

public record QuoteSnapshotComparisonResponse(
        String baseQuoteSnapshotId,
        String targetQuoteSnapshotId,
        Instant comparedAt,
        QuoteSnapshotResponse base,
        QuoteSnapshotResponse target,
        CommercialAssumptionsDeltaResponse assumptionsDelta,
        CostEstimateDeltaResponse estimateDelta) {
}
