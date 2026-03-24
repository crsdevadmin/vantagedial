package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record InstallationReportResponse(
        String customerId,
        Instant generatedAt,
        InstallationDashboardResponse dashboard,
        CustomerInstallationResponse latestInstallation,
        List<InstallationTimelineEntryResponse> timeline) {
}
