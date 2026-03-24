package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CallSessionResponse(
        String callSessionId,
        String campaignId,
        String leadId,
        String provider,
        String customerNumber,
        String agentId,
        String agentChannel,
        String callMode,
        String ivrFlowId,
        String status,
        String lastEventType,
        Instant lastEventAt,
        Instant createdAt) {
}
