package com.vantage.dialer.api.dto;

public record CustomerPortfolioEntryResponse(
        String customerId,
        String latestInstallationJobId,
        String latestInstallationName,
        String latestInstallationStatus,
        int totalInstallations,
        int completedInstallations,
        int failedInstallations,
        int quoteSnapshotCount,
        String latestQuoteSnapshotId,
        Double latestSuggestedSellPrice,
        boolean deliveryPackageAvailable,
        boolean healthy,
        String healthStatusMessage,
        boolean reportAvailable,
        boolean artifactCatalogAvailable) {
}
