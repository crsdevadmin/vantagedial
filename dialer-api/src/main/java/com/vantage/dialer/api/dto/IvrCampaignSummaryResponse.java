package com.vantage.dialer.api.dto;

public record IvrCampaignSummaryResponse(
        String campaignId,
        String ivrFlowId,
        long totalIvrSessions,
        long completedIvrSessions,
        long failedIvrSessions) {
}
