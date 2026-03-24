package com.vantage.dialer.api.dto;

import java.time.Instant;

public record PlatformDeliveryPackageExportResponse(
        String exportDirectory,
        String packageJsonPath,
        String packageHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
