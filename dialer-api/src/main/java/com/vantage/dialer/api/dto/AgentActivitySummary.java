package com.vantage.dialer.api.dto;

public record AgentActivitySummary(
        String agentId,
        long callsHandled,
        long answeredCalls,
        long bridgedCalls,
        long completedCalls,
        long failedCalls) {
}
