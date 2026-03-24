package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerAccountCenterExportResponse(
        String customerId,
        String exportDirectory,
        String accountJsonPath,
        String accountHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
