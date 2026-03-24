package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerDeliveryPackageExportResponse(
        String installationJobId,
        String exportDirectory,
        String packageJsonPath,
        String packageHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
