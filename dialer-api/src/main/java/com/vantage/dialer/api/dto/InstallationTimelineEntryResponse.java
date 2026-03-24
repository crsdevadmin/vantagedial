package com.vantage.dialer.api.dto;

import java.time.Duration;
import java.time.Instant;

public record InstallationTimelineEntryResponse(
        String installationJobId,
        String installationName,
        String customerId,
        String clientType,
        String status,
        Instant createdAt,
        Instant completedAt,
        Long durationSeconds,
        int agentCount,
        String deploymentJobId,
        String message,
        String errorMessage) {

    public static InstallationTimelineEntryResponse from(CustomerInstallationResponse installation) {
        Long durationSeconds = null;
        if (installation.createdAt() != null && installation.completedAt() != null) {
            durationSeconds = Duration.between(installation.createdAt(), installation.completedAt()).getSeconds();
        }
        return new InstallationTimelineEntryResponse(
                installation.installationJobId(),
                installation.installationName(),
                installation.customerId(),
                installation.clientType(),
                installation.status(),
                installation.createdAt(),
                installation.completedAt(),
                durationSeconds,
                installation.agentCount(),
                installation.deploymentJobId(),
                installation.message(),
                installation.errorMessage()
        );
    }
}
