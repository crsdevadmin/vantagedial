package com.vantage.dialer.api.dto;

public record CampaignResponse(
        String campaignId,
        String name,
        String provider,
        String dialMode,
        String status,
        String ivrFlowId,
        int maxConcurrentCalls,
        int callsPerSecond,
        double predictiveRatio) {
}
