package com.vantage.dialer.api.dto;

import java.time.Instant;

public record CustomerDeliveryPackageDetailResponse(
        String installationJobId,
        String customerId,
        Instant generatedAt,
        CustomerInstallationResponse installation,
        InstallationHandoffResponse handoff,
        InstallationWorkspaceResponse workspace,
        InstallationArtifactCatalogResponse artifactCatalog) {
}
