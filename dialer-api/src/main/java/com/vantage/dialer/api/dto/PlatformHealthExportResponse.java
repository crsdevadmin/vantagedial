package com.vantage.dialer.api.dto;

import java.time.Instant;

public record PlatformHealthExportResponse(
        String exportDirectory,
        String healthJsonPath,
        String healthHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
