package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CustomerBootstrapBundleResponse;
import com.vantage.dialer.api.dto.InstallationHandoffBundleResponse;
import com.vantage.dialer.api.dto.InstallationHandoffExportResponse;
import com.vantage.dialer.api.dto.InstallationHandoffResponse;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstallationHandoffBundleServiceTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String INSTALLATION_JOB_ID = "install-1";

    @Test
    void handoffBuildsBootstrapQuoteAndDashboardFromInstallation() {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        CustomerBootstrapBundleService bootstrapBundleService = mock(CustomerBootstrapBundleService.class);
        CustomerQuoteService customerQuoteService = mock(CustomerQuoteService.class);
        QuoteSnapshotService quoteSnapshotService = mock(QuoteSnapshotService.class);
        InstallationHandoffBundleService service = service(
                installationService,
                bootstrapBundleService,
                customerQuoteService,
                quoteSnapshotService,
                "./build/test-exports"
        );

        stubHandoffDependencies(installationService, bootstrapBundleService, customerQuoteService, quoteSnapshotService);

        InstallationHandoffResponse handoff = service.handoff(INSTALLATION_JOB_ID);
        ArgumentCaptor<CostEstimateRequest> requestCaptor = ArgumentCaptor.forClass(CostEstimateRequest.class);
        verify(customerQuoteService).quote(eq(INSTALLATION_JOB_ID), requestCaptor.capture());

        assertEquals(INSTALLATION_JOB_ID, handoff.installationJobId());
        assertEquals("Acme Softphone", handoff.installationName());
        assertEquals("bootstrap-bundle", handoff.bootstrapBundle().bundleDirectory());
        assertEquals(1, handoff.quoteDashboard().summary().snapshotCount());
        assertEquals(149.5, handoff.quoteSummary().estimate().suggestedSellPrice());
        assertEquals(CUSTOMER_ID, requestCaptor.getValue().getCustomerId());
        assertEquals(2, requestCaptor.getValue().getAgentCount());
        assertTrue(requestCaptor.getValue().getUseCustomerPresetDefaults());
    }

    @Test
    void generateAndExportWriteHandoffFiles(@TempDir Path tempDir) throws Exception {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        CustomerBootstrapBundleService bootstrapBundleService = mock(CustomerBootstrapBundleService.class);
        CustomerQuoteService customerQuoteService = mock(CustomerQuoteService.class);
        QuoteSnapshotService quoteSnapshotService = mock(QuoteSnapshotService.class);
        InstallationHandoffBundleService service = service(
                installationService,
                bootstrapBundleService,
                customerQuoteService,
                quoteSnapshotService,
                tempDir.toString()
        );

        stubHandoffDependencies(installationService, bootstrapBundleService, customerQuoteService, quoteSnapshotService);

        InstallationHandoffBundleResponse bundle = service.generate(INSTALLATION_JOB_ID);
        InstallationHandoffExportResponse export = service.export(INSTALLATION_JOB_ID);

        JsonNode installationJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.installationPath())));
        JsonNode quoteSummaryJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.quoteSummaryPath())));
        JsonNode quoteDashboardJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.quoteDashboardPath())));
        JsonNode handoffExportJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(export.handoffJsonPath())));
        String markdown = Files.readString(Path.of(bundle.handoffMarkdownPath()));
        String html = Files.readString(Path.of(bundle.handoffHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals(CUSTOMER_ID, installationJson.get("customerId").asText());
        assertEquals("Acme Corp", quoteSummaryJson.get("customerName").asText());
        assertEquals(1, quoteDashboardJson.get("summary").get("snapshotCount").asInt());
        assertEquals(INSTALLATION_JOB_ID, handoffExportJson.get("installationJobId").asText());
        assertTrue(markdown.contains("Recommended Delivery Steps"));
        assertTrue(markdown.contains("Quote snapshot count: 1"));
        assertTrue(html.contains("Bootstrap bundle directory"));
        assertTrue(html.contains("Latest estimated cost: 112.0"));
        assertTrue(readme.contains("snapshot count of 1"));
    }

    private void stubHandoffDependencies(CustomerInstallationService installationService,
                                         CustomerBootstrapBundleService bootstrapBundleService,
                                         CustomerQuoteService customerQuoteService,
                                         QuoteSnapshotService quoteSnapshotService) {
        CustomerBootstrapBundleResponse bootstrapBundle =
                CustomerServiceTestFixtures.customerBootstrapBundleResponse(INSTALLATION_JOB_ID, "bootstrap-bundle");
        InstallationQuoteSummaryResponse quoteSummary =
                CustomerServiceTestFixtures.installationQuoteSummaryResponse(INSTALLATION_JOB_ID, CUSTOMER_ID);
        QuoteSnapshotDashboardResponse quoteDashboard =
                CustomerServiceTestFixtures.quoteSnapshotDashboardResponse(CUSTOMER_ID);

        when(installationService.get(INSTALLATION_JOB_ID)).thenReturn(
                CustomerServiceTestFixtures.customerInstallationResponse(INSTALLATION_JOB_ID, CUSTOMER_ID)
        );
        when(bootstrapBundleService.generate(INSTALLATION_JOB_ID)).thenReturn(bootstrapBundle);
        when(customerQuoteService.quote(eq(INSTALLATION_JOB_ID), any(CostEstimateRequest.class))).thenReturn(quoteSummary);
        when(quoteSnapshotService.dashboard(INSTALLATION_JOB_ID, CUSTOMER_ID)).thenReturn(quoteDashboard);
    }

    private InstallationHandoffBundleService service(CustomerInstallationService installationService,
                                                     CustomerBootstrapBundleService bootstrapBundleService,
                                                     CustomerQuoteService customerQuoteService,
                                                     QuoteSnapshotService quoteSnapshotService,
                                                     String exportDirectory) {
        return new InstallationHandoffBundleService(
                installationService,
                bootstrapBundleService,
                customerQuoteService,
                quoteSnapshotService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
