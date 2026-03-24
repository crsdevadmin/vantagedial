package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record PlatformDeliveryPackageResponse(
        String packageDirectory,
        String manifestPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
