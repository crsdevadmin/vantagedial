package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceExportResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerOperationsWorkspaceServiceTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String INSTALLATION_JOB_ID = "install-1";

    @Test
    void workspaceCombinesInstallationsQuoteSummaryAndLatestDeliveryPackage() {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        InstallationDashboardService installationDashboardService = mock(InstallationDashboardService.class);
        QuoteSnapshotService quoteSnapshotService = mock(QuoteSnapshotService.class);
        CustomerDeliveryPackageService deliveryPackageService = mock(CustomerDeliveryPackageService.class);
        CustomerOperationsWorkspaceService service = service(
                installationService,
                installationDashboardService,
                quoteSnapshotService,
                deliveryPackageService,
                "./build/test-exports"
        );

        stubWorkspaceDependencies(installationService, installationDashboardService, quoteSnapshotService, deliveryPackageService);

        CustomerOperationsWorkspaceResponse workspace = service.workspace(CUSTOMER_ID);

        assertTrue(workspace.healthy());
        assertTrue(workspace.hasReport());
        assertTrue(workspace.hasArtifactCatalog());
        assertEquals("Customer workspace is healthy", workspace.statusMessage());
        assertEquals(INSTALLATION_JOB_ID, workspace.latestInstallationJobId());
        assertEquals("Acme Softphone", workspace.latestInstallationName());
        assertEquals("quote-1", workspace.latestQuoteSnapshotId());
        assertEquals(149.50, workspace.latestSuggestedSellPrice());
        assertEquals("workspace-bundle", workspace.latestDeliveryPackage().artifactCatalog().workspaceBundle().bundleDirectory());
    }

    @Test
    void bundleAndExportWriteWorkspaceFiles(@TempDir Path tempDir) throws Exception {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        InstallationDashboardService installationDashboardService = mock(InstallationDashboardService.class);
        QuoteSnapshotService quoteSnapshotService = mock(QuoteSnapshotService.class);
        CustomerDeliveryPackageService deliveryPackageService = mock(CustomerDeliveryPackageService.class);
        CustomerOperationsWorkspaceService service = service(
                installationService,
                installationDashboardService,
                quoteSnapshotService,
                deliveryPackageService,
                tempDir.toString()
        );

        stubWorkspaceDependencies(installationService, installationDashboardService, quoteSnapshotService, deliveryPackageService);

        CustomerOperationsWorkspaceBundleResponse bundle = service.generateBundle(CUSTOMER_ID);
        CustomerOperationsWorkspaceExportResponse export = service.export(CUSTOMER_ID);

        JsonNode bundleJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.workspaceJsonPath())));
        JsonNode exportJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(export.workspaceJsonPath())));
        String bundleReadme = Files.readString(Path.of(bundle.readmePath()));
        String exportHtml = Files.readString(Path.of(export.workspaceHtmlPath()));

        assertEquals(CUSTOMER_ID, bundleJson.get("customerId").asText());
        assertEquals("quote-1", bundleJson.get("latestQuoteSnapshotId").asText());
        assertEquals("Acme Softphone", exportJson.get("latestInstallationName").asText());
        assertTrue(bundleReadme.contains("healthy: true"));
        assertTrue(bundleReadme.contains("report ready: true"));
        assertTrue(exportHtml.contains("Delivery Package"));
        assertTrue(exportHtml.contains("Customer workspace is healthy"));
    }

    private void stubWorkspaceDependencies(CustomerInstallationService installationService,
                                           InstallationDashboardService installationDashboardService,
                                           QuoteSnapshotService quoteSnapshotService,
                                           CustomerDeliveryPackageService deliveryPackageService) {
        when(installationService.list(CUSTOMER_ID)).thenReturn(
                List.of(CustomerServiceTestFixtures.customerInstallationResponse(INSTALLATION_JOB_ID, CUSTOMER_ID))
        );
        when(installationDashboardService.overview(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.installationWorkspaceResponse(CUSTOMER_ID).overview()
        );
        when(quoteSnapshotService.summary(null, CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.quoteSnapshotSummaryResponse(CUSTOMER_ID)
        );
        when(deliveryPackageService.detail(INSTALLATION_JOB_ID)).thenReturn(
                CustomerServiceTestFixtures.customerDeliveryPackageDetailResponse(CUSTOMER_ID, INSTALLATION_JOB_ID)
        );
    }

    private CustomerOperationsWorkspaceService service(CustomerInstallationService installationService,
                                                       InstallationDashboardService installationDashboardService,
                                                       QuoteSnapshotService quoteSnapshotService,
                                                       CustomerDeliveryPackageService deliveryPackageService,
                                                       String exportDirectory) {
        return new CustomerOperationsWorkspaceService(
                installationService,
                installationDashboardService,
                quoteSnapshotService,
                deliveryPackageService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
