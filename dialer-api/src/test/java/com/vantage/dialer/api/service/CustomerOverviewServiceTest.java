package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerOverviewBundleResponse;
import com.vantage.dialer.api.dto.CustomerOverviewResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerOverviewServiceTest {

    private static final String CUSTOMER_ID = "customer-1";

    @Test
    void overviewAggregatesHealthWorkspaceAndAccountCenter() {
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOperationsWorkspaceService customerOperationsWorkspaceService = mock(CustomerOperationsWorkspaceService.class);
        CustomerAccountCenterService customerAccountCenterService = mock(CustomerAccountCenterService.class);
        CustomerOverviewService service = service(
                customerHealthService,
                customerOperationsWorkspaceService,
                customerAccountCenterService,
                "./build/test-exports"
        );

        stubOverviewDependencies(customerHealthService, customerOperationsWorkspaceService, customerAccountCenterService);

        CustomerOverviewResponse overview = service.overview(CUSTOMER_ID);

        assertFalse(overview.healthy());
        assertTrue(overview.hasReport());
        assertTrue(overview.hasArtifactCatalog());
        assertEquals("Needs attention", overview.statusMessage());
        assertEquals("install-1", overview.latestInstallationJobId());
        assertEquals("Acme Softphone", overview.latestInstallationName());
        assertTrue(overview.accountCenter().hasDeliveryPackage());
    }

    @Test
    void bundleWritesCustomerOverviewFiles(@TempDir Path tempDir) throws Exception {
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOperationsWorkspaceService customerOperationsWorkspaceService = mock(CustomerOperationsWorkspaceService.class);
        CustomerAccountCenterService customerAccountCenterService = mock(CustomerAccountCenterService.class);
        CustomerOverviewService service = service(
                customerHealthService,
                customerOperationsWorkspaceService,
                customerAccountCenterService,
                tempDir.toString()
        );

        stubOverviewDependencies(customerHealthService, customerOperationsWorkspaceService, customerAccountCenterService);

        CustomerOverviewBundleResponse bundle = service.generateBundle(CUSTOMER_ID);

        JsonNode json = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.overviewJsonPath())));
        String html = Files.readString(Path.of(bundle.overviewHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals(CUSTOMER_ID, json.get("customerId").asText());
        assertTrue(json.get("hasArtifactCatalog").asBoolean());
        assertTrue(readme.contains("Artifact catalog ready: true"));
        assertTrue(readme.contains("Latest installation name: Acme Softphone"));
        assertTrue(html.contains("Customer account has delivery package: YES"));
        assertTrue(html.contains("Customer artifact catalog ready: YES"));
    }

    private void stubOverviewDependencies(CustomerHealthService customerHealthService,
                                          CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                          CustomerAccountCenterService customerAccountCenterService) {
        when(customerHealthService.health(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerHealthResponse(CUSTOMER_ID)
        );
        when(customerOperationsWorkspaceService.workspace(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOperationsWorkspaceResponse(CUSTOMER_ID)
        );
        when(customerAccountCenterService.account(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerAccountCenterResponse(CUSTOMER_ID)
        );
    }

    private CustomerOverviewService service(CustomerHealthService customerHealthService,
                                            CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                            CustomerAccountCenterService customerAccountCenterService,
                                            String exportDirectory) {
        return new CustomerOverviewService(
                customerHealthService,
                customerOperationsWorkspaceService,
                customerAccountCenterService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
