package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerDeliveryCenterBundleResponse(
        String customerId,
        String bundleDirectory,
        String deliveryJsonPath,
        String deliveryHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
