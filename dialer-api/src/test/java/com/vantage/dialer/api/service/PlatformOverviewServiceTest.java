package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.PlatformOverviewResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformOverviewServiceTest {

    @Test
    void overviewAggregatesHealthWorkspaceControlCenterAndReport() {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        CustomerPortfolioService customerPortfolioService = mock(CustomerPortfolioService.class);
        PlatformArtifactCatalogService artifactCatalogService = mock(PlatformArtifactCatalogService.class);
        PlatformDeliveryPackageService deliveryPackageService = mock(PlatformDeliveryPackageService.class);

        when(customerCommandCenterService.commandCenter()).thenReturn(
                PlatformServiceTestFixtures.customerCommandCenterResponse(2, 2)
        );
        when(customerPortfolioService.portfolio()).thenReturn(
                PlatformServiceTestFixtures.customerPortfolioResponse(2, 2)
        );
        when(deploymentAuditService.listDeploymentAudits(null)).thenReturn(
                PlatformServiceTestFixtures.sampleDeployments()
        );
        when(artifactCatalogService.catalog()).thenReturn(null);
        when(deliveryPackageService.detail()).thenReturn(null);

        PlatformOverviewService service = service(
                customerCommandCenterService,
                deploymentAuditService,
                customerPortfolioService,
                artifactCatalogService,
                deliveryPackageService,
                "./build/test-exports"
        );

        PlatformOverviewResponse overview = service.overview();

        assertFalse(overview.healthy());
        assertEquals("Deployment failures require attention", overview.statusMessage());
        assertEquals("ASTERISK", overview.latestDeploymentProvider());
        assertEquals("job-2", overview.latestDeploymentJobId());
        assertEquals(2, overview.recentDeployments().size());
        assertEquals("job-2", overview.deploymentOverview().mostRecentDeploymentJobId());
        assertEquals("ASTERISK", overview.deploymentOverview().mostRecentDeploymentProvider());
        assertEquals(1, overview.health().failedDeployments());
        assertNotNull(overview.controlCenter());
        assertNotNull(overview.workspace());
        assertNotNull(overview.report());
        assertEquals("ASTERISK", overview.report().latestDeploymentProvider());
        assertNull(overview.report().artifactCatalog());
        assertNull(overview.report().deliveryPackage());
    }

    private PlatformOverviewService service(CustomerCommandCenterService customerCommandCenterService,
                                           TelephonyDeploymentAuditService deploymentAuditService,
                                           CustomerPortfolioService customerPortfolioService,
                                           PlatformArtifactCatalogService artifactCatalogService,
                                           PlatformDeliveryPackageService deliveryPackageService,
                                           String exportDirectory) {
        PlatformControlCenterService controlCenterService = new PlatformControlCenterService(
                customerCommandCenterService,
                deploymentAuditService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
        PlatformHealthService healthService = new PlatformHealthService(
                controlCenterService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
        PlatformWorkspaceService workspaceService = new PlatformWorkspaceService(
                controlCenterService,
                customerPortfolioService,
                deploymentAuditService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
        PlatformReportService reportService = new PlatformReportService(
                controlCenterService,
                workspaceService,
                healthService,
                artifactCatalogService,
                deliveryPackageService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
        return new PlatformOverviewService(
                healthService,
                controlCenterService,
                workspaceService,
                reportService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
