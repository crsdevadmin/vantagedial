package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageExportResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformDeliveryPackageServiceTest {

    @Test
    void detailCarriesDeploymentRollupsFromHealthAndWorkspace(@TempDir Path tempDir) {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        CustomerPortfolioService customerPortfolioService = mock(CustomerPortfolioService.class);
        PlatformArtifactCatalogService artifactCatalogService = mock(PlatformArtifactCatalogService.class);
        PlatformDeliveryPackageService service = service(
                customerCommandCenterService,
                deploymentAuditService,
                customerPortfolioService,
                artifactCatalogService,
                tempDir.toString()
        );

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

        PlatformDeliveryPackageDetailResponse detail = service.detail();

        assertFalse(detail.healthy());
        assertEquals("Deployment failures require attention", detail.statusMessage());
        assertEquals("ASTERISK", detail.latestDeploymentProvider());
        assertEquals("job-2", detail.latestDeploymentJobId());
        assertEquals(2, detail.recentDeployments().size());
        assertEquals(2, detail.recentDeploymentProviders().size());
        assertEquals("job-2", detail.deploymentOverview().mostRecentDeploymentJobId());
        assertEquals("ASTERISK", detail.deploymentOverview().mostRecentDeploymentProvider());
        assertNull(detail.artifactCatalog());
    }

    @Test
    void generateAndExportWriteProviderAwareDeploymentOutputs(@TempDir Path tempDir) throws Exception {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        CustomerPortfolioService customerPortfolioService = mock(CustomerPortfolioService.class);
        PlatformArtifactCatalogService artifactCatalogService = mock(PlatformArtifactCatalogService.class);
        PlatformDeliveryPackageService service = service(
                customerCommandCenterService,
                deploymentAuditService,
                customerPortfolioService,
                artifactCatalogService,
                tempDir.toString()
        );

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

        PlatformDeliveryPackageResponse bundle = service.generate();
        PlatformDeliveryPackageExportResponse export = service.export();

        JsonNode manifest = PlatformServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.manifestPath())));
        JsonNode exportedDetail = PlatformServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(export.packageJsonPath())));
        String bundleReadme = Files.readString(Path.of(bundle.readmePath()));
        String exportHtml = Files.readString(Path.of(export.packageHtmlPath()));

        assertEquals("ASTERISK", manifest.get("latestDeploymentProvider").asText());
        assertEquals("ASTERISK", manifest.get("deploymentOverview").get("mostRecentDeploymentProvider").asText());
        assertEquals("CISCO", manifest.get("recentDeploymentProviders").get(1).asText());
        assertEquals("ASTERISK", exportedDetail.get("latestDeploymentProvider").asText());
        assertEquals("job-2", exportedDetail.get("deploymentOverview").get("mostRecentDeploymentJobId").asText());
        assertTrue(bundleReadme.contains("latest deployment provider: ASTERISK"));
        assertTrue(bundleReadme.contains("recent deployment providers: ASTERISK, CISCO"));
        assertTrue(exportHtml.contains("Latest deployment detail: provider=ASTERISK, job=job-2, status=DEPLOYED"));
        assertTrue(exportHtml.contains("Recent deployment history: count=2, providers=ASTERISK, CISCO"));
    }

    private PlatformDeliveryPackageService service(CustomerCommandCenterService customerCommandCenterService,
                                                   TelephonyDeploymentAuditService deploymentAuditService,
                                                   CustomerPortfolioService customerPortfolioService,
                                                   PlatformArtifactCatalogService artifactCatalogService,
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
        return new PlatformDeliveryPackageService(
                controlCenterService,
                workspaceService,
                healthService,
                artifactCatalogService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
