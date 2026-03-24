package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioBundleResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioEntryResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioExportResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioResponse;
import com.vantage.dialer.api.dto.InstallationDashboardResponse;
import com.vantage.dialer.api.dto.InstallationHealthResponse;
import com.vantage.dialer.api.dto.InstallationOverviewResponse;
import com.vantage.dialer.api.dto.InstallationReportResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerPortfolioServiceTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-03-22T12:00:00Z");
    private static final String CUSTOMER_ID = "customer-1";

    @Test
    void portfolioAggregatesCustomersAndNormalizesUnassigned() {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOperationsWorkspaceService customerOperationsWorkspaceService = mock(CustomerOperationsWorkspaceService.class);
        CustomerPortfolioService service = service(
                installationService,
                customerHealthService,
                customerOperationsWorkspaceService,
                "./build/test-exports"
        );

        when(installationService.list(null)).thenReturn(List.of(
                CustomerServiceTestFixtures.customerInstallationResponse("install-1", CUSTOMER_ID),
                CustomerServiceTestFixtures.customerInstallationResponse("install-2", null),
                CustomerServiceTestFixtures.customerInstallationResponse("install-3", CUSTOMER_ID)
        ));
        when(customerOperationsWorkspaceService.workspace(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOperationsWorkspaceResponse(CUSTOMER_ID)
        );
        when(customerOperationsWorkspaceService.workspace(null)).thenReturn(unassignedWorkspace());
        when(customerHealthService.health(CUSTOMER_ID)).thenReturn(healthyCustomerHealth(CUSTOMER_ID));
        when(customerHealthService.health(null)).thenReturn(unassignedCustomerHealth());

        CustomerPortfolioResponse portfolio = service.portfolio();
        CustomerPortfolioEntryResponse assigned = entry(portfolio, CUSTOMER_ID);
        CustomerPortfolioEntryResponse unassigned = entry(portfolio, "UNASSIGNED");

        assertEquals(2, portfolio.totalCustomers());
        assertEquals(1, portfolio.healthyCustomers());
        assertEquals(1, portfolio.customersWithDeliveryPackage());
        assertEquals(1, portfolio.customersWithReport());
        assertEquals(1, portfolio.customersWithArtifactCatalog());
        assertFalse(portfolio.healthy());
        assertEquals("Some customers still need attention", portfolio.statusMessage());

        assertTrue(assigned.healthy());
        assertTrue(assigned.reportAvailable());
        assertTrue(assigned.artifactCatalogAvailable());
        assertEquals("install-1", assigned.latestInstallationJobId());
        assertEquals(149.50, assigned.latestSuggestedSellPrice());

        assertFalse(unassigned.healthy());
        assertFalse(unassigned.deliveryPackageAvailable());
        assertFalse(unassigned.reportAvailable());
        assertFalse(unassigned.artifactCatalogAvailable());
        assertEquals(0, unassigned.totalInstallations());
        assertEquals("Awaiting customer assignment", unassigned.healthStatusMessage());

        verify(customerOperationsWorkspaceService).workspace(null);
        verify(customerHealthService).health(null);
    }

    @Test
    void bundleAndExportWritePortfolioArtifacts(@TempDir Path tempDir) throws Exception {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOperationsWorkspaceService customerOperationsWorkspaceService = mock(CustomerOperationsWorkspaceService.class);
        CustomerPortfolioService service = service(
                installationService,
                customerHealthService,
                customerOperationsWorkspaceService,
                tempDir.toString()
        );

        when(installationService.list(null)).thenReturn(List.of(
                CustomerServiceTestFixtures.customerInstallationResponse("install-1", CUSTOMER_ID)
        ));
        when(customerOperationsWorkspaceService.workspace(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOperationsWorkspaceResponse(CUSTOMER_ID)
        );
        when(customerHealthService.health(CUSTOMER_ID)).thenReturn(healthyCustomerHealth(CUSTOMER_ID));

        CustomerPortfolioBundleResponse bundle = service.generateBundle();
        CustomerPortfolioExportResponse export = service.export();

        JsonNode bundleJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.portfolioJsonPath())));
        String bundleHtml = Files.readString(Path.of(bundle.portfolioHtmlPath()));
        String bundleReadme = Files.readString(Path.of(bundle.readmePath()));
        String exportReadme = Files.readString(Path.of(export.readmePath()));

        assertEquals(1, bundleJson.get("totalCustomers").asInt());
        assertEquals(CUSTOMER_ID, bundleJson.get("customers").get(0).get("customerId").asText());
        assertEquals(3, bundle.files().size());
        assertTrue(bundleReadme.contains("Customer portfolio bundle"));
        assertTrue(bundleReadme.contains("Healthy: true"));
        assertTrue(bundleHtml.contains("Customer Portfolio"));
        assertTrue(bundleHtml.contains("Acme Softphone"));
        assertTrue(bundleHtml.contains("149.5"));
        assertTrue(exportReadme.contains("Customer portfolio export"));
        assertTrue(exportReadme.contains("Status: Customer portfolio is healthy"));
    }

    private CustomerPortfolioService service(CustomerInstallationService installationService,
                                             CustomerHealthService customerHealthService,
                                             CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                             String exportDirectory) {
        return new CustomerPortfolioService(
                installationService,
                customerHealthService,
                customerOperationsWorkspaceService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }

    private CustomerPortfolioEntryResponse entry(CustomerPortfolioResponse portfolio, String customerId) {
        return portfolio.customers().stream()
                .filter(customer -> customerId.equals(customer.customerId()))
                .findFirst()
                .orElseThrow();
    }

    private CustomerHealthResponse healthyCustomerHealth(String customerId) {
        return new CustomerHealthResponse(
                customerId,
                GENERATED_AT,
                true,
                "Customer is healthy",
                1,
                1,
                0,
                1,
                true,
                true,
                true,
                149.50,
                "install-1",
                "Acme Softphone",
                "COMPLETED",
                "quote-1"
        );
    }

    private CustomerHealthResponse unassignedCustomerHealth() {
        return new CustomerHealthResponse(
                null,
                GENERATED_AT,
                false,
                "Awaiting customer assignment",
                0,
                0,
                0,
                0,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                null
        );
    }

    private CustomerOperationsWorkspaceResponse unassignedWorkspace() {
        InstallationDashboardResponse dashboard = new InstallationDashboardResponse(
                null,
                GENERATED_AT,
                0,
                0,
                0,
                0,
                0,
                0,
                null,
                List.of()
        );
        InstallationOverviewResponse overview = new InstallationOverviewResponse(
                null,
                GENERATED_AT,
                dashboard,
                new InstallationHealthResponse(null, GENERATED_AT, 0, 0, 0, 0, 0, Map.of(), List.of()),
                new InstallationReportResponse(null, GENERATED_AT, dashboard, null, List.of())
        );
        return new CustomerOperationsWorkspaceResponse(
                null,
                GENERATED_AT,
                null,
                overview,
                new QuoteSnapshotSummaryResponse(null, null, 0, null, null, null, null, null, null, null),
                null,
                false,
                false,
                false,
                false,
                "Awaiting customer assignment",
                null,
                null,
                null,
                null,
                null
        );
    }
}
