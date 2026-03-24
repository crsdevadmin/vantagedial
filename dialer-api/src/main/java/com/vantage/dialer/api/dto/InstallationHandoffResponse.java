package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationHandoffResponse(
        String installationJobId,
        String customerId,
        String installationName,
        Instant generatedAt,
        CustomerInstallationResponse installation,
        CustomerBootstrapBundleResponse bootstrapBundle,
        InstallationQuoteSummaryResponse quoteSummary,
        QuoteSnapshotDashboardResponse quoteDashboard) {
}
