package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.InstallationDashboardResponse;
import com.vantage.dialer.api.dto.InstallationHealthResponse;
import com.vantage.dialer.api.dto.InstallationOverviewResponse;
import com.vantage.dialer.api.dto.InstallationReportResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstallationDashboardServiceTest {

    private static final String CUSTOMER_ID = "customer-1";

    @Test
    void dashboardHealthOverviewAndWorkspaceAggregateInstallations(@TempDir Path tempDir) throws Exception {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        InstallationDashboardService service = service(installationService, tempDir.toString());

        when(installationService.list(CUSTOMER_ID)).thenReturn(sampleInstallations());

        InstallationDashboardResponse dashboard = service.dashboard(CUSTOMER_ID);
        InstallationHealthResponse health = service.health(CUSTOMER_ID);
        InstallationReportResponse report = service.report(CUSTOMER_ID);
        InstallationOverviewResponse overview = service.overview(CUSTOMER_ID);
        InstallationWorkspaceResponse workspace = service.workspace(CUSTOMER_ID);

        assertEquals(3, dashboard.totalInstallations());
        assertEquals(1, dashboard.completedInstallations());
        assertEquals(1, dashboard.failedInstallations());
        assertEquals(1, dashboard.dryRunInstallations());
        assertEquals("install-3", dashboard.latestInstallation().installationJobId());
        assertEquals(2, health.clientTypeCounts().get("SOFTPHONE"));
        assertEquals(1, health.clientTypeCounts().get("HARDPHONE"));
        assertEquals(1, health.recentFailures().size());
        assertEquals("install-2", health.recentFailures().get(0).installationJobId());
        assertEquals("install-3", report.latestInstallation().installationJobId());
        assertEquals(3, overview.report().timeline().size());
        assertTrue(Files.exists(Path.of(workspace.timelineBundle().timelineJsonPath())));
        assertEquals("Gamma Deploy", overview.report().latestInstallation().installationName());
    }

    @Test
    void workspaceAndArtifactCatalogBundlesWriteExpectedFiles(@TempDir Path tempDir) throws Exception {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        InstallationDashboardService service = service(installationService, tempDir.toString());

        when(installationService.list(CUSTOMER_ID)).thenReturn(sampleInstallations());

        InstallationWorkspaceBundleResponse workspaceBundle = service.generateWorkspaceBundle(CUSTOMER_ID);
        InstallationArtifactCatalogBundleResponse artifactBundle = service.generateArtifactCatalogBundle(CUSTOMER_ID);
        InstallationArtifactCatalogExportResponse artifactExport = service.exportArtifactCatalog(CUSTOMER_ID);

        JsonNode workspaceManifest = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(workspaceBundle.manifestPath())));
        JsonNode artifactJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(artifactBundle.catalogJsonPath())));
        JsonNode artifactExportJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(artifactExport.catalogJsonPath())));
        String artifactHtml = Files.readString(Path.of(artifactBundle.catalogHtmlPath()));
        String artifactReadme = Files.readString(Path.of(artifactBundle.readmePath()));

        assertEquals(CUSTOMER_ID, workspaceManifest.get("customerId").asText());
        assertTrue(workspaceManifest.get("dashboardBundle").get("bundleDirectory").asText().contains("dashboard"));
        assertTrue(workspaceManifest.get("overviewBundle").get("bundleDirectory").asText().contains("overview"));
        assertEquals(artifactJson.get("workspaceBundle").get("bundleDirectory").asText(),
                artifactExportJson.get("workspaceBundle").get("bundleDirectory").asText());
        assertTrue(artifactJson.get("dashboardExport").get("exportDirectory").asText().contains("dashboard-export"));
        assertTrue(artifactHtml.contains("Installation Artifact Catalog"));
        assertTrue(artifactHtml.contains("Workspace bundle:"));
        assertTrue(artifactReadme.contains("This catalog references all generated installation exports and the workspace bundle."));
    }

    private List<CustomerInstallationResponse> sampleInstallations() {
        return List.of(
                CustomerServiceTestFixtures.customerInstallationResponse(
                        "install-3",
                        CUSTOMER_ID,
                        "Gamma Deploy",
                        "SOFTPHONE",
                        "COMPLETED",
                        3,
                        "deploy-3",
                        "Deployment completed",
                        null
                ),
                CustomerServiceTestFixtures.customerInstallationResponse(
                        "install-2",
                        CUSTOMER_ID,
                        "Beta Patch",
                        "HARDPHONE",
                        "FAILED",
                        1,
                        "deploy-2",
                        "Gateway update failed",
                        "gateway down"
                ),
                CustomerServiceTestFixtures.customerInstallationResponse(
                        "install-1",
                        CUSTOMER_ID,
                        "Alpha Preview",
                        "SOFTPHONE",
                        "DRY_RUN",
                        2,
                        null,
                        "Preview completed",
                        null
                )
        );
    }

    private InstallationDashboardService service(CustomerInstallationService installationService,
                                                 String exportDirectory) {
        return new InstallationDashboardService(
                installationService,
                CustomerServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }
}
