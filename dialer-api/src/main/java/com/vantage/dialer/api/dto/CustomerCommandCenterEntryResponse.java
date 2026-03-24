package com.vantage.dialer.api.dto;

public record CustomerCommandCenterEntryResponse(
        String customerId,
        CustomerPortfolioEntryResponse portfolio,
        CustomerAccountCenterResponse account,
        boolean healthy,
        boolean hasInstallations,
        boolean hasQuotes,
        boolean hasDeliveryPackage,
        boolean hasReport,
        boolean hasArtifactCatalog,
        String healthStatusMessage,
        String latestInstallationJobId,
        String latestInstallationName,
        String latestInstallationStatus,
        String latestQuoteSnapshotId,
        Double latestSuggestedSellPrice) {
}
