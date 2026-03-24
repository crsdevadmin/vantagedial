package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record OperationalDashboardResponse(
        String campaignId,
        Instant from,
        Instant to,
        Instant generatedAt,
        CampaignSummaryResponse campaignSummary,
        IvrCampaignSummaryResponse ivrSummary,
        List<AgentActivitySummary> agentActivity,
        long totalAgents,
        long totalCallsHandled,
        long totalAnsweredCalls,
        long totalCompletedCalls,
        long totalFailedCalls) {
}
