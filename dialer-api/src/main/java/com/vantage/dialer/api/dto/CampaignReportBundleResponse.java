package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CampaignReportBundleResponse(
        String campaignId,
        Instant from,
        Instant to,
        String bundleDirectoryPath,
        String summaryJsonPath,
        String ivrSummaryJsonPath,
        String sessionsJsonPath,
        String reportMarkdownPath,
        String reportHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
