package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerOperationsWorkspaceBundleResponse(
        String customerId,
        String bundleDirectory,
        String workspaceJsonPath,
        String workspaceHtmlPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
