package com.vantage.dialer.api.dto;

import java.time.Instant;

public record AgentActivityBundleResponse(
        Instant from,
        Instant to,
        String bundleDirectoryPath,
        String activityJsonPath,
        String activityCsvPath,
        String activityMarkdownPath,
        String activityHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
