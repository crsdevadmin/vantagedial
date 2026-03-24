package com.vantage.dialer.api.dto;

public record CostEstimateDeltaResponse(
        Double fixedInfrastructureCostDelta,
        Double variableUsageCostDelta,
        Double totalEstimatedCostDelta,
        Double suggestedSellPriceDelta,
        Double desiredMarginPercentDelta) {
}
