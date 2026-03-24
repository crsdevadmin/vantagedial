package com.vantage.dialer.api.dto;

import com.vantage.dialer.api.agent.Agent;

import java.time.Instant;
import java.util.List;

public record CustomerInstallationResponse(
        String installationJobId,
        String customerId,
        String installationName,
        String clientType,
        String status,
        boolean dryRun,
        boolean deployAfterProvision,
        boolean performRemoteChecks,
        int agentCount,
        String packageId,
        String deploymentJobId,
        List<Agent> provisionedAgents,
        AsteriskDeploymentPreflightResponse preflight,
        AsteriskDeploymentExecutionResponse deployment,
        Instant createdAt,
        Instant completedAt,
        String message,
        String errorMessage) {
}
