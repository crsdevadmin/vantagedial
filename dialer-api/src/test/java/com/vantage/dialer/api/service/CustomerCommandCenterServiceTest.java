package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerAccountCenterResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterEntryResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioEntryResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerCommandCenterServiceTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-03-22T12:00:00Z");
    private static final String CUSTOMER_ID = "customer-1";

    @Test
    void commandCenterAggregatesPortfolioAndAccountSignals() {
        CustomerPortfolioService customerPortfolioService = mock(CustomerPortfolioService.class);
        CustomerAccountCenterService customerAccountCenterService = mock(CustomerAccountCenterService.class);
        CustomerCommandCenterService service = service(
                customerPortfolioService,
                customerAccountCenterService,
                "./build/test-exports"
        );

        when(customerPortfolioService.portfolio()).thenReturn(portfolioResponse());
        when(customerAccountCenterService.account(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerAccountCenterResponse(CUSTOMER_ID)
        );
        when(customerAccountCenterService.account(null)).thenReturn(unassignedAccountCenter());

        CustomerCommandCenterResponse commandCenter = service.commandCenter();
        CustomerCommandCenterEntryResponse assigned = entry(commandCenter, CUSTOMER_ID);
        CustomerCommandCenterEntryResponse unassigned = entry(commandCenter, "UNASSIGNED");

        assertEquals(2, commandCenter.totalCustomers());
        assertEquals(1, commandCenter.healthyCustomers());
        assertEquals(1, commandCenter.customersWithInstallations());
        assertEquals(1, commandCenter.customersWithQuotes());
        assertEquals(1, commandCenter.customersWithDeliveryPackage());
        assertEquals(1, commandCenter.customersWithReport());
        assertEquals(1, commandCenter.customersWithArtifactCatalog());
        assertFalse(commandCenter.healthy());
        assertEquals("Some customers still need attention", commandCenter.statusMessage());

        assertTrue(assigned.hasInstallations());
        assertTrue(assigned.hasQuotes());
        assertTrue(assigned.hasDeliveryPackage());
        assertEquals("install-1", assigned.latestInstallationJobId());
        assertEquals("quote-1", assigned.latestQuoteSnapshotId());

        assertFalse(unassigned.hasInstallations());
        assertFalse(unassigned.hasQuotes());
        assertFalse(unassigned.hasDeliveryPackage());
        assertFalse(unassigned.hasReport());
        assertFalse(unassigned.hasArtifactCatalog());
        assertEquals("Awaiting customer assignment", unassigned.healthStatusMessage());

        verify(customerAccountCenterService).account(null);
    }

    @Test
    void bundleAndExportWriteCommandCenterArtifacts(@TempDir Path tempDir) throws Exception {
        CustomerPortfolioService customerPortfolioService = mock(CustomerPortfolioService.class);
        CustomerAccountCenterService customerAccountCenterService = mock(CustomerAccountCenterService.class);
        CustomerCommandCenterService service = service(
                customerPortfolioService,
                customerAccountCenterService,
                tempDir.toString()
        );

        when(customerPortfolioService.portfolio()).thenReturn(portfolioResponse());
        when(customerAccountCenterService.account(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerAccountCenterResponse(CUSTOMER_ID)
        );
        when(customerAccountCenterService.account(null)).thenReturn(unassignedAccountCenter());

        CustomerCommandCenterBundleResponse bundle = service.generateBundle();
        CustomerCommandCenterExportResponse export = service.export();

        JsonNode bundleJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.commandCenterJsonPath())));
        String bundleHtml = Files.readString(Path.of(bundle.commandCenterHtmlPath()));
        String bundleReadme = Files.readString(Path.of(bundle.readmePath()));
        String exportReadme = Files.readString(Path.of(export.readmePath()));

        assertEquals(2, bundleJson.get("totalCustomers").asInt());
        assertEquals("UNASSIGNED", bundleJson.get("customers").get(1).get("customerId").asText());
        assertEquals(3, bundle.files().size());
        assertTrue(bundleReadme.contains("Customer command center bundle"));
        assertTrue(bundleReadme.contains("Status: Some customers still need attention"));
        assertTrue(bundleHtml.contains("Customer Command Center"));
        assertTrue(bundleHtml.contains("Acme Softphone"));
        assertTrue(bundleHtml.contains("NOT READY"));
        assertTrue(exportReadme.contains("Customer command center export"));
        assertTrue(exportReadme.contains("Total customers: 2"));
    }

    private CustomerCommandCenterService service(CustomerPortfolioService customerPortfolioService,
                                                 CustomerAccountCenterService customerAccountCenterService,
                                                 String exportDirectory) {
        return new CustomerCommandCenterService(
                customerPortfolioService,
                customerAccountCenterService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }

    private CustomerCommandCenterEntryResponse entry(CustomerCommandCenterResponse commandCenter, String customerId) {
        return commandCenter.customers().stream()
                .filter(customer -> customerId.equals(customer.customerId()))
                .findFirst()
                .orElseThrow();
    }

    private CustomerPortfolioResponse portfolioResponse() {
        return new CustomerPortfolioResponse(
                GENERATED_AT,
                2,
                1,
                1,
                1,
                1,
                false,
                "Some customers still need attention",
                List.of(
                        new CustomerPortfolioEntryResponse(
                                CUSTOMER_ID,
                                "install-1",
                                "Acme Softphone",
                                "COMPLETED",
                                1,
                                1,
                                0,
                                1,
                                "quote-1",
                                149.50,
                                true,
                                true,
                                "Customer is healthy",
                                true,
                                true
                        ),
                        new CustomerPortfolioEntryResponse(
                                "UNASSIGNED",
                                null,
                                null,
                                null,
                                0,
                                0,
                                0,
                                0,
                                null,
                                null,
                                false,
                                false,
                                "Awaiting customer assignment",
                                false,
                                false
                        )
                )
        );
    }

    private CustomerAccountCenterResponse unassignedAccountCenter() {
        return new CustomerAccountCenterResponse(
                null,
                GENERATED_AT,
                null,
                null,
                null,
                null,
                false,
                false,
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
