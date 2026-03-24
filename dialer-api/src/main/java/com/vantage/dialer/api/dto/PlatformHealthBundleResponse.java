package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record PlatformHealthBundleResponse(
        String bundleDirectory,
        String healthJsonPath,
        String healthHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
