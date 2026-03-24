package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationHandoffExportResponse(
        String installationJobId,
        String exportDirectory,
        String handoffJsonPath,
        String handoffHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
