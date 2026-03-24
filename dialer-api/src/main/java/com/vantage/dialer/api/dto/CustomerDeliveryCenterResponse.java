package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerDeliveryCenterResponse(
        String customerId,
        Instant generatedAt,
        CustomerHealthResponse health,
        CustomerOverviewResponse overview,
        CustomerAccountCenterResponse accountCenter,
        CustomerArtifactCatalogResponse artifactCatalog,
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
