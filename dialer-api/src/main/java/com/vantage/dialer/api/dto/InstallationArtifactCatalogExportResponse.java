package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationArtifactCatalogExportResponse(
        String customerId,
        String exportDirectory,
        String catalogJsonPath,
        String catalogHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
