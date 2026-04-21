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
        Instant createdAt,
        String operatorDisposition,
        String operatorNotes,
        String operatorPriority,
        Instant followUpAt,
        Instant wrapUpUpdatedAt) {

    public CallSessionResponse(String callSessionId,
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
        this(
                callSessionId,
                campaignId,
                leadId,
                provider,
                customerNumber,
                agentId,
                agentChannel,
                callMode,
                ivrFlowId,
                status,
                lastEventType,
                lastEventAt,
                createdAt,
                null,
                null,
                null,
                null,
                null
        );
    }
}
