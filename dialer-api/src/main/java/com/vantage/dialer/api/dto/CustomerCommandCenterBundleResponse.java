package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerCommandCenterBundleResponse(
        String bundleDirectory,
        String commandCenterJsonPath,
        String commandCenterHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
