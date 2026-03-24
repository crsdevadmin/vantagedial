package com.vantage.dialer.api.dto;

public record CommercialAssumptionsResponse(
        String source,
        Long monthlyCallMinutes,
        Long monthlyTtsUnits,
        Long monthlySttMinutes,
        Double monthlyRecordingGb,
        Integer agentCount,
        Integer concurrentChannels,
        Double desiredMarginPercent) {
}
