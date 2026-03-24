package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerAccountCenterResponse(
        String customerId,
        Instant generatedAt,
        CustomerOperationsWorkspaceResponse operationsWorkspace,
        QuoteSnapshotDashboardResponse quoteDashboard,
        InstallationWorkspaceResponse installationWorkspace,
        CustomerDeliveryPackageDetailResponse latestDeliveryPackage,
        boolean healthy,
        boolean hasInstallations,
        boolean hasQuotes,
        boolean hasDeliveryPackage,
        boolean hasReport,
        boolean hasArtifactCatalog,
        String statusMessage,
        String latestInstallationJobId,
        String latestInstallationStatus,
        String latestQuoteSnapshotId,
        Double latestSuggestedSellPrice,
        String latestInstallationName) {
}
