package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerPortfolioBundleResponse(
        String bundleDirectory,
        String portfolioJsonPath,
        String portfolioHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
