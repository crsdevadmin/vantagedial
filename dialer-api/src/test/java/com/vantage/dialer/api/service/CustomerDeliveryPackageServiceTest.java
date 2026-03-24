package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageExportResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerDeliveryPackageServiceTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String INSTALLATION_JOB_ID = "install-1";

    @Test
    void detailIncludesInstallationHandoffWorkspaceAndArtifactCatalog() {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        InstallationHandoffBundleService handoffBundleService = mock(InstallationHandoffBundleService.class);
        InstallationDashboardService installationDashboardService = mock(InstallationDashboardService.class);
        CustomerDeliveryPackageService service = service(
                installationService,
                handoffBundleService,
                installationDashboardService,
                "./build/test-exports"
        );

        stubDeliveryDependencies(installationService, handoffBundleService, installationDashboardService);

        CustomerDeliveryPackageDetailResponse detail = service.detail(INSTALLATION_JOB_ID);

        assertEquals(INSTALLATION_JOB_ID, detail.installationJobId());
        assertEquals(CUSTOMER_ID, detail.customerId());
        assertEquals("SOFTPHONE", detail.handoff().installation().clientType());
        assertEquals(1, detail.workspace().dashboard().totalInstallations());
        assertEquals("workspace-bundle", detail.artifactCatalog().workspaceBundle().bundleDirectory());
    }

    @Test
    void generateAndExportWriteManifestAndCustomerFacingFiles(@TempDir Path tempDir) throws Exception {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        InstallationHandoffBundleService handoffBundleService = mock(InstallationHandoffBundleService.class);
        InstallationDashboardService installationDashboardService = mock(InstallationDashboardService.class);
        CustomerDeliveryPackageService service = service(
                installationService,
                handoffBundleService,
                installationDashboardService,
                tempDir.toString()
        );

        stubDeliveryDependencies(installationService, handoffBundleService, installationDashboardService);

        CustomerDeliveryPackageResponse bundle = service.generate(INSTALLATION_JOB_ID);
        CustomerDeliveryPackageExportResponse export = service.export(INSTALLATION_JOB_ID);

        JsonNode manifest = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.manifestPath())));
        JsonNode exportJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(export.packageJsonPath())));
        String bundleReadme = Files.readString(Path.of(bundle.readmePath()));
        String exportHtml = Files.readString(Path.of(export.packageHtmlPath()));

        assertEquals(CUSTOMER_ID, manifest.get("installation").get("customerId").asText());
        assertEquals("handoff-bundle", manifest.get("handoffBundle").get("bundleDirectory").asText());
        assertEquals("artifact-catalog-bundle", manifest.get("artifactCatalogBundle").get("bundleDirectory").asText());
        assertEquals("workspace-bundle", exportJson.get("artifactCatalog").get("workspaceBundle").get("bundleDirectory").asText());
        assertTrue(bundleReadme.contains("This delivery package references:"));
        assertTrue(bundleReadme.contains("installation artifact catalog bundle"));
        assertTrue(exportHtml.contains("Handoff client type: SOFTPHONE"));
        assertTrue(exportHtml.contains("Artifact catalog workspace bundle: workspace-bundle"));
    }

    private void stubDeliveryDependencies(CustomerInstallationService installationService,
                                          InstallationHandoffBundleService handoffBundleService,
                                          InstallationDashboardService installationDashboardService) {
        when(installationService.get(INSTALLATION_JOB_ID)).thenReturn(
                CustomerServiceTestFixtures.customerInstallationResponse(INSTALLATION_JOB_ID, CUSTOMER_ID)
        );
        when(handoffBundleService.generate(INSTALLATION_JOB_ID)).thenReturn(
                CustomerServiceTestFixtures.installationHandoffBundleResponse(INSTALLATION_JOB_ID, "handoff-bundle")
        );
        when(handoffBundleService.export(INSTALLATION_JOB_ID)).thenReturn(
                CustomerServiceTestFixtures.installationHandoffExportResponse(INSTALLATION_JOB_ID, "handoff-export")
        );
        when(handoffBundleService.handoff(INSTALLATION_JOB_ID)).thenReturn(
                CustomerServiceTestFixtures.installationHandoffResponse(INSTALLATION_JOB_ID, CUSTOMER_ID)
        );
        when(installationDashboardService.generateWorkspaceBundle(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.installationWorkspaceBundleResponse(CUSTOMER_ID, "workspace-bundle")
        );
        when(installationDashboardService.exportWorkspace(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.installationWorkspaceExportResponse(CUSTOMER_ID, "workspace-export")
        );
        when(installationDashboardService.workspace(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.installationWorkspaceResponse(CUSTOMER_ID)
        );
        when(installationDashboardService.generateArtifactCatalog(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.installationArtifactCatalogResponse(CUSTOMER_ID)
        );
        when(installationDashboardService.generateArtifactCatalogBundle(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.installationArtifactCatalogBundleResponse(CUSTOMER_ID, "artifact-catalog-bundle")
        );
    }

    private CustomerDeliveryPackageService service(CustomerInstallationService installationService,
                                                   InstallationHandoffBundleService handoffBundleService,
                                                   InstallationDashboardService installationDashboardService,
                                                   String exportDirectory) {
        return new CustomerDeliveryPackageService(
                installationService,
                handoffBundleService,
                installationDashboardService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
