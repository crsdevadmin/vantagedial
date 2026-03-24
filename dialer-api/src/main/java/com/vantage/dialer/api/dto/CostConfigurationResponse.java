package com.vantage.dialer.api.dto;

public record CostConfigurationResponse(
        String configurationId,
        String customerId,
        double asteriskServerMonthlyCost,
        double appServerMonthlyCost,
        double ebsMonthlyCost,
        double snapshotMonthlyCost,
        double voiceMinuteCost,
        double ttsUnitCost,
        double sttMinuteCost,
        double recordingGbCost) {
}
