package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerReportBundleResponse;
import com.vantage.dialer.api.dto.CustomerReportExportResponse;
import com.vantage.dialer.api.dto.CustomerReportResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerReportServiceTest {

    private static final String CUSTOMER_ID = "customer-1";

    @Test
    void reportAggregatesHealthOverviewAndDeliveryCenter() {
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOverviewService customerOverviewService = mock(CustomerOverviewService.class);
        CustomerDeliveryCenterService customerDeliveryCenterService = mock(CustomerDeliveryCenterService.class);
        CustomerReportService service = service(
                customerHealthService,
                customerOverviewService,
                customerDeliveryCenterService,
                "./build/test-exports"
        );

        stubReportDependencies(customerHealthService, customerOverviewService, customerDeliveryCenterService);

        CustomerReportResponse report = service.report(CUSTOMER_ID);

        assertFalse(report.healthy());
        assertTrue(report.hasReport());
        assertTrue(report.hasArtifactCatalog());
        assertEquals("Needs attention", report.statusMessage());
        assertEquals("install-1", report.latestInstallationJobId());
        assertEquals("Acme Softphone", report.latestInstallationName());
        assertEquals("quote-1", report.latestQuoteSnapshotId());
        assertTrue(report.deliveryPackage().accountCenter().hasDeliveryPackage());
    }

    @Test
    void exportAndBundleWriteCustomerSummaryFiles(@TempDir Path tempDir) throws Exception {
        CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        CustomerOverviewService customerOverviewService = mock(CustomerOverviewService.class);
        CustomerDeliveryCenterService customerDeliveryCenterService = mock(CustomerDeliveryCenterService.class);
        CustomerReportService service = service(
                customerHealthService,
                customerOverviewService,
                customerDeliveryCenterService,
                tempDir.toString()
        );

        stubReportDependencies(customerHealthService, customerOverviewService, customerDeliveryCenterService);

        CustomerReportBundleResponse bundle = service.generateBundle(CUSTOMER_ID);
        CustomerReportExportResponse export = service.export(CUSTOMER_ID);

        JsonNode bundleJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.reportJsonPath())));
        JsonNode exportJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(export.reportJsonPath())));
        String html = Files.readString(Path.of(export.reportHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals(CUSTOMER_ID, bundleJson.get("customerId").asText());
        assertEquals("install-1", bundleJson.get("latestInstallationJobId").asText());
        assertTrue(bundleJson.get("deliveryPackage").get("accountCenter").get("hasDeliveryPackage").asBoolean());
        assertEquals("Acme Softphone", exportJson.get("latestInstallationName").asText());
        assertTrue(readme.contains("artifact catalog ready: true"));
        assertTrue(readme.contains("Latest installation name: Acme Softphone"));
        assertTrue(html.contains("Customer health message: Needs attention"));
        assertTrue(html.contains("Delivery package generated at: 2026-03-22T12:00:00Z"));
    }

    private void stubReportDependencies(CustomerHealthService customerHealthService,
                                        CustomerOverviewService customerOverviewService,
                                        CustomerDeliveryCenterService customerDeliveryCenterService) {
        when(customerHealthService.health(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerHealthResponse(CUSTOMER_ID)
        );
        when(customerOverviewService.overview(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOverviewResponse(CUSTOMER_ID)
        );
        when(customerDeliveryCenterService.deliveryPackage(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerDeliveryCenterResponse(CUSTOMER_ID)
        );
    }

    private CustomerReportService service(CustomerHealthService customerHealthService,
                                          CustomerOverviewService customerOverviewService,
                                          CustomerDeliveryCenterService customerDeliveryCenterService,
                                          String exportDirectory) {
        return new CustomerReportService(
                customerHealthService,
                customerOverviewService,
                customerDeliveryCenterService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
