package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerPortfolioExportResponse(
        String exportDirectory,
        String portfolioJsonPath,
        String portfolioHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
