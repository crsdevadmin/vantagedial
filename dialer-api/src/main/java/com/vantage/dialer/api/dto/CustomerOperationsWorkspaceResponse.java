package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerOperationsWorkspaceResponse(
        String customerId,
        Instant generatedAt,
        CustomerInstallationResponse latestInstallation,
        InstallationOverviewResponse installationOverview,
        QuoteSnapshotSummaryResponse quoteSummary,
        CustomerDeliveryPackageDetailResponse latestDeliveryPackage,
        boolean deliveryPackageAvailable,
        boolean healthy,
        boolean hasReport,
        boolean hasArtifactCatalog,
        String statusMessage,
        String latestInstallationJobId,
        String latestInstallationName,
        String latestInstallationStatus,
        String latestQuoteSnapshotId,
        Double latestSuggestedSellPrice) {
}
