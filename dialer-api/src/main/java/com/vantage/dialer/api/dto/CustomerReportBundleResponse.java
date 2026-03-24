package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerReportBundleResponse(
        String customerId,
        String bundleDirectory,
        String reportJsonPath,
        String reportHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
