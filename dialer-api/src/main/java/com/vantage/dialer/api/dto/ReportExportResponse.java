package com.vantage.dialer.api.dto;

import java.time.Instant;

public record ReportExportResponse(
        String exportJobId,
        String exportType,
        String status,
        String filePath,
        long rowCount,
        Instant createdAt,
        Instant completedAt,
        String errorMessage) {
}
