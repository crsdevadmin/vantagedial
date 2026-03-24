package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerAccountCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerAccountCenterServiceTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String INSTALLATION_JOB_ID = "install-1";

    @Test
    void accountCombinesWorkspaceQuoteDashboardAndLatestDelivery() {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOperationsWorkspaceService customerOperationsWorkspaceService = mock(CustomerOperationsWorkspaceService.class);
        InstallationDashboardService installationDashboardService = mock(InstallationDashboardService.class);
        QuoteSnapshotService quoteSnapshotService = mock(QuoteSnapshotService.class);
        CustomerDeliveryPackageService deliveryPackageService = mock(CustomerDeliveryPackageService.class);
        CustomerAccountCenterService service = service(
                installationService,
                customerHealthService,
                customerOperationsWorkspaceService,
                installationDashboardService,
                quoteSnapshotService,
                deliveryPackageService,
                "./build/test-exports"
        );

        stubAccountDependencies(
                installationService,
                customerHealthService,
                customerOperationsWorkspaceService,
                installationDashboardService,
                quoteSnapshotService,
                deliveryPackageService
        );

        CustomerAccountCenterResponse account = service.account(CUSTOMER_ID);

        assertTrue(account.hasQuotes());
        assertTrue(account.hasDeliveryPackage());
        assertTrue(account.hasReport());
        assertTrue(account.hasArtifactCatalog());
        assertEquals("install-1", account.latestInstallationJobId());
        assertEquals("Acme Softphone", account.latestInstallationName());
        assertEquals("quote-1", account.latestQuoteSnapshotId());
        assertEquals(149.50, account.latestSuggestedSellPrice());
        assertEquals(1, account.quoteDashboard().timeline().size());
    }

    @Test
    void bundleAndExportWriteAccountCenterFiles(@TempDir Path tempDir) throws Exception {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOperationsWorkspaceService customerOperationsWorkspaceService = mock(CustomerOperationsWorkspaceService.class);
        InstallationDashboardService installationDashboardService = mock(InstallationDashboardService.class);
        QuoteSnapshotService quoteSnapshotService = mock(QuoteSnapshotService.class);
        CustomerDeliveryPackageService deliveryPackageService = mock(CustomerDeliveryPackageService.class);
        CustomerAccountCenterService service = service(
                installationService,
                customerHealthService,
                customerOperationsWorkspaceService,
                installationDashboardService,
                quoteSnapshotService,
                deliveryPackageService,
                tempDir.toString()
        );

        stubAccountDependencies(
                installationService,
                customerHealthService,
                customerOperationsWorkspaceService,
                installationDashboardService,
                quoteSnapshotService,
                deliveryPackageService
        );

        CustomerAccountCenterBundleResponse bundle = service.generateBundle(CUSTOMER_ID);
        CustomerAccountCenterExportResponse export = service.export(CUSTOMER_ID);

        JsonNode bundleJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.accountJsonPath())));
        JsonNode exportJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(export.accountJsonPath())));
        String html = Files.readString(Path.of(export.accountHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals(CUSTOMER_ID, bundleJson.get("customerId").asText());
        assertTrue(bundleJson.get("hasDeliveryPackage").asBoolean());
        assertEquals("quote-1", exportJson.get("latestQuoteSnapshotId").asText());
        assertTrue(readme.contains("latest delivery package details"));
        assertTrue(readme.contains("Latest installation name: Acme Softphone"));
        assertTrue(html.contains("Timeline entries: 1"));
        assertTrue(html.contains("Delivery"));
    }

    private void stubAccountDependencies(CustomerInstallationService installationService,
                                         CustomerHealthService customerHealthService,
                                         CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                         InstallationDashboardService installationDashboardService,
                                         QuoteSnapshotService quoteSnapshotService,
                                         CustomerDeliveryPackageService deliveryPackageService) {
        when(installationService.list(CUSTOMER_ID)).thenReturn(
                List.of(CustomerServiceTestFixtures.customerInstallationResponse(INSTALLATION_JOB_ID, CUSTOMER_ID))
        );
        when(customerHealthService.health(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerHealthResponse(CUSTOMER_ID)
        );
        when(customerOperationsWorkspaceService.workspace(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOperationsWorkspaceResponse(CUSTOMER_ID)
        );
        when(installationDashboardService.workspace(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.installationWorkspaceResponse(CUSTOMER_ID)
        );
        when(quoteSnapshotService.dashboard(null, CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.quoteSnapshotDashboardResponse(CUSTOMER_ID)
        );
        when(deliveryPackageService.detail(INSTALLATION_JOB_ID)).thenReturn(
                CustomerServiceTestFixtures.customerDeliveryPackageDetailResponse(CUSTOMER_ID, INSTALLATION_JOB_ID)
        );
    }

    private CustomerAccountCenterService service(CustomerInstallationService installationService,
                                                 CustomerHealthService customerHealthService,
                                                 CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                                 InstallationDashboardService installationDashboardService,
                                                 QuoteSnapshotService quoteSnapshotService,
                                                 CustomerDeliveryPackageService deliveryPackageService,
                                                 String exportDirectory) {
        return new CustomerAccountCenterService(
                installationService,
                customerHealthService,
                customerOperationsWorkspaceService,
                installationDashboardService,
                quoteSnapshotService,
                deliveryPackageService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
