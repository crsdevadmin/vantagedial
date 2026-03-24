package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerArtifactCatalogResponse(
        String customerId,
        Instant generatedAt,
        boolean healthy,
        String statusMessage,
        String latestInstallationJobId,
        String latestInstallationName,
        String latestInstallationStatus,
        String latestQuoteSnapshotId,
        Double latestSuggestedSellPrice,
        CustomerHealthExportResponse healthExport,
        CustomerHealthBundleResponse healthBundle,
        CustomerOperationsWorkspaceExportResponse workspaceExport,
        CustomerOperationsWorkspaceBundleResponse workspaceBundle,
        CustomerOverviewExportResponse overviewExport,
        CustomerOverviewBundleResponse overviewBundle,
        CustomerDeliveryCenterExportResponse deliveryPackageExport,
        CustomerDeliveryCenterBundleResponse deliveryPackageBundle,
        CustomerReportExportResponse reportExport,
        CustomerReportBundleResponse reportBundle,
        CustomerAccountCenterExportResponse accountExport,
        CustomerAccountCenterBundleResponse accountBundle) {
}
