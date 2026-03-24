package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerHealthExportResponse(
        String customerId,
        String exportDirectory,
        String healthJsonPath,
        String healthHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
