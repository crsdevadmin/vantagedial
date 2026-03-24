package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerHealthBundleResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import com.vantage.dialer.api.dto.InstallationOverviewResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerHealthServiceTest {

    private static final String CUSTOMER_ID = "customer-1";

    @Test
    void healthUsesWorkspaceRollupsForHealthyCustomer() {
        CustomerOperationsWorkspaceService workspaceService = mock(CustomerOperationsWorkspaceService.class);
        CustomerHealthService service = service(workspaceService, "./build/test-exports");

        when(workspaceService.workspace(CUSTOMER_ID)).thenReturn(
                CustomerServiceTestFixtures.customerOperationsWorkspaceResponse(CUSTOMER_ID)
        );

        CustomerHealthResponse health = service.health(CUSTOMER_ID);

        assertTrue(health.healthy());
        assertEquals("Customer is healthy", health.statusMessage());
        assertEquals(1, health.totalInstallations());
        assertEquals(0, health.failedInstallations());
        assertEquals("install-1", health.latestInstallationJobId());
        assertEquals("quote-1", health.latestQuoteSnapshotId());
        assertEquals(149.50, health.latestSuggestedSellPrice());
    }

    @Test
    void bundleRendersFailedInstallationStatus(@TempDir Path tempDir) throws Exception {
        CustomerOperationsWorkspaceService workspaceService = mock(CustomerOperationsWorkspaceService.class);
        CustomerHealthService service = service(workspaceService, tempDir.toString());

        when(workspaceService.workspace(CUSTOMER_ID)).thenReturn(workspaceWithFailedInstallation());

        CustomerHealthBundleResponse bundle = service.generateBundle(CUSTOMER_ID);

        JsonNode json = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.healthJsonPath())));
        String html = Files.readString(Path.of(bundle.healthHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals("Customer has failed installations", json.get("statusMessage").asText());
        assertEquals(1, json.get("failedInstallations").asInt());
        assertTrue(readme.contains("Latest installation name: Acme Softphone"));
        assertTrue(readme.contains("Latest quote snapshot: quote-1"));
        assertTrue(html.contains("Customer has failed installations"));
        assertTrue(html.contains("Latest installation job: install-1"));
    }

    private CustomerOperationsWorkspaceResponse workspaceWithFailedInstallation() {
        CustomerOperationsWorkspaceResponse base = CustomerServiceTestFixtures.customerOperationsWorkspaceResponse(CUSTOMER_ID);
        InstallationOverviewResponse baseOverview = base.installationOverview();
        InstallationOverviewResponse failedOverview = new InstallationOverviewResponse(
                baseOverview.customerId(),
                baseOverview.generatedAt(),
                new com.vantage.dialer.api.dto.InstallationDashboardResponse(
                        baseOverview.dashboard().customerId(),
                        baseOverview.dashboard().generatedAt(),
                        baseOverview.dashboard().totalInstallations(),
                        0,
                        1,
                        baseOverview.dashboard().dryRunInstallations(),
                        baseOverview.dashboard().pendingInstallations(),
                        baseOverview.dashboard().totalProvisionedAgents(),
                        baseOverview.dashboard().latestInstallation(),
                        baseOverview.dashboard().installations()
                ),
                baseOverview.health(),
                baseOverview.report()
        );
        return new CustomerOperationsWorkspaceResponse(
                base.customerId(),
                base.generatedAt(),
                base.latestInstallation(),
                failedOverview,
                base.quoteSummary(),
                base.latestDeliveryPackage(),
                base.deliveryPackageAvailable(),
                base.healthy(),
                base.hasReport(),
                base.hasArtifactCatalog(),
                base.statusMessage(),
                base.latestInstallationJobId(),
                base.latestInstallationName(),
                base.latestInstallationStatus(),
                base.latestQuoteSnapshotId(),
                base.latestSuggestedSellPrice()
        );
    }

    private CustomerHealthService service(CustomerOperationsWorkspaceService workspaceService,
                                          String exportDirectory) {
        return new CustomerHealthService(
                workspaceService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
