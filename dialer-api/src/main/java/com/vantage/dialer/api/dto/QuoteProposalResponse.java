package com.vantage.dialer.api.dto;

import java.time.Instant;

public record QuoteProposalResponse(
        String quoteSnapshotId,
        String proposalDirectoryPath,
        String proposalMarkdownPath,
        String proposalHtmlPath,
        String assumptionsJsonPath,
        String pricingBreakdownJsonPath,
        Instant generatedAt) {
}
