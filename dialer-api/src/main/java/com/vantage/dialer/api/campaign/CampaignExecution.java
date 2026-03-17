package com.vantage.dialer.api.campaign;

public record CampaignExecution(
        String campaignId,
        int maxConcurrentCalls,
        int callsPerSecond,
        String provider,
        DialMode dialMode,
        double predictiveRatio) {
}
