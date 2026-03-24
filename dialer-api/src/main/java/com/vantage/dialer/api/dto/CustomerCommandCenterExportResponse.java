package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerCommandCenterExportResponse(
        String exportDirectory,
        String commandCenterJsonPath,
        String commandCenterHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
