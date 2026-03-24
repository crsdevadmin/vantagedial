package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerOverviewResponse(
        String customerId,
        Instant generatedAt,
        CustomerHealthResponse health,
        CustomerOperationsWorkspaceResponse workspace,
        CustomerAccountCenterResponse accountCenter,
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
