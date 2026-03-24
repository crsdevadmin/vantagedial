package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InstallationHealthResponse(
        String customerId,
        Instant generatedAt,
        int totalInstallations,
        int completedInstallations,
        int failedInstallations,
        int dryRunInstallations,
        int pendingInstallations,
        Map<String, Integer> clientTypeCounts,
        List<InstallationTimelineEntryResponse> recentFailures) {
}
