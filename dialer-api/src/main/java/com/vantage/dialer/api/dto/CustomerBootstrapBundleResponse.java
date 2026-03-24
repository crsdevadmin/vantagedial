package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerBootstrapBundleResponse(
        String installationJobId,
        String installationName,
        String bundleDirectory,
        String summaryPath,
        String customerConfigPath,
        String commercialProfilePath,
        String appStackEnvPath,
        String agentInventoryPath,
        String softphoneEnvPath,
        String uiConnectionPath,
        String asteriskHandoffPath,
        String readmePath,
        Instant generatedAt,
        List<String> files) {
}
