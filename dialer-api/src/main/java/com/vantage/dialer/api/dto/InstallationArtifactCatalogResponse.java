package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationArtifactCatalogResponse(
        String customerId,
        Instant generatedAt,
        InstallationDashboardExportResponse dashboardExport,
        InstallationTimelineExportResponse timelineExport,
        InstallationHealthExportResponse healthExport,
        InstallationReportExportResponse reportExport,
        InstallationOverviewExportResponse overviewExport,
        InstallationWorkspaceExportResponse workspaceExport,
        InstallationWorkspaceBundleResponse workspaceBundle) {
}
