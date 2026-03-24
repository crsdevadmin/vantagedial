package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record PlatformControlCenterBundleResponse(
        String bundleDirectory,
        String controlCenterJsonPath,
        String controlCenterHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
