package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerAccountCenterBundleResponse(
        String customerId,
        String bundleDirectory,
        String accountJsonPath,
        String accountHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
