package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record PlatformArtifactCatalogBundleResponse(
        String bundleDirectory,
        String catalogJsonPath,
        String catalogHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
