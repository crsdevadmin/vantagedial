package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerHealthResponse(
        String customerId,
        Instant generatedAt,
        boolean healthy,
        String statusMessage,
        int totalInstallations,
        int completedInstallations,
        int failedInstallations,
        int quoteSnapshotCount,
        boolean deliveryPackageAvailable,
        boolean reportAvailable,
        boolean artifactCatalogAvailable,
        Double latestSuggestedSellPrice,
        String latestInstallationJobId,
        String latestInstallationName,
        String latestInstallationStatus,
        String latestQuoteSnapshotId) {
}
