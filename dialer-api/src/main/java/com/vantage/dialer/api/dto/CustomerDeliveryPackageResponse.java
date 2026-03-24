package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerDeliveryPackageResponse(
        String installationJobId,
        String customerId,
        String packageDirectory,
        String manifestPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
