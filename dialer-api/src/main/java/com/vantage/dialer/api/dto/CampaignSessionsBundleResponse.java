package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CampaignSessionsBundleResponse(
        String campaignId,
        String bundleDirectoryPath,
        String sessionsJsonPath,
        String sessionsCsvPath,
        String sessionsMarkdownPath,
        String sessionsHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
