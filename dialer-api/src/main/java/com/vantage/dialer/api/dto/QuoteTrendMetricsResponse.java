package com.vantage.dialer.api.dto;

public record QuoteTrendMetricsResponse(
        Double averageSuggestedSellPrice,
        Double averageEstimatedCost,
        Double latestSuggestedSellPriceDelta,
        Double latestEstimatedCostDelta,
        String suggestedSellPriceTrend,
        String estimatedCostTrend) {
}
