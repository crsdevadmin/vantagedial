package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerReportExportResponse(
        String customerId,
        String exportDirectory,
        String reportJsonPath,
        String reportHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
