package com.vantage.dialer.api.dto;

public record CostEstimateResponse(
        String customerId,
        String configurationId,
        double fixedInfrastructureCost,
        double variableUsageCost,
        double totalEstimatedCost,
        double suggestedSellPrice,
        double desiredMarginPercent) {
}
