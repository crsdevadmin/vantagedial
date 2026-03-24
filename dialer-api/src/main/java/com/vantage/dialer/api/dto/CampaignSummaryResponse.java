package com.vantage.dialer.api.dto;

public record CampaignSummaryResponse(
        String campaignId,
        long totalSessions,
        long completedSessions,
        long failedSessions,
        long bridgedSessions,
        long answeredSessions) {
}
