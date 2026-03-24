package com.vantage.dialer.api.dto;

import java.time.Instant;

public record PlatformControlCenterExportResponse(
        String exportDirectory,
        String controlCenterJsonPath,
        String controlCenterHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
