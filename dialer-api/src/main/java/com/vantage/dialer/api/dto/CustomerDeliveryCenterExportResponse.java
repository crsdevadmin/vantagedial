package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerDeliveryCenterExportResponse(
        String customerId,
        String exportDirectory,
        String deliveryJsonPath,
        String deliveryHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
