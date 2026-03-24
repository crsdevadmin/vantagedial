package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record InstallationWorkspaceBundleResponse(
        String customerId,
        String bundleDirectory,
        String manifestPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
