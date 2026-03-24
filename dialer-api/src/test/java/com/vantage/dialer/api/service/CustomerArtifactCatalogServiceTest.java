package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerArtifactCatalogServiceTest {

    private static final String CUSTOMER_ID = "customer-1";

    @Test
    void catalogAggregatesCustomerHealthAndIncludedExports() {
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOperationsWorkspaceService customerOperationsWorkspaceService = mock(CustomerOperationsWorkspaceService.class);
        CustomerOverviewService customerOverviewService = mock(CustomerOverviewService.class);
        CustomerDeliveryCenterService customerDeliveryCenterService = mock(CustomerDeliveryCenterService.class);
        CustomerReportService customerReportService = mock(CustomerReportService.class);
        CustomerAccountCenterService customerAccountCenterService = mock(CustomerAccountCenterService.class);
        CustomerArtifactCatalogService service = service(
                customerHealthService,
                customerOperationsWorkspaceService,
                customerOverviewService,
                customerDeliveryCenterService,
                customerReportService,
                customerAccountCenterService,
                "./build/test-exports"
        );

        stubCatalogDependencies(
                customerHealthService,
                customerOperationsWorkspaceService,
                customerOverviewService,
                customerDeliveryCenterService,
                customerReportService,
                customerAccountCenterService
        );

        CustomerArtifactCatalogResponse catalog = service.catalog(CUSTOMER_ID);

        assertFalse(catalog.healthy());
        assertEquals("Needs attention", catalog.statusMessage());
        assertEquals("install-1", catalog.latestInstallationJobId());
        assertEquals("Acme Softphone", catalog.latestInstallationName());
        assertEquals("delivery-center-export", catalog.deliveryPackageExport().exportDirectory());
        assertEquals("report-bundle", catalog.reportBundle().bundleDirectory());
        assertEquals("account-export", catalog.accountExport().exportDirectory());
    }

    @Test
    void exportAndBundleWriteCustomerArtifactFiles(@TempDir Path tempDir) throws Exception {
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOperationsWorkspaceService customerOperationsWorkspaceService = mock(CustomerOperationsWorkspaceService.class);
        CustomerOverviewService customerOverviewService = mock(CustomerOverviewService.class);
        CustomerDeliveryCenterService customerDeliveryCenterService = mock(CustomerDeliveryCenterService.class);
        CustomerReportService customerReportService = mock(CustomerReportService.class);
        CustomerAccountCenterService customerAccountCenterService = mock(CustomerAccountCenterService.class);
        CustomerArtifactCatalogService service = service(
                customerHealthService,
                customerOperationsWorkspaceService,
                customerOverviewService,
                customerDeliveryCenterService,
                customerReportService,
                customerAccountCenterService,
                tempDir.toString()
        );

        stubCatalogDependencies(
                customerHealthService,
                customerOperationsWorkspaceService,
                customerOverviewService,
                customerDeliveryCenterService,
                customerReportService,
                customerAccountCenterService
        );

        CustomerArtifactCatalogBundleResponse bundle = service.generateBundle(CUSTOMER_ID);
        CustomerArtifactCatalogExportResponse export = service.export(CUSTOMER_ID);

        JsonNode bundleJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.catalogJsonPath())));
        JsonNode exportJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(export.catalogJsonPath())));
        String html = Files.readString(Path.of(export.catalogHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals(CUSTOMER_ID, bundleJson.get("customerId").asText());
        assertEquals("health-export", bundleJson.get("healthExport").get("exportDirectory").asText());
        assertEquals("workspace-bundle", bundleJson.get("workspaceBundle").get("bundleDirectory").asText());
        assertEquals("account-bundle", exportJson.get("accountBundle").get("bundleDirectory").asText());
        assertTrue(readme.contains("Latest installation name: Acme Softphone"));
        assertTrue(readme.contains("Latest quote snapshot: quote-1"));
        assertTrue(html.contains("Customer delivery package export: delivery-center-export"));
        assertTrue(html.contains("Customer account export: account-export"));
    }

    private void stubCatalogDependencies(CustomerHealthService customerHealthService,
                                         CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                         CustomerOverviewService customerOverviewService,
                                         CustomerDeliveryCenterService customerDeliveryCenterService,
                                         CustomerReportService customerReportService,
                                         CustomerAccountCenterService customerAccountCenterService) {
        when(customerHealthService.health(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerHealthResponse(CUSTOMER_ID)
        );
        when(customerHealthService.export(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerHealthExportResponse(CUSTOMER_ID, "health-export")
        );
        when(customerHealthService.generateBundle(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerHealthBundleResponse(CUSTOMER_ID, "health-bundle")
        );
        when(customerOperationsWorkspaceService.export(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOperationsWorkspaceExportResponse(CUSTOMER_ID, "workspace-export")
        );
        when(customerOperationsWorkspaceService.generateBundle(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOperationsWorkspaceBundleResponse(CUSTOMER_ID, "workspace-bundle")
        );
        when(customerOverviewService.export(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOverviewExportResponse(CUSTOMER_ID, "overview-export")
        );
        when(customerOverviewService.generateBundle(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOverviewBundleResponse(CUSTOMER_ID, "overview-bundle")
        );
        when(customerDeliveryCenterService.export(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerDeliveryCenterExportResponse(CUSTOMER_ID, "delivery-center-export")
        );
        when(customerDeliveryCenterService.generateBundle(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerDeliveryCenterBundleResponse(CUSTOMER_ID, "delivery-center-bundle")
        );
        when(customerReportService.export(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerReportExportResponse(CUSTOMER_ID, "report-export")
        );
        when(customerReportService.generateBundle(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerReportBundleResponse(CUSTOMER_ID, "report-bundle")
        );
        when(customerAccountCenterService.export(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerAccountCenterExportResponse(CUSTOMER_ID, "account-export")
        );
        when(customerAccountCenterService.generateBundle(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerAccountCenterBundleResponse(CUSTOMER_ID, "account-bundle")
        );
    }

    private CustomerArtifactCatalogService service(CustomerHealthService customerHealthService,
                                                   CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                                   CustomerOverviewService customerOverviewService,
                                                   CustomerDeliveryCenterService customerDeliveryCenterService,
                                                   CustomerReportService customerReportService,
                                                   CustomerAccountCenterService customerAccountCenterService,
                                                   String exportDirectory) {
        return new CustomerArtifactCatalogService(
                customerHealthService,
                customerOperationsWorkspaceService,
                customerOverviewService,
                customerDeliveryCenterService,
                customerReportService,
                customerAccountCenterService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
