package com.vantage.dialer.api.dto;

import java.time.Instant;

public record OperatorWrapUpResponse(
        String callSessionId,
        String campaignId,
        String customerNumber,
        String agentId,
        String disposition,
        String notes,
        String priority,
        Instant followUpAt,
        Instant wrapUpUpdatedAt) {
}
