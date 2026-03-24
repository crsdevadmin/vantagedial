package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.dto.CustomerCommandCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioBundleResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioExportResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageExportResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageResponse;
import com.vantage.dialer.api.dto.PlatformOverviewBundleResponse;
import com.vantage.dialer.api.dto.PlatformOverviewExportResponse;
import com.vantage.dialer.api.dto.PlatformReportBundleResponse;
import com.vantage.dialer.api.dto.PlatformReportExportResponse;
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
import static org.mockito.Mockito.when;

class PlatformArtifactCatalogServiceTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-03-22T12:30:00Z");

    @Test
    void catalogAggregatesPlatformRollupsAndIncludedArtifacts(@TempDir Path tempDir) {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        CustomerPortfolioService customerPortfolioService = mock(CustomerPortfolioService.class);
        PlatformOverviewService platformOverviewService = mock(PlatformOverviewService.class);
        PlatformDeliveryPackageService platformDeliveryPackageService = mock(PlatformDeliveryPackageService.class);
        PlatformReportService platformReportService = mock(PlatformReportService.class);
        PlatformArtifactCatalogService service = service(
                customerCommandCenterService,
                deploymentAuditService,
                customerPortfolioService,
                platformOverviewService,
                platformDeliveryPackageService,
                platformReportService,
                tempDir.toString()
        );

        stubCatalogDependencies(
                customerCommandCenterService,
                deploymentAuditService,
                customerPortfolioService,
                platformOverviewService,
                platformDeliveryPackageService,
                platformReportService
        );

        PlatformArtifactCatalogResponse catalog = service.catalog();

        assertFalse(catalog.healthy());
        assertEquals("Deployment failures require attention", catalog.statusMessage());
        assertEquals("ASTERISK", catalog.latestDeploymentProvider());
        assertEquals("job-2", catalog.latestDeploymentJobId());
        assertEquals(2, catalog.recentDeployments().size());
        assertEquals("job-2", catalog.deploymentOverview().mostRecentDeploymentJobId());
        assertEquals("ASTERISK", catalog.deploymentOverview().mostRecentDeploymentProvider());
        assertEquals("overview-export", catalog.overviewExport().exportDirectory());
        assertEquals("delivery-bundle", catalog.deliveryPackageBundle().packageDirectory());
        assertEquals("command-center-export", catalog.customerCommandCenterExport().exportDirectory());
    }

    @Test
    void generateBundleWritesDeploymentAndIncludedExportContent(@TempDir Path tempDir) throws Exception {
        CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        TelephonyDeploymentAuditService deploymentAuditService = mock(TelephonyDeploymentAuditService.class);
        CustomerPortfolioService customerPortfolioService = mock(CustomerPortfolioService.class);
        PlatformOverviewService platformOverviewService = mock(PlatformOverviewService.class);
        PlatformDeliveryPackageService platformDeliveryPackageService = mock(PlatformDeliveryPackageService.class);
        PlatformReportService platformReportService = mock(PlatformReportService.class);
        PlatformArtifactCatalogService service = service(
                customerCommandCenterService,
                deploymentAuditService,
                customerPortfolioService,
                platformOverviewService,
                platformDeliveryPackageService,
                platformReportService,
                tempDir.toString()
        );

        stubCatalogDependencies(
                customerCommandCenterService,
                deploymentAuditService,
                customerPortfolioService,
                platformOverviewService,
                platformDeliveryPackageService,
                platformReportService
        );

        PlatformArtifactCatalogBundleResponse bundle = service.generateBundle();

        JsonNode json = PlatformServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.catalogJsonPath())));
        String html = Files.readString(Path.of(bundle.catalogHtmlPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals("ASTERISK", json.get("latestDeploymentProvider").asText());
        assertEquals("CISCO", json.get("recentDeploymentProviders").get(1).asText());
        assertEquals("delivery-export", json.get("deliveryPackageExport").get("exportDirectory").asText());
        assertEquals("ASTERISK", json.get("deploymentOverview").get("mostRecentDeploymentProvider").asText());
        assertTrue(readme.contains("Latest deployment detail: provider=ASTERISK, job=job-2, status=DEPLOYED"));
        assertTrue(readme.contains("Recent deployment history: count=2, providers=ASTERISK, CISCO"));
        assertTrue(html.contains("Platform delivery package export: delivery-export"));
        assertTrue(html.contains("Latest deployment detail: provider=ASTERISK, job=job-2, status=DEPLOYED"));
    }

    private PlatformArtifactCatalogService service(CustomerCommandCenterService customerCommandCenterService,
                                                   TelephonyDeploymentAuditService deploymentAuditService,
                                                   CustomerPortfolioService customerPortfolioService,
                                                   PlatformOverviewService platformOverviewService,
                                                   PlatformDeliveryPackageService platformDeliveryPackageService,
                                                   PlatformReportService platformReportService,
                                                   String exportDirectory) {
        PlatformControlCenterService controlCenterService = new PlatformControlCenterService(
                customerCommandCenterService,
                deploymentAuditService,
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
        PlatformHealthService healthService = new PlatformHealthService(
                controlCenterService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
        return new PlatformArtifactCatalogService(
                controlCenterService,
                workspaceService,
                healthService,
                platformOverviewService,
                platformDeliveryPackageService,
                platformReportService,
                customerPortfolioService,
                customerCommandCenterService,
                PlatformServiceTestFixtures.objectMapper(),
                exportDirectory
        );
    }

    private void stubCatalogDependencies(CustomerCommandCenterService customerCommandCenterService,
                                         TelephonyDeploymentAuditService deploymentAuditService,
                                         CustomerPortfolioService customerPortfolioService,
                                         PlatformOverviewService platformOverviewService,
                                         PlatformDeliveryPackageService platformDeliveryPackageService,
                                         PlatformReportService platformReportService) {
        when(customerCommandCenterService.commandCenter()).thenReturn(
                PlatformServiceTestFixtures.customerCommandCenterResponse(2, 2)
        );
        when(customerCommandCenterService.export()).thenReturn(
                new CustomerCommandCenterExportResponse(
                        "command-center-export",
                        "command-center-export/command-center.json",
                        "command-center-export/command-center.html",
                        "command-center-export/README.txt",
                        GENERATED_AT
                )
        );
        when(customerCommandCenterService.generateBundle()).thenReturn(
                new CustomerCommandCenterBundleResponse(
                        "command-center-bundle",
                        "command-center-bundle/command-center.json",
                        "command-center-bundle/command-center.html",
                        "command-center-bundle/README.txt",
                        GENERATED_AT,
                        List.of("command-center.json", "command-center.html", "README.txt")
                )
        );
        when(customerPortfolioService.portfolio()).thenReturn(
                PlatformServiceTestFixtures.customerPortfolioResponse(2, 2)
        );
        when(customerPortfolioService.export()).thenReturn(
                new CustomerPortfolioExportResponse(
                        "portfolio-export",
                        "portfolio-export/portfolio.json",
                        "portfolio-export/portfolio.html",
                        "portfolio-export/README.txt",
                        GENERATED_AT
                )
        );
        when(customerPortfolioService.generateBundle()).thenReturn(
                new CustomerPortfolioBundleResponse(
                        "portfolio-bundle",
                        "portfolio-bundle/portfolio.json",
                        "portfolio-bundle/portfolio.html",
                        "portfolio-bundle/README.txt",
                        GENERATED_AT,
                        List.of("portfolio.json", "portfolio.html", "README.txt")
                )
        );
        when(deploymentAuditService.listDeploymentAudits(null)).thenReturn(
                PlatformServiceTestFixtures.sampleDeployments()
        );
        when(platformOverviewService.export()).thenReturn(
                new PlatformOverviewExportResponse(
                        "overview-export",
                        "overview-export/platform-overview.json",
                        "overview-export/platform-overview.html",
                        "overview-export/README.txt",
                        GENERATED_AT
                )
        );
        when(platformOverviewService.generateBundle()).thenReturn(
                new PlatformOverviewBundleResponse(
                        "overview-bundle",
                        "overview-bundle/platform-overview.json",
                        "overview-bundle/platform-overview.html",
                        "overview-bundle/README.txt",
                        GENERATED_AT,
                        List.of("platform-overview.json", "platform-overview.html", "README.txt")
                )
        );
        when(platformDeliveryPackageService.export()).thenReturn(
                new PlatformDeliveryPackageExportResponse(
                        "delivery-export",
                        "delivery-export/platform-delivery-package.json",
                        "delivery-export/platform-delivery-package.html",
                        "delivery-export/README.txt",
                        GENERATED_AT
                )
        );
        when(platformDeliveryPackageService.generate()).thenReturn(
                new PlatformDeliveryPackageResponse(
                        "delivery-bundle",
                        "delivery-bundle/platform-delivery-package.json",
                        "delivery-bundle/README.txt",
                        GENERATED_AT,
                        List.of("platform-delivery-package.json", "README.txt")
                )
        );
        when(platformReportService.export()).thenReturn(
                new PlatformReportExportResponse(
                        "report-export",
                        "report-export/platform-report.json",
                        "report-export/platform-report.html",
                        "report-export/README.txt",
                        GENERATED_AT
                )
        );
        when(platformReportService.generateBundle()).thenReturn(
                new PlatformReportBundleResponse(
                        "report-bundle",
                        "report-bundle/platform-report.json",
                        "report-bundle/platform-report.html",
                        "report-bundle/README.txt",
                        GENERATED_AT,
                        List.of("platform-report.json", "platform-report.html", "README.txt")
                )
        );
    }
}
