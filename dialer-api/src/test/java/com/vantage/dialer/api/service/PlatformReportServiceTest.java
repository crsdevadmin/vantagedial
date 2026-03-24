package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.PlatformReportResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformReportServiceTest {

    @Test
    void reportAggregatesDeploymentRollupsAcrossPlatformSurfaces() {
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

        PlatformReportService service = service(
                customerCommandCenterService,
                deploymentAuditService,
                customerPortfolioService,
                artifactCatalogService,
                deliveryPackageService,
                "./build/test-exports"
        );

        PlatformReportResponse report = service.report();

        assertFalse(report.healthy());
        assertEquals("Deployment failures require attention", report.statusMessage());
        assertEquals("ASTERISK", report.latestDeploymentProvider());
        assertEquals("job-2", report.latestDeploymentJobId());
        assertEquals(2, report.recentDeployments().size());
        assertEquals("job-2", report.deploymentOverview().mostRecentDeploymentJobId());
        assertEquals("ASTERISK", report.deploymentOverview().mostRecentDeploymentProvider());
        assertEquals(1, report.health().failedDeployments());
        assertNotNull(report.workspace());
        assertNotNull(report.controlCenter());
        assertNull(report.artifactCatalog());
        assertNull(report.deliveryPackage());
    }

    private PlatformReportService service(CustomerCommandCenterService customerCommandCenterService,
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
        return new PlatformReportService(
                controlCenterService,
                workspaceService,
                healthService,
                artifactCatalogService,
                deliveryPackageService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
