package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerDeliveryCenterServiceTest {

    private static final String CUSTOMER_ID = "customer-1";

    @Test
    void deliveryCenterAggregatesHealthOverviewAccountAndArtifactCatalog() {
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOverviewService customerOverviewService = mock(CustomerOverviewService.class);
        CustomerAccountCenterService customerAccountCenterService = mock(CustomerAccountCenterService.class);
        CustomerArtifactCatalogService customerArtifactCatalogService = mock(CustomerArtifactCatalogService.class);
        CustomerDeliveryCenterService service = service(
                customerHealthService,
                customerOverviewService,
                customerAccountCenterService,
                customerArtifactCatalogService,
                "./build/test-exports"
        );

        stubDeliveryCenterDependencies(
                customerHealthService,
                customerOverviewService,
                customerAccountCenterService,
                customerArtifactCatalogService
        );

        CustomerDeliveryCenterResponse detail = service.deliveryPackage(CUSTOMER_ID);

        assertFalse(detail.healthy());
        assertTrue(detail.hasReport());
        assertTrue(detail.hasArtifactCatalog());
        assertEquals("Needs attention", detail.statusMessage());
        assertEquals("install-1", detail.latestInstallationJobId());
        assertEquals("Acme Softphone", detail.latestInstallationName());
        assertEquals("workspace-export", detail.artifactCatalog().workspaceExport().exportDirectory());
    }

    @Test
    void bundleWritesCustomerDeliveryCenterFiles(@TempDir Path tempDir) throws Exception {
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOverviewService customerOverviewService = mock(CustomerOverviewService.class);
        CustomerAccountCenterService customerAccountCenterService = mock(CustomerAccountCenterService.class);
        CustomerArtifactCatalogService customerArtifactCatalogService = mock(CustomerArtifactCatalogService.class);
        CustomerDeliveryCenterService service = service(
                customerHealthService,
                customerOverviewService,
                customerAccountCenterService,
                customerArtifactCatalogService,
                tempDir.toString()
        );

        stubDeliveryCenterDependencies(
                customerHealthService,
                customerOverviewService,
                customerAccountCenterService,
                customerArtifactCatalogService
        );

        CustomerDeliveryCenterBundleResponse bundle = service.generateBundle(CUSTOMER_ID);

        JsonNode json = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.deliveryJsonPath())));
        String html = Files.readString(Path.of(bundle.deliveryHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals(CUSTOMER_ID, json.get("customerId").asText());
        assertEquals("workspace-export", json.get("artifactCatalog").get("workspaceExport").get("exportDirectory").asText());
        assertTrue(readme.contains("artifact catalog ready: true"));
        assertTrue(readme.contains("customer artifact catalog"));
        assertTrue(html.contains("Artifact catalog export root: workspace-export"));
        assertTrue(html.contains("Account center includes delivery package: YES"));
    }

    private void stubDeliveryCenterDependencies(CustomerHealthService customerHealthService,
                                                CustomerOverviewService customerOverviewService,
                                                CustomerAccountCenterService customerAccountCenterService,
                                                CustomerArtifactCatalogService customerArtifactCatalogService) {
        when(customerHealthService.health(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerHealthResponse(CUSTOMER_ID)
        );
        when(customerOverviewService.overview(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOverviewResponse(CUSTOMER_ID)
        );
        when(customerAccountCenterService.account(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerAccountCenterResponse(CUSTOMER_ID)
        );
        when(customerArtifactCatalogService.catalog(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerArtifactCatalogResponse(CUSTOMER_ID)
        );
    }

    private CustomerDeliveryCenterService service(CustomerHealthService customerHealthService,
                                                  CustomerOverviewService customerOverviewService,
                                                  CustomerAccountCenterService customerAccountCenterService,
                                                  CustomerArtifactCatalogService customerArtifactCatalogService,
                                                  String exportDirectory) {
        return new CustomerDeliveryCenterService(
                customerHealthService,
                customerOverviewService,
                customerAccountCenterService,
                customerArtifactCatalogService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
