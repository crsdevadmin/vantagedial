package com.vantage.dialer.api.dto;

import java.time.Instant;

public record PlatformWorkspaceExportResponse(
        String exportDirectory,
        String workspaceJsonPath,
        String workspaceHtmlPath,
        String readmePath,
        Instant generatedAt) {
}
