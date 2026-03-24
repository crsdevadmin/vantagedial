package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationReportExportResponse(
        String customerId,
        String exportDirectory,
        String reportJsonPath,
        String reportHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
