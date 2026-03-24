package com.vantage.dialer.api.dto;

import java.time.Instant;

public record InstallationWorkspaceExportResponse(
        String customerId,
        String exportDirectory,
        String workspaceJsonPath,
        String workspaceHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
