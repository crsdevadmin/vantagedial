package com.vantage.dialer.api.dto;

public record CommercialAssumptionsDeltaResponse(
        Long monthlyCallMinutesDelta,
        Long monthlyTtsUnitsDelta,
        Long monthlySttMinutesDelta,
        Double monthlyRecordingGbDelta,
        Integer agentCountDelta,
        Integer concurrentChannelsDelta,
        Double desiredMarginPercentDelta) {
}
