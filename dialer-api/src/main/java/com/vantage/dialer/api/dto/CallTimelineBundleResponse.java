package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CallTimelineBundleResponse(
        String callSessionId,
        String bundleDirectoryPath,
        String sessionJsonPath,
        String timelineJsonPath,
        String timelineCsvPath,
        String timelineMarkdownPath,
        String timelineHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
