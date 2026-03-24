package com.vantage.dialer.api.dto;

import java.time.Instant;

public record PlatformReportExportResponse(
        String exportDirectory,
        String reportJsonPath,
        String reportHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
