package com.vantage.dialer.api.dto;

import java.time.Instant;

public record IvrCampaignBundleResponse(
        String campaignId,
        Instant from,
        Instant to,
        String bundleDirectoryPath,
        String summaryJsonPath,
        String summaryMarkdownPath,
        String summaryHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
