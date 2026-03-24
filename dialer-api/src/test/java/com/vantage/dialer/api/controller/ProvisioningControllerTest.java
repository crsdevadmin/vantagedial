package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.CommercialAssumptionsDeltaResponse;
import com.vantage.dialer.api.dto.CommercialAssumptionsResponse;
import com.vantage.dialer.api.dto.CostEstimateDeltaResponse;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CostEstimateResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterResponse;
import com.vantage.dialer.api.dto.CustomerBootstrapBundleResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageExportResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageResponse;
import com.vantage.dialer.api.dto.CustomerHealthBundleResponse;
import com.vantage.dialer.api.dto.CustomerHealthExportResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
import com.vantage.dialer.api.dto.CustomerInstallationRequest;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceExportResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import com.vantage.dialer.api.dto.CustomerOverviewBundleResponse;
import com.vantage.dialer.api.dto.CustomerOverviewExportResponse;
import com.vantage.dialer.api.dto.CustomerOverviewResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioBundleResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioEntryResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioExportResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioResponse;
import com.vantage.dialer.api.dto.CustomerReportBundleResponse;
import com.vantage.dialer.api.dto.CustomerReportExportResponse;
import com.vantage.dialer.api.dto.CustomerReportResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogResponse;
import com.vantage.dialer.api.dto.InstallationDashboardBundleResponse;
import com.vantage.dialer.api.dto.InstallationDashboardExportResponse;
import com.vantage.dialer.api.dto.InstallationDashboardResponse;
import com.vantage.dialer.api.dto.InstallationHandoffBundleResponse;
import com.vantage.dialer.api.dto.InstallationHandoffExportResponse;
import com.vantage.dialer.api.dto.InstallationHandoffResponse;
import com.vantage.dialer.api.dto.InstallationHealthBundleResponse;
import com.vantage.dialer.api.dto.InstallationHealthExportResponse;
import com.vantage.dialer.api.dto.InstallationHealthResponse;
import com.vantage.dialer.api.dto.InstallationOverviewBundleResponse;
import com.vantage.dialer.api.dto.InstallationOverviewExportResponse;
import com.vantage.dialer.api.dto.InstallationOverviewResponse;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.InstallationReportBundleResponse;
import com.vantage.dialer.api.dto.InstallationReportExportResponse;
import com.vantage.dialer.api.dto.InstallationReportResponse;
import com.vantage.dialer.api.dto.InstallationTimelineBundleResponse;
import com.vantage.dialer.api.dto.InstallationTimelineEntryResponse;
import com.vantage.dialer.api.dto.InstallationTimelineExportResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceExportResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterExportResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterBundleResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageExportResponse;
import com.vantage.dialer.api.dto.PlatformDeliveryPackageResponse;
import com.vantage.dialer.api.dto.PlatformHealthBundleResponse;
import com.vantage.dialer.api.dto.PlatformHealthExportResponse;
import com.vantage.dialer.api.dto.PlatformHealthResponse;
import com.vantage.dialer.api.dto.PlatformOverviewBundleResponse;
import com.vantage.dialer.api.dto.PlatformOverviewExportResponse;
import com.vantage.dialer.api.dto.PlatformOverviewResponse;
import com.vantage.dialer.api.dto.PlatformReportBundleResponse;
import com.vantage.dialer.api.dto.PlatformReportExportResponse;
import com.vantage.dialer.api.dto.PlatformReportResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceExportResponse;
import com.vantage.dialer.api.dto.RecentDeploymentSummaryResponse;
import com.vantage.dialer.api.dto.QuoteProposalResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotComparisonBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotComparisonResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardExportResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotPreviousComparisonResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotSummaryResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotTimelineBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotTimelineEntryResponse;
import com.vantage.dialer.api.service.CustomerAccountCenterService;
import com.vantage.dialer.api.service.CustomerArtifactCatalogService;
import com.vantage.dialer.api.service.CustomerBootstrapBundleService;
import com.vantage.dialer.api.service.CustomerCommandCenterService;
import com.vantage.dialer.api.service.CustomerDeliveryCenterService;
import com.vantage.dialer.api.service.CustomerDeliveryPackageService;
import com.vantage.dialer.api.service.CustomerHealthService;
import com.vantage.dialer.api.service.CustomerInstallationService;
import com.vantage.dialer.api.service.CustomerOperationsWorkspaceService;
import com.vantage.dialer.api.service.CustomerOverviewService;
import com.vantage.dialer.api.service.CustomerPortfolioService;
import com.vantage.dialer.api.service.CustomerQuoteService;
import com.vantage.dialer.api.service.CustomerReportService;
import com.vantage.dialer.api.service.InstallationDashboardService;
import com.vantage.dialer.api.service.InstallationHandoffBundleService;
import com.vantage.dialer.api.service.PlatformArtifactCatalogService;
import com.vantage.dialer.api.service.PlatformControlCenterService;
import com.vantage.dialer.api.service.PlatformDeliveryPackageService;
import com.vantage.dialer.api.service.PlatformHealthService;
import com.vantage.dialer.api.service.PlatformOverviewService;
import com.vantage.dialer.api.service.PlatformReportService;
import com.vantage.dialer.api.service.PlatformWorkspaceService;
import com.vantage.dialer.api.service.QuoteSnapshotService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProvisioningControllerTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-03-22T12:00:00Z");

    @Test
    void installationEndpointsDelegateAcrossInstallBundleAndDeliveryFlows() throws Exception {
        Fixture fixture = new Fixture();
        CustomerInstallationResponse installation = installation("install-1", "cust-1");
        when(fixture.installationService.install(any(CustomerInstallationRequest.class))).thenReturn(installation);
        when(fixture.installationService.list("cust-1")).thenReturn(List.of(installation));
        when(fixture.installationService.get("install-1")).thenReturn(installation);
        when(fixture.bundleService.generate("install-1")).thenReturn(bootstrapBundle("install-1"));
        when(fixture.handoffBundleService.generate("install-1")).thenReturn(handoffBundle("install-1"));
        when(fixture.deliveryPackageService.generate("install-1")).thenReturn(deliveryPackage("install-1", "cust-1"));
        when(fixture.deliveryPackageService.detail("install-1")).thenReturn(deliveryPackageDetail("install-1", "cust-1"));
        when(fixture.deliveryPackageService.export("install-1")).thenReturn(deliveryExport("install-1"));

        fixture.mockMvc.perform(post("/provisioning/installations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"customerId":"cust-1","installationName":"Acme Install","clientType":"SOFTPHONE","dryRun":false,"performRemoteChecks":true,"deployAfterProvision":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installationJobId").value("install-1"))
                .andExpect(jsonPath("$.customerId").value("cust-1"));

        fixture.mockMvc.perform(get("/provisioning/installations").queryParam("customerId", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installationJobId").value("install-1"));

        fixture.mockMvc.perform(get("/provisioning/installations/install-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installationName").value("Acme Install"));

        fixture.mockMvc.perform(post("/provisioning/installations/install-1/bootstrap-bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/bootstrap"));

        fixture.mockMvc.perform(post("/provisioning/installations/install-1/handoff-bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/handoff"));

        fixture.mockMvc.perform(post("/provisioning/installations/install-1/delivery-package"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageDirectory").value("packages/install-1"));

        fixture.mockMvc.perform(get("/provisioning/installations/install-1/delivery-package"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-1"));

        fixture.mockMvc.perform(post("/provisioning/installations/install-1/delivery-package/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/install-1"));

        ArgumentCaptor<CustomerInstallationRequest> captor = ArgumentCaptor.forClass(CustomerInstallationRequest.class);
        verify(fixture.installationService).install(captor.capture());
        assertEquals("cust-1", captor.getValue().getCustomerId());
        assertEquals("Acme Install", captor.getValue().getInstallationName());
        verify(fixture.installationService).list("cust-1");
        verify(fixture.installationService).get("install-1");
        verify(fixture.bundleService).generate("install-1");
        verify(fixture.handoffBundleService).generate("install-1");
        verify(fixture.deliveryPackageService).generate("install-1");
        verify(fixture.deliveryPackageService).detail("install-1");
        verify(fixture.deliveryPackageService).export("install-1");
    }

    @Test
    void customerAndPlatformRollupEndpointsDelegateToTheirServices() throws Exception {
        Fixture fixture = new Fixture();
        when(fixture.customerOperationsWorkspaceService.export("cust-1"))
                .thenReturn(new CustomerOperationsWorkspaceExportResponse("cust-1", "exports/workspace", "workspace.json", "workspace.html", "README.txt", GENERATED_AT));
        when(fixture.customerPortfolioService.portfolio())
                .thenReturn(new CustomerPortfolioResponse(GENERATED_AT, 2, 1, 1, 1, 1, false, "Attention", List.of(
                        new CustomerPortfolioEntryResponse("cust-1", "install-1", "Acme", "COMPLETED", 1, 1, 0, 1, "quote-1", 149.5, true, true, "Healthy", true, true)
                )));
        when(fixture.customerPortfolioService.export())
                .thenReturn(new CustomerPortfolioExportResponse("exports/portfolio", "portfolio.json", "portfolio.html", "README.txt", GENERATED_AT));
        when(fixture.platformWorkspaceService.export())
                .thenReturn(new PlatformWorkspaceExportResponse("exports/platform-workspace", "workspace.json", "workspace.html", "README.txt", GENERATED_AT));
        when(fixture.platformHealthService.generateBundle())
                .thenReturn(new PlatformHealthBundleResponse("bundles/platform-health", "health.json", "health.html", "README.txt", GENERATED_AT, List.of("health.json")));
        when(fixture.platformControlCenterService.generateBundle())
                .thenReturn(new PlatformControlCenterBundleResponse("bundles/control-center", "control-center.json", "control-center.html", "README.txt", GENERATED_AT, List.of("control-center.json")));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-1/workspace/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-1"))
                .andExpect(jsonPath("$.workspaceJsonPath").value("workspace.json"));

        fixture.mockMvc.perform(get("/provisioning/customers/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(2))
                .andExpect(jsonPath("$.customers[0].customerId").value("cust-1"));

        fixture.mockMvc.perform(post("/provisioning/customers/portfolio/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/portfolio"));

        fixture.mockMvc.perform(post("/provisioning/platform/workspace/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/platform-workspace"));

        fixture.mockMvc.perform(post("/provisioning/platform/health/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/platform-health"));

        fixture.mockMvc.perform(post("/provisioning/platform/control-center/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/control-center"));

        verify(fixture.customerOperationsWorkspaceService).export("cust-1");
        verify(fixture.customerPortfolioService).portfolio();
        verify(fixture.customerPortfolioService).export();
        verify(fixture.platformWorkspaceService).export();
        verify(fixture.platformHealthService).generateBundle();
        verify(fixture.platformControlCenterService).generateBundle();
    }

    @Test
    void customerRollupEndpointsDelegateAcrossRemainingWorkspaceHealthOverviewArtifactDeliveryReportAndAccountFlows() throws Exception {
        Fixture fixture = new Fixture();

        when(fixture.customerOperationsWorkspaceService.workspace("cust-7")).thenReturn(customerOperationsWorkspaceResponse("cust-7"));
        when(fixture.customerOperationsWorkspaceService.generateBundle("cust-7"))
                .thenReturn(customerOperationsWorkspaceBundleResponse("cust-7", "bundles/customer-workspace"));
        when(fixture.customerHealthService.health("cust-7")).thenReturn(customerHealthResponse("cust-7"));
        when(fixture.customerHealthService.export("cust-7"))
                .thenReturn(customerHealthExportResponse("cust-7", "exports/customer-health"));
        when(fixture.customerHealthService.generateBundle("cust-7"))
                .thenReturn(customerHealthBundleResponse("cust-7", "bundles/customer-health"));
        when(fixture.customerOverviewService.overview("cust-7")).thenReturn(customerOverviewResponse("cust-7"));
        when(fixture.customerOverviewService.export("cust-7"))
                .thenReturn(customerOverviewExportResponse("cust-7", "exports/customer-overview"));
        when(fixture.customerOverviewService.generateBundle("cust-7"))
                .thenReturn(customerOverviewBundleResponse("cust-7", "bundles/customer-overview"));
        when(fixture.customerArtifactCatalogService.catalog("cust-7")).thenReturn(customerArtifactCatalogResponse("cust-7"));
        when(fixture.customerArtifactCatalogService.export("cust-7"))
                .thenReturn(customerArtifactCatalogExportResponse("cust-7", "exports/customer-artifacts"));
        when(fixture.customerArtifactCatalogService.generateBundle("cust-7"))
                .thenReturn(customerArtifactCatalogBundleResponse("cust-7", "bundles/customer-artifacts"));
        when(fixture.customerDeliveryCenterService.deliveryPackage("cust-7")).thenReturn(customerDeliveryCenterResponse("cust-7"));
        when(fixture.customerDeliveryCenterService.export("cust-7"))
                .thenReturn(customerDeliveryCenterExportResponse("cust-7", "exports/customer-delivery"));
        when(fixture.customerDeliveryCenterService.generateBundle("cust-7"))
                .thenReturn(customerDeliveryCenterBundleResponse("cust-7", "bundles/customer-delivery"));
        when(fixture.customerReportService.report("cust-7")).thenReturn(customerReportResponse("cust-7"));
        when(fixture.customerReportService.export("cust-7"))
                .thenReturn(customerReportExportResponse("cust-7", "exports/customer-report"));
        when(fixture.customerReportService.generateBundle("cust-7"))
                .thenReturn(customerReportBundleResponse("cust-7", "bundles/customer-report"));
        when(fixture.customerAccountCenterService.account("cust-7")).thenReturn(customerAccountCenterResponse("cust-7"));
        when(fixture.customerAccountCenterService.export("cust-7"))
                .thenReturn(customerAccountCenterExportResponse("cust-7", "exports/customer-account"));
        when(fixture.customerAccountCenterService.generateBundle("cust-7"))
                .thenReturn(customerAccountCenterBundleResponse("cust-7", "bundles/customer-account"));

        fixture.mockMvc.perform(get("/provisioning/customers/cust-7/workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-7"))
                .andExpect(jsonPath("$.latestInstallationJobId").value("install-1"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/workspace/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/customer-workspace"));

        fixture.mockMvc.perform(get("/provisioning/customers/cust-7/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestInstallationName").value("Acme Softphone"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/health/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/customer-health"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/health/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/customer-health"));

        fixture.mockMvc.perform(get("/provisioning/customers/cust-7/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspace.latestInstallationName").value("Acme Softphone"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/overview/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/customer-overview"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/overview/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/customer-overview"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/artifacts/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceExport.exportDirectory").value("workspace-export"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/artifacts/catalog/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/customer-artifacts"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/artifacts/catalog/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/customer-artifacts"));

        fixture.mockMvc.perform(get("/provisioning/customers/cust-7/delivery-package"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifactCatalog.latestInstallationJobId").value("install-1"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/delivery-package/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/customer-delivery"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/delivery-package/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/customer-delivery"));

        fixture.mockMvc.perform(get("/provisioning/customers/cust-7/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryPackage.latestInstallationName").value("Acme Softphone"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/report/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/customer-report"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/report/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/customer-report"));

        fixture.mockMvc.perform(get("/provisioning/customers/cust-7/account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestQuoteSnapshotId").value("quote-1"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/account/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/customer-account"));

        fixture.mockMvc.perform(post("/provisioning/customers/cust-7/account/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/customer-account"));

        verify(fixture.customerOperationsWorkspaceService).workspace("cust-7");
        verify(fixture.customerOperationsWorkspaceService).generateBundle("cust-7");
        verify(fixture.customerHealthService).health("cust-7");
        verify(fixture.customerHealthService).export("cust-7");
        verify(fixture.customerHealthService).generateBundle("cust-7");
        verify(fixture.customerOverviewService).overview("cust-7");
        verify(fixture.customerOverviewService).export("cust-7");
        verify(fixture.customerOverviewService).generateBundle("cust-7");
        verify(fixture.customerArtifactCatalogService).catalog("cust-7");
        verify(fixture.customerArtifactCatalogService).export("cust-7");
        verify(fixture.customerArtifactCatalogService).generateBundle("cust-7");
        verify(fixture.customerDeliveryCenterService).deliveryPackage("cust-7");
        verify(fixture.customerDeliveryCenterService).export("cust-7");
        verify(fixture.customerDeliveryCenterService).generateBundle("cust-7");
        verify(fixture.customerReportService).report("cust-7");
        verify(fixture.customerReportService).export("cust-7");
        verify(fixture.customerReportService).generateBundle("cust-7");
        verify(fixture.customerAccountCenterService).account("cust-7");
        verify(fixture.customerAccountCenterService).export("cust-7");
        verify(fixture.customerAccountCenterService).generateBundle("cust-7");
    }

    @Test
    void customerAggregateBundleEndpointsDelegateAcrossPortfolioAndCommandCenterFlows() throws Exception {
        Fixture fixture = new Fixture();

        when(fixture.customerPortfolioService.generateBundle())
                .thenReturn(new CustomerPortfolioBundleResponse("bundles/customer-portfolio", "portfolio.json", "portfolio.html", "README.txt", GENERATED_AT, List.of("portfolio.json")));
        when(fixture.customerCommandCenterService.commandCenter()).thenReturn(customerCommandCenterResponse(3, 2));
        when(fixture.customerCommandCenterService.export())
                .thenReturn(new CustomerCommandCenterExportResponse("exports/customer-command-center", "command-center.json", "command-center.html", "README.txt", GENERATED_AT));
        when(fixture.customerCommandCenterService.generateBundle())
                .thenReturn(new CustomerCommandCenterBundleResponse("bundles/customer-command-center", "command-center.json", "command-center.html", "README.txt", GENERATED_AT, List.of("command-center.json")));

        fixture.mockMvc.perform(post("/provisioning/customers/portfolio/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/customer-portfolio"));

        fixture.mockMvc.perform(get("/provisioning/customers/command-center"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(3))
                .andExpect(jsonPath("$.healthyCustomers").value(2));

        fixture.mockMvc.perform(post("/provisioning/customers/command-center/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/customer-command-center"));

        fixture.mockMvc.perform(post("/provisioning/customers/command-center/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/customer-command-center"));

        verify(fixture.customerPortfolioService).generateBundle();
        verify(fixture.customerCommandCenterService).commandCenter();
        verify(fixture.customerCommandCenterService).export();
        verify(fixture.customerCommandCenterService).generateBundle();
    }

    @Test
    void platformEndpointsDelegateAcrossControlCenterWorkspaceHealthOverviewDeliveryReportAndArtifactFlows() throws Exception {
        Fixture fixture = new Fixture();

        PlatformControlCenterResponse controlCenter = platformControlCenterResponse();
        PlatformWorkspaceResponse workspace = platformWorkspaceResponse(controlCenter);
        PlatformHealthResponse health = platformHealthResponse();
        PlatformOverviewResponse overview = platformOverviewResponse(health, controlCenter, workspace);
        PlatformArtifactCatalogResponse artifactCatalog = platformArtifactCatalogResponse();
        PlatformDeliveryPackageDetailResponse deliveryDetail = platformDeliveryPackageDetailResponse(health, controlCenter, workspace, artifactCatalog);
        PlatformReportResponse report = platformReportResponse(health, controlCenter, workspace, artifactCatalog, deliveryDetail);

        when(fixture.platformControlCenterService.controlCenter()).thenReturn(controlCenter);
        when(fixture.platformControlCenterService.export())
                .thenReturn(new PlatformControlCenterExportResponse("exports/platform-control-center", "control-center.json", "control-center.html", "README.txt", GENERATED_AT));
        when(fixture.platformWorkspaceService.workspace()).thenReturn(workspace);
        when(fixture.platformWorkspaceService.generateBundle())
                .thenReturn(new PlatformWorkspaceBundleResponse("bundles/platform-workspace", "workspace.json", "workspace.html", "README.txt", GENERATED_AT, List.of("workspace.json")));
        when(fixture.platformHealthService.health()).thenReturn(health);
        when(fixture.platformHealthService.export())
                .thenReturn(new PlatformHealthExportResponse("exports/platform-health", "health.json", "health.html", "README.txt", GENERATED_AT));
        when(fixture.platformOverviewService.overview()).thenReturn(overview);
        when(fixture.platformOverviewService.export())
                .thenReturn(new PlatformOverviewExportResponse("exports/platform-overview", "overview.json", "overview.html", "README.txt", GENERATED_AT));
        when(fixture.platformOverviewService.generateBundle())
                .thenReturn(new PlatformOverviewBundleResponse("bundles/platform-overview", "overview.json", "overview.html", "README.txt", GENERATED_AT, List.of("overview.json")));
        when(fixture.platformArtifactCatalogService.catalog()).thenReturn(artifactCatalog);
        when(fixture.platformArtifactCatalogService.export())
                .thenReturn(new PlatformArtifactCatalogExportResponse("exports/platform-artifacts", "catalog.json", "catalog.html", "README.txt", GENERATED_AT));
        when(fixture.platformArtifactCatalogService.generateBundle())
                .thenReturn(new PlatformArtifactCatalogBundleResponse("bundles/platform-artifacts", "catalog.json", "catalog.html", "README.txt", GENERATED_AT, List.of("catalog.json")));
        when(fixture.platformDeliveryPackageService.detail()).thenReturn(deliveryDetail);
        when(fixture.platformDeliveryPackageService.export())
                .thenReturn(new PlatformDeliveryPackageExportResponse("exports/platform-delivery", "package.json", "package.html", "README.txt", GENERATED_AT));
        when(fixture.platformDeliveryPackageService.generate())
                .thenReturn(new PlatformDeliveryPackageResponse("bundles/platform-delivery", "manifest.json", "README.txt", GENERATED_AT, List.of("manifest.json")));
        when(fixture.platformReportService.report()).thenReturn(report);
        when(fixture.platformReportService.export())
                .thenReturn(new PlatformReportExportResponse("exports/platform-report", "report.json", "report.html", "README.txt", GENERATED_AT));
        when(fixture.platformReportService.generateBundle())
                .thenReturn(platformReportBundleResponse());

        fixture.mockMvc.perform(get("/provisioning/platform/control-center"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthyCustomers").value(2))
                .andExpect(jsonPath("$.latestDeploymentProvider").value("ASTERISK"));

        fixture.mockMvc.perform(post("/provisioning/platform/control-center/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/platform-control-center"));

        fixture.mockMvc.perform(get("/provisioning/platform/workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusMessage").value("Platform workspace ready"))
                .andExpect(jsonPath("$.latestDeploymentProvider").value("ASTERISK"));

        fixture.mockMvc.perform(post("/provisioning/platform/workspace/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/platform-workspace"));

        fixture.mockMvc.perform(get("/provisioning/platform/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(3))
                .andExpect(jsonPath("$.latestDeploymentProvider").value("ASTERISK"));

        fixture.mockMvc.perform(post("/provisioning/platform/health/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/platform-health"));

        fixture.mockMvc.perform(get("/provisioning/platform/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.statusMessage").value("Platform report ready"));

        fixture.mockMvc.perform(post("/provisioning/platform/overview/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/platform-overview"));

        fixture.mockMvc.perform(post("/provisioning/platform/overview/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/platform-overview"));

        fixture.mockMvc.perform(post("/provisioning/platform/artifacts/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controlCenterExport.exportDirectory").value("exports/platform-control-center"));

        fixture.mockMvc.perform(post("/provisioning/platform/artifacts/catalog/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/platform-artifacts"));

        fixture.mockMvc.perform(post("/provisioning/platform/artifacts/catalog/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/platform-artifacts"));

        fixture.mockMvc.perform(get("/provisioning/platform/delivery-package"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifactCatalog.statusMessage").value("Platform artifacts ready"));

        fixture.mockMvc.perform(post("/provisioning/platform/delivery-package/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/platform-delivery"));

        fixture.mockMvc.perform(post("/provisioning/platform/delivery-package"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageDirectory").value("bundles/platform-delivery"));

        fixture.mockMvc.perform(get("/provisioning/platform/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifactCatalog.statusMessage").value("Platform artifacts ready"));

        fixture.mockMvc.perform(post("/provisioning/platform/report/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/platform-report"));

        fixture.mockMvc.perform(post("/provisioning/platform/report/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/platform-report"));

        verify(fixture.platformControlCenterService).controlCenter();
        verify(fixture.platformControlCenterService).export();
        verify(fixture.platformWorkspaceService).workspace();
        verify(fixture.platformWorkspaceService).generateBundle();
        verify(fixture.platformHealthService).health();
        verify(fixture.platformHealthService).export();
        verify(fixture.platformOverviewService).overview();
        verify(fixture.platformOverviewService).export();
        verify(fixture.platformOverviewService).generateBundle();
        verify(fixture.platformArtifactCatalogService).catalog();
        verify(fixture.platformArtifactCatalogService).export();
        verify(fixture.platformArtifactCatalogService).generateBundle();
        verify(fixture.platformDeliveryPackageService).detail();
        verify(fixture.platformDeliveryPackageService).export();
        verify(fixture.platformDeliveryPackageService).generate();
        verify(fixture.platformReportService).report();
        verify(fixture.platformReportService).export();
        verify(fixture.platformReportService).generateBundle();
    }

    @Test
    void installationSupportEndpointsDelegateAcrossHandoffReportHealthOverviewWorkspaceAndArtifacts() throws Exception {
        Fixture fixture = new Fixture();
        CustomerInstallationResponse installation = installation("install-2", "cust-2");
        InstallationDashboardResponse dashboard = new InstallationDashboardResponse(
                "cust-2",
                GENERATED_AT,
                1,
                1,
                0,
                0,
                0,
                2,
                installation,
                List.of(installation)
        );
        InstallationReportResponse report = new InstallationReportResponse(
                "cust-2",
                GENERATED_AT,
                dashboard,
                installation,
                List.of(InstallationTimelineEntryResponse.from(installation))
        );
        InstallationHealthResponse health = new InstallationHealthResponse(
                "cust-2",
                GENERATED_AT,
                1,
                1,
                0,
                0,
                0,
                Map.of("SOFTPHONE", 1),
                List.of()
        );
        InstallationOverviewResponse overview = new InstallationOverviewResponse("cust-2", GENERATED_AT, dashboard, health, report);
        InstallationWorkspaceResponse workspace = new InstallationWorkspaceResponse("cust-2", GENERATED_AT, dashboard, null, health, report, overview);
        InstallationArtifactCatalogResponse artifactCatalog = new InstallationArtifactCatalogResponse(
                "cust-2",
                GENERATED_AT,
                new InstallationDashboardExportResponse("cust-2", "exports/dashboard", "dashboard.json", "dashboard.csv", "dashboard.html", "README.txt", GENERATED_AT),
                new InstallationTimelineExportResponse("cust-2", "exports/timeline", "timeline.json", "timeline.csv", "timeline.html", "README.txt", GENERATED_AT),
                new InstallationHealthExportResponse("cust-2", "exports/health", "health.json", "health.html", "README.txt", GENERATED_AT),
                new InstallationReportExportResponse("cust-2", "exports/report", "report.json", "report.html", "README.txt", GENERATED_AT),
                new InstallationOverviewExportResponse("cust-2", "exports/overview", "overview.json", "overview.html", "README.txt", GENERATED_AT),
                new InstallationWorkspaceExportResponse("cust-2", "exports/workspace", "workspace.json", "workspace.html", "README.txt", GENERATED_AT),
                new InstallationWorkspaceBundleResponse("cust-2", "bundles/workspace", "manifest.json", "README.txt", GENERATED_AT, List.of("manifest.json"))
        );

        when(fixture.handoffBundleService.handoff("install-2"))
                .thenReturn(new InstallationHandoffResponse(
                        "install-2",
                        "cust-2",
                        "Acme Install",
                        GENERATED_AT,
                        installation,
                        bootstrapBundle("install-2"),
                        summaryForInstallation("install-2", "cust-2"),
                        quoteDashboard("install-2", "cust-2", "quote-2")
                ));
        when(fixture.handoffBundleService.export("install-2"))
                .thenReturn(new InstallationHandoffExportResponse("install-2", "exports/handoff", "handoff.json", "handoff.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.report("cust-2")).thenReturn(report);
        when(fixture.installationDashboardService.exportReport("cust-2"))
                .thenReturn(new InstallationReportExportResponse("cust-2", "exports/report", "report.json", "report.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateReportBundle("cust-2"))
                .thenReturn(new InstallationReportBundleResponse("cust-2", "bundles/report", "dashboard.json", "timeline.json", "latest-installation.json", "report.md", "report.html", "README.txt", GENERATED_AT, List.of("report.json")));
        when(fixture.installationDashboardService.health("cust-2")).thenReturn(health);
        when(fixture.installationDashboardService.exportHealth("cust-2"))
                .thenReturn(new InstallationHealthExportResponse("cust-2", "exports/health", "health.json", "health.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateHealthBundle("cust-2"))
                .thenReturn(new InstallationHealthBundleResponse("cust-2", "bundles/health", "health.json", "health.md", "health.html", "README.txt", GENERATED_AT, List.of("health.json")));
        when(fixture.installationDashboardService.overview("cust-2")).thenReturn(overview);
        when(fixture.installationDashboardService.exportOverview("cust-2"))
                .thenReturn(new InstallationOverviewExportResponse("cust-2", "exports/overview", "overview.json", "overview.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateOverviewBundle("cust-2"))
                .thenReturn(new InstallationOverviewBundleResponse("cust-2", "bundles/overview", "overview.json", "dashboard.json", "health.json", "report.json", "overview.md", "overview.html", "README.txt", GENERATED_AT, List.of("overview.json")));
        when(fixture.installationDashboardService.workspace("cust-2")).thenReturn(workspace);
        when(fixture.installationDashboardService.exportWorkspace("cust-2"))
                .thenReturn(new InstallationWorkspaceExportResponse("cust-2", "exports/workspace", "workspace.json", "workspace.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateWorkspaceBundle("cust-2"))
                .thenReturn(new InstallationWorkspaceBundleResponse("cust-2", "bundles/workspace", "manifest.json", "README.txt", GENERATED_AT, List.of("manifest.json")));
        when(fixture.installationDashboardService.generateArtifactCatalog("cust-2")).thenReturn(artifactCatalog);
        when(fixture.installationDashboardService.exportArtifactCatalog("cust-2"))
                .thenReturn(new InstallationArtifactCatalogExportResponse("cust-2", "exports/artifacts", "catalog.json", "catalog.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateArtifactCatalogBundle("cust-2"))
                .thenReturn(new InstallationArtifactCatalogBundleResponse("cust-2", "bundles/artifacts", "catalog.json", "catalog.html", "README.txt", GENERATED_AT, List.of("catalog.json")));

        fixture.mockMvc.perform(get("/provisioning/installations/install-2/handoff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installationJobId").value("install-2"))
                .andExpect(jsonPath("$.quoteDashboard.summary.snapshotCount").value(1));

        fixture.mockMvc.perform(post("/provisioning/installations/install-2/handoff-export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/handoff"));

        fixture.mockMvc.perform(get("/provisioning/installations/report").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestInstallation.installationJobId").value("install-2"));

        fixture.mockMvc.perform(post("/provisioning/installations/report/export").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/report"));

        fixture.mockMvc.perform(post("/provisioning/installations/report/bundle").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/report"));

        fixture.mockMvc.perform(get("/provisioning/installations/health").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInstallations").value(1))
                .andExpect(jsonPath("$.clientTypeCounts.SOFTPHONE").value(1));

        fixture.mockMvc.perform(post("/provisioning/installations/health/export").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/health"));

        fixture.mockMvc.perform(post("/provisioning/installations/health/bundle").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/health"));

        fixture.mockMvc.perform(get("/provisioning/installations/overview").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health.totalInstallations").value(1));

        fixture.mockMvc.perform(post("/provisioning/installations/overview/export").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/overview"));

        fixture.mockMvc.perform(post("/provisioning/installations/overview/bundle").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/overview"));

        fixture.mockMvc.perform(get("/provisioning/installations/workspace").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.customerId").value("cust-2"));

        fixture.mockMvc.perform(post("/provisioning/installations/workspace/export").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/workspace"));

        fixture.mockMvc.perform(post("/provisioning/installations/workspace/bundle").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/workspace"));

        fixture.mockMvc.perform(post("/provisioning/installations/artifacts/catalog").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceExport.exportDirectory").value("exports/workspace"));

        fixture.mockMvc.perform(post("/provisioning/installations/artifacts/catalog/export").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/artifacts"));

        fixture.mockMvc.perform(post("/provisioning/installations/artifacts/catalog/bundle").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/artifacts"));

        verify(fixture.handoffBundleService).handoff("install-2");
        verify(fixture.handoffBundleService).export("install-2");
        verify(fixture.installationDashboardService).report("cust-2");
        verify(fixture.installationDashboardService).exportReport("cust-2");
        verify(fixture.installationDashboardService).generateReportBundle("cust-2");
        verify(fixture.installationDashboardService).health("cust-2");
        verify(fixture.installationDashboardService).exportHealth("cust-2");
        verify(fixture.installationDashboardService).generateHealthBundle("cust-2");
        verify(fixture.installationDashboardService).overview("cust-2");
        verify(fixture.installationDashboardService).exportOverview("cust-2");
        verify(fixture.installationDashboardService).generateOverviewBundle("cust-2");
        verify(fixture.installationDashboardService).workspace("cust-2");
        verify(fixture.installationDashboardService).exportWorkspace("cust-2");
        verify(fixture.installationDashboardService).generateWorkspaceBundle("cust-2");
        verify(fixture.installationDashboardService).generateArtifactCatalog("cust-2");
        verify(fixture.installationDashboardService).exportArtifactCatalog("cust-2");
        verify(fixture.installationDashboardService).generateArtifactCatalogBundle("cust-2");
    }

    @Test
    void installationDashboardAndTimelineEndpointsForwardOptionalCustomerId() throws Exception {
        Fixture fixture = new Fixture();
        CustomerInstallationResponse installation = installation("install-2", "cust-2");
        when(fixture.installationDashboardService.dashboard("cust-2"))
                .thenReturn(new InstallationDashboardResponse("cust-2", GENERATED_AT, 1, 1, 0, 0, 0, 2, installation, List.of(installation)));
        when(fixture.installationDashboardService.exportDashboard("cust-2"))
                .thenReturn(new InstallationDashboardExportResponse("cust-2", "exports/dashboard", "dashboard.json", "dashboard.csv", "dashboard.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateBundle("cust-2"))
                .thenReturn(new InstallationDashboardBundleResponse("cust-2", "bundles/dashboard", "dashboard.json", "dashboard.md", "dashboard.html", "README.txt", GENERATED_AT, List.of("dashboard.json")));
        when(fixture.installationDashboardService.timeline("cust-2"))
                .thenReturn(List.of(InstallationTimelineEntryResponse.from(installation)));
        when(fixture.installationDashboardService.exportTimeline("cust-2"))
                .thenReturn(new InstallationTimelineExportResponse("cust-2", "exports/timeline", "timeline.json", "timeline.csv", "timeline.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateTimelineBundle("cust-2"))
                .thenReturn(new InstallationTimelineBundleResponse("cust-2", "bundles/timeline", "timeline.json", "timeline.csv", "timeline.md", "timeline.html", "README.txt", GENERATED_AT, List.of("timeline.json")));

        fixture.mockMvc.perform(get("/provisioning/installations/dashboard").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-2"))
                .andExpect(jsonPath("$.latestInstallation.installationJobId").value("install-2"));

        fixture.mockMvc.perform(post("/provisioning/installations/dashboard/export").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/dashboard"));

        fixture.mockMvc.perform(post("/provisioning/installations/dashboard/bundle").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/dashboard"));

        fixture.mockMvc.perform(get("/provisioning/installations/timeline").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installationJobId").value("install-2"));

        fixture.mockMvc.perform(post("/provisioning/installations/timeline/export").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/timeline"));

        fixture.mockMvc.perform(post("/provisioning/installations/timeline/bundle").queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/timeline"));

        verify(fixture.installationDashboardService).dashboard("cust-2");
        verify(fixture.installationDashboardService).exportDashboard("cust-2");
        verify(fixture.installationDashboardService).generateBundle("cust-2");
        verify(fixture.installationDashboardService).timeline("cust-2");
        verify(fixture.installationDashboardService).exportTimeline("cust-2");
        verify(fixture.installationDashboardService).generateTimelineBundle("cust-2");
    }

    @Test
    void installationOptionalCustomerFiltersMayBeOmitted() throws Exception {
        Fixture fixture = new Fixture();
        CustomerInstallationResponse installation = installation("install-all", "cust-all");
        InstallationDashboardResponse dashboard = new InstallationDashboardResponse(
                null,
                GENERATED_AT,
                1,
                1,
                0,
                0,
                0,
                2,
                installation,
                List.of(installation)
        );
        InstallationReportResponse report = new InstallationReportResponse(
                null,
                GENERATED_AT,
                dashboard,
                installation,
                List.of(InstallationTimelineEntryResponse.from(installation))
        );
        InstallationHealthResponse health = new InstallationHealthResponse(
                null,
                GENERATED_AT,
                1,
                1,
                0,
                0,
                0,
                Map.of("SOFTPHONE", 1),
                List.of()
        );
        InstallationOverviewResponse overview = new InstallationOverviewResponse(null, GENERATED_AT, dashboard, health, report);
        InstallationWorkspaceResponse workspace = new InstallationWorkspaceResponse(null, GENERATED_AT, dashboard, null, health, report, overview);
        InstallationArtifactCatalogResponse artifactCatalog = new InstallationArtifactCatalogResponse(
                null,
                GENERATED_AT,
                new InstallationDashboardExportResponse(null, "exports/dashboard-all", "dashboard.json", "dashboard.csv", "dashboard.html", "README.txt", GENERATED_AT),
                new InstallationTimelineExportResponse(null, "exports/timeline-all", "timeline.json", "timeline.csv", "timeline.html", "README.txt", GENERATED_AT),
                new InstallationHealthExportResponse(null, "exports/health-all", "health.json", "health.html", "README.txt", GENERATED_AT),
                new InstallationReportExportResponse(null, "exports/report-all", "report.json", "report.html", "README.txt", GENERATED_AT),
                new InstallationOverviewExportResponse(null, "exports/overview-all", "overview.json", "overview.html", "README.txt", GENERATED_AT),
                new InstallationWorkspaceExportResponse(null, "exports/workspace-all", "workspace.json", "workspace.html", "README.txt", GENERATED_AT),
                new InstallationWorkspaceBundleResponse(null, "bundles/workspace-all", "manifest.json", "README.txt", GENERATED_AT, List.of("manifest.json"))
        );

        when(fixture.installationService.list(null)).thenReturn(List.of(installation));
        when(fixture.installationDashboardService.dashboard(null)).thenReturn(dashboard);
        when(fixture.installationDashboardService.exportDashboard(null))
                .thenReturn(new InstallationDashboardExportResponse(null, "exports/dashboard-all", "dashboard.json", "dashboard.csv", "dashboard.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateBundle(null))
                .thenReturn(new InstallationDashboardBundleResponse(null, "bundles/dashboard-all", "dashboard.json", "dashboard.md", "dashboard.html", "README.txt", GENERATED_AT, List.of("dashboard.json")));
        when(fixture.installationDashboardService.timeline(null))
                .thenReturn(List.of(InstallationTimelineEntryResponse.from(installation)));
        when(fixture.installationDashboardService.exportTimeline(null))
                .thenReturn(new InstallationTimelineExportResponse(null, "exports/timeline-all", "timeline.json", "timeline.csv", "timeline.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateTimelineBundle(null))
                .thenReturn(new InstallationTimelineBundleResponse(null, "bundles/timeline-all", "timeline.json", "timeline.csv", "timeline.md", "timeline.html", "README.txt", GENERATED_AT, List.of("timeline.json")));
        when(fixture.installationDashboardService.report(null)).thenReturn(report);
        when(fixture.installationDashboardService.exportReport(null))
                .thenReturn(new InstallationReportExportResponse(null, "exports/report-all", "report.json", "report.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateReportBundle(null))
                .thenReturn(new InstallationReportBundleResponse(null, "bundles/report-all", "dashboard.json", "timeline.json", "latest-installation.json", "report.md", "report.html", "README.txt", GENERATED_AT, List.of("report.json")));
        when(fixture.installationDashboardService.health(null)).thenReturn(health);
        when(fixture.installationDashboardService.exportHealth(null))
                .thenReturn(new InstallationHealthExportResponse(null, "exports/health-all", "health.json", "health.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateHealthBundle(null))
                .thenReturn(new InstallationHealthBundleResponse(null, "bundles/health-all", "health.json", "health.md", "health.html", "README.txt", GENERATED_AT, List.of("health.json")));
        when(fixture.installationDashboardService.overview(null)).thenReturn(overview);
        when(fixture.installationDashboardService.exportOverview(null))
                .thenReturn(new InstallationOverviewExportResponse(null, "exports/overview-all", "overview.json", "overview.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateOverviewBundle(null))
                .thenReturn(new InstallationOverviewBundleResponse(null, "bundles/overview-all", "overview.json", "dashboard.json", "health.json", "report.json", "overview.md", "overview.html", "README.txt", GENERATED_AT, List.of("overview.json")));
        when(fixture.installationDashboardService.workspace(null)).thenReturn(workspace);
        when(fixture.installationDashboardService.exportWorkspace(null))
                .thenReturn(new InstallationWorkspaceExportResponse(null, "exports/workspace-all", "workspace.json", "workspace.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateWorkspaceBundle(null))
                .thenReturn(new InstallationWorkspaceBundleResponse(null, "bundles/workspace-all", "manifest.json", "README.txt", GENERATED_AT, List.of("manifest.json")));
        when(fixture.installationDashboardService.generateArtifactCatalog(null)).thenReturn(artifactCatalog);
        when(fixture.installationDashboardService.exportArtifactCatalog(null))
                .thenReturn(new InstallationArtifactCatalogExportResponse(null, "exports/artifacts-all", "catalog.json", "catalog.html", "README.txt", GENERATED_AT));
        when(fixture.installationDashboardService.generateArtifactCatalogBundle(null))
                .thenReturn(new InstallationArtifactCatalogBundleResponse(null, "bundles/artifacts-all", "catalog.json", "catalog.html", "README.txt", GENERATED_AT, List.of("catalog.json")));

        fixture.mockMvc.perform(get("/provisioning/installations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installationJobId").value("install-all"));

        fixture.mockMvc.perform(get("/provisioning/installations/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestInstallation.installationJobId").value("install-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/dashboard/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/dashboard-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/dashboard/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/dashboard-all"));

        fixture.mockMvc.perform(get("/provisioning/installations/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installationJobId").value("install-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/timeline/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/timeline-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/timeline/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/timeline-all"));

        fixture.mockMvc.perform(get("/provisioning/installations/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestInstallation.installationJobId").value("install-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/report/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/report-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/report/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/report-all"));

        fixture.mockMvc.perform(get("/provisioning/installations/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInstallations").value(1));

        fixture.mockMvc.perform(post("/provisioning/installations/health/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/health-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/health/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/health-all"));

        fixture.mockMvc.perform(get("/provisioning/installations/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboard.latestInstallation.installationJobId").value("install-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/overview/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/overview-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/overview/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/overview-all"));

        fixture.mockMvc.perform(get("/provisioning/installations/workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboard.latestInstallation.installationJobId").value("install-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/workspace/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/workspace-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/workspace/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/workspace-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/artifacts/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboardExport.exportDirectory").value("exports/dashboard-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/artifacts/catalog/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectory").value("exports/artifacts-all"));

        fixture.mockMvc.perform(post("/provisioning/installations/artifacts/catalog/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectory").value("bundles/artifacts-all"));

        verify(fixture.installationService).list(null);
        verify(fixture.installationDashboardService).dashboard(null);
        verify(fixture.installationDashboardService).exportDashboard(null);
        verify(fixture.installationDashboardService).generateBundle(null);
        verify(fixture.installationDashboardService).timeline(null);
        verify(fixture.installationDashboardService).exportTimeline(null);
        verify(fixture.installationDashboardService).generateTimelineBundle(null);
        verify(fixture.installationDashboardService).report(null);
        verify(fixture.installationDashboardService).exportReport(null);
        verify(fixture.installationDashboardService).generateReportBundle(null);
        verify(fixture.installationDashboardService).health(null);
        verify(fixture.installationDashboardService).exportHealth(null);
        verify(fixture.installationDashboardService).generateHealthBundle(null);
        verify(fixture.installationDashboardService).overview(null);
        verify(fixture.installationDashboardService).exportOverview(null);
        verify(fixture.installationDashboardService).generateOverviewBundle(null);
        verify(fixture.installationDashboardService).workspace(null);
        verify(fixture.installationDashboardService).exportWorkspace(null);
        verify(fixture.installationDashboardService).generateWorkspaceBundle(null);
        verify(fixture.installationDashboardService).generateArtifactCatalog(null);
        verify(fixture.installationDashboardService).exportArtifactCatalog(null);
        verify(fixture.installationDashboardService).generateArtifactCatalogBundle(null);
    }

    @Test
    void quoteSummaryAndSnapshotDashboardEndpointsForwardBodiesAndFilters() throws Exception {
        Fixture fixture = new Fixture();
        QuoteSnapshotResponse snapshot = snapshot("quote-1", "install-1", "cust-1");
        QuoteSnapshotSummaryResponse summary = new QuoteSnapshotSummaryResponse("install-1", "cust-1", 1, snapshot, null, 149.5, 149.5, 149.5, 112.0, null);

        when(fixture.quoteService.quote(any(String.class), any(CostEstimateRequest.class))).thenReturn(summaryForInstallation("install-1", "cust-1"));
        when(fixture.quoteSnapshotService.create(any(String.class), any(CostEstimateRequest.class))).thenReturn(snapshot);
        when(fixture.quoteSnapshotService.list("install-1", "cust-1")).thenReturn(List.of(snapshot));
        when(fixture.quoteSnapshotService.summary("install-1", "cust-1")).thenReturn(summary);
        when(fixture.quoteSnapshotService.exportDashboard("install-1", "cust-1"))
                .thenReturn(new QuoteSnapshotDashboardExportResponse("install-1", "cust-1", "exports/quotes", "dashboard.json", "dashboard.csv", "dashboard.html", "README.txt", GENERATED_AT));

        fixture.mockMvc.perform(post("/provisioning/installations/install-1/quote-summary")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(costRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installationJobId").value("install-1"))
                .andExpect(jsonPath("$.customerId").value("cust-1"));

        fixture.mockMvc.perform(post("/provisioning/installations/install-1/quote-snapshots")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(costRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteSnapshotId").value("quote-1"));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots")
                        .queryParam("installationJobId", "install-1")
                        .queryParam("customerId", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quoteSnapshotId").value("quote-1"));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/summary")
                        .queryParam("installationJobId", "install-1")
                        .queryParam("customerId", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotCount").value(1));

        fixture.mockMvc.perform(post("/provisioning/quote-snapshots/dashboard-export")
                        .queryParam("installationJobId", "install-1")
                        .queryParam("customerId", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectoryPath").value("exports/quotes"));

        ArgumentCaptor<CostEstimateRequest> requestCaptor = ArgumentCaptor.forClass(CostEstimateRequest.class);
        verify(fixture.quoteService).quote(eq("install-1"), requestCaptor.capture());
        assertEquals(1500L, requestCaptor.getValue().getMonthlyCallMinutes());
        verify(fixture.quoteSnapshotService).create(eq("install-1"), any(CostEstimateRequest.class));
        verify(fixture.quoteSnapshotService).list("install-1", "cust-1");
        verify(fixture.quoteSnapshotService).summary("install-1", "cust-1");
        verify(fixture.quoteSnapshotService).exportDashboard("install-1", "cust-1");
    }

    @Test
    void quoteSnapshotFilterEndpointsMayBeOmitted() throws Exception {
        Fixture fixture = new Fixture();
        QuoteSnapshotResponse snapshot = snapshot("quote-null", null, null);
        QuoteSnapshotSummaryResponse summary = new QuoteSnapshotSummaryResponse(null, null, 1, snapshot, null, 149.5, 149.5, 149.5, 112.0, null);

        when(fixture.quoteSnapshotService.list(null, null)).thenReturn(List.of(snapshot));
        when(fixture.quoteSnapshotService.summary(null, null)).thenReturn(summary);
        when(fixture.quoteSnapshotService.exportDashboard(null, null))
                .thenReturn(new QuoteSnapshotDashboardExportResponse(null, null, "exports/quotes-all", "dashboard.json", "dashboard.csv", "dashboard.html", "README.txt", GENERATED_AT));
        when(fixture.quoteSnapshotService.dashboard(null, null)).thenReturn(quoteDashboard(null, null, "quote-null"));
        when(fixture.quoteSnapshotService.generateDashboardBundle(null, null))
                .thenReturn(new QuoteSnapshotDashboardBundleResponse(null, null, "bundles/quote-dashboard-all", "summary.json", "timeline.json", "dashboard.md", "dashboard.html", "README.txt", GENERATED_AT));
        when(fixture.quoteSnapshotService.timeline(null, null))
                .thenReturn(List.of(new QuoteSnapshotTimelineEntryResponse(snapshot, null, false, null, null)));
        when(fixture.quoteSnapshotService.generateTimelineBundle(null, null))
                .thenReturn(new QuoteSnapshotTimelineBundleResponse(null, null, "bundles/quote-timeline-all", "timeline.json", "timeline.csv", "timeline.md", "README.txt", GENERATED_AT));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quoteSnapshotId").value("quote-null"));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotCount").value(1));

        fixture.mockMvc.perform(post("/provisioning/quote-snapshots/dashboard-export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportDirectoryPath").value("exports/quotes-all"));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.snapshotCount").value(1))
                .andExpect(jsonPath("$.timeline[0].snapshot.quoteSnapshotId").value("quote-null"));

        fixture.mockMvc.perform(post("/provisioning/quote-snapshots/dashboard-bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectoryPath").value("bundles/quote-dashboard-all"));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snapshot.quoteSnapshotId").value("quote-null"));

        fixture.mockMvc.perform(post("/provisioning/quote-snapshots/timeline/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectoryPath").value("bundles/quote-timeline-all"));

        verify(fixture.quoteSnapshotService).list(null, null);
        verify(fixture.quoteSnapshotService).summary(null, null);
        verify(fixture.quoteSnapshotService).exportDashboard(null, null);
        verify(fixture.quoteSnapshotService).dashboard(null, null);
        verify(fixture.quoteSnapshotService).generateDashboardBundle(null, null);
        verify(fixture.quoteSnapshotService).timeline(null, null);
        verify(fixture.quoteSnapshotService).generateTimelineBundle(null, null);
    }

    @Test
    void quoteDashboardAndTimelineEndpointsForwardFilters() throws Exception {
        Fixture fixture = new Fixture();
        QuoteSnapshotResponse snapshot = snapshot("quote-2", "install-2", "cust-2");

        when(fixture.quoteSnapshotService.dashboard("install-2", "cust-2")).thenReturn(quoteDashboard("install-2", "cust-2", "quote-2"));
        when(fixture.quoteSnapshotService.generateDashboardBundle("install-2", "cust-2"))
                .thenReturn(new QuoteSnapshotDashboardBundleResponse("install-2", "cust-2", "bundles/quote-dashboard", "summary.json", "timeline.json", "dashboard.md", "dashboard.html", "README.txt", GENERATED_AT));
        when(fixture.quoteSnapshotService.timeline("install-2", "cust-2"))
                .thenReturn(List.of(new QuoteSnapshotTimelineEntryResponse(snapshot, "quote-1", true, null, null)));
        when(fixture.quoteSnapshotService.generateTimelineBundle("install-2", "cust-2"))
                .thenReturn(new QuoteSnapshotTimelineBundleResponse("install-2", "cust-2", "bundles/quote-timeline", "timeline.json", "timeline.csv", "timeline.md", "README.txt", GENERATED_AT));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/dashboard")
                        .queryParam("installationJobId", "install-2")
                        .queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.snapshotCount").value(1))
                .andExpect(jsonPath("$.timeline[0].snapshot.quoteSnapshotId").value("quote-2"));

        fixture.mockMvc.perform(post("/provisioning/quote-snapshots/dashboard-bundle")
                        .queryParam("installationJobId", "install-2")
                        .queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectoryPath").value("bundles/quote-dashboard"));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/timeline")
                        .queryParam("installationJobId", "install-2")
                        .queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snapshot.quoteSnapshotId").value("quote-2"))
                .andExpect(jsonPath("$[0].comparisonAvailable").value(true));

        fixture.mockMvc.perform(post("/provisioning/quote-snapshots/timeline/bundle")
                        .queryParam("installationJobId", "install-2")
                        .queryParam("customerId", "cust-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectoryPath").value("bundles/quote-timeline"));

        verify(fixture.quoteSnapshotService).dashboard("install-2", "cust-2");
        verify(fixture.quoteSnapshotService).generateDashboardBundle("install-2", "cust-2");
        verify(fixture.quoteSnapshotService).timeline("install-2", "cust-2");
        verify(fixture.quoteSnapshotService).generateTimelineBundle("install-2", "cust-2");
    }

    @Test
    void quoteComparisonAndBundleEndpointsForwardIdsAndQueryParams() throws Exception {
        Fixture fixture = new Fixture();
        QuoteSnapshotResponse base = snapshot("quote-1", "install-1", "cust-1");
        QuoteSnapshotResponse target = snapshot("quote-2", "install-1", "cust-1");
        QuoteSnapshotComparisonResponse comparison = new QuoteSnapshotComparisonResponse(
                "quote-1",
                "quote-2",
                GENERATED_AT,
                base,
                target,
                new CommercialAssumptionsDeltaResponse(100L, 10L, 5L, 1.0, 1, 2, 3.0),
                new CostEstimateDeltaResponse(1.0, 2.0, 3.0, 4.0, 5.0)
        );

        when(fixture.quoteSnapshotService.get("quote-1")).thenReturn(base);
        when(fixture.quoteSnapshotService.getAssumptions("quote-1"))
                .thenReturn(new CommercialAssumptionsResponse("preset", 1500L, 200L, 30L, 4.5, 3, 12, 30.0));
        when(fixture.quoteSnapshotService.compare("quote-1", "quote-2")).thenReturn(comparison);
        when(fixture.quoteSnapshotService.compareToPrevious("quote-2"))
                .thenReturn(new QuoteSnapshotPreviousComparisonResponse("quote-2", "quote-1", GENERATED_AT, true, "Compared", comparison));
        when(fixture.quoteSnapshotService.generatePreviousComparisonBundle("quote-2"))
                .thenReturn(new QuoteSnapshotComparisonBundleResponse("quote-2", "quote-1", true, "bundles/compare", "comparison.json", "summary.md", "README.txt", GENERATED_AT));
        when(fixture.quoteSnapshotService.generateBundle("quote-2"))
                .thenReturn(new QuoteSnapshotBundleResponse("quote-2", "bundles/quote", "summary.json", "assumptions.json", "request.json", "quote.csv", "README.txt", GENERATED_AT));
        when(fixture.quoteSnapshotService.generateProposal("quote-2"))
                .thenReturn(new QuoteProposalResponse("quote-2", "bundles/proposal", "proposal.md", "proposal.html", "assumptions.json", "pricing.json", GENERATED_AT));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/quote-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteSnapshotId").value("quote-1"));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/quote-1/assumptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyCallMinutes").value(1500));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/compare")
                        .queryParam("baseQuoteSnapshotId", "quote-1")
                        .queryParam("targetQuoteSnapshotId", "quote-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseQuoteSnapshotId").value("quote-1"))
                .andExpect(jsonPath("$.targetQuoteSnapshotId").value("quote-2"));

        fixture.mockMvc.perform(get("/provisioning/quote-snapshots/quote-2/compare-previous"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparisonAvailable").value(true));

        fixture.mockMvc.perform(post("/provisioning/quote-snapshots/quote-2/compare-previous-bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectoryPath").value("bundles/compare"));

        fixture.mockMvc.perform(post("/provisioning/quote-snapshots/quote-2/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleDirectoryPath").value("bundles/quote"));

        fixture.mockMvc.perform(post("/provisioning/quote-snapshots/quote-2/proposal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalDirectoryPath").value("bundles/proposal"));

        verify(fixture.quoteSnapshotService).get("quote-1");
        verify(fixture.quoteSnapshotService).getAssumptions("quote-1");
        verify(fixture.quoteSnapshotService).compare("quote-1", "quote-2");
        verify(fixture.quoteSnapshotService).compareToPrevious("quote-2");
        verify(fixture.quoteSnapshotService).generatePreviousComparisonBundle("quote-2");
        verify(fixture.quoteSnapshotService).generateBundle("quote-2");
        verify(fixture.quoteSnapshotService).generateProposal("quote-2");
    }

    private static CustomerInstallationResponse installation(String installationJobId, String customerId) {
        return new CustomerInstallationResponse(
                installationJobId,
                customerId,
                "Acme Install",
                "SOFTPHONE",
                "COMPLETED",
                false,
                true,
                true,
                0,
                "pkg-1",
                "deploy-1",
                List.of(),
                null,
                null,
                GENERATED_AT.minusSeconds(300),
                GENERATED_AT,
                "Done",
                null
        );
    }

    private static CustomerBootstrapBundleResponse bootstrapBundle(String installationJobId) {
        return new CustomerBootstrapBundleResponse(
                installationJobId,
                "Acme Install",
                "bundles/bootstrap",
                "summary.json",
                "customer.json",
                "commercial.json",
                "app.env",
                "agents.json",
                "softphone.env",
                "ui.json",
                "handoff.md",
                "README.txt",
                GENERATED_AT,
                List.of("summary.json")
        );
    }

    private static InstallationHandoffBundleResponse handoffBundle(String installationJobId) {
        return new InstallationHandoffBundleResponse(
                installationJobId,
                "Acme Install",
                "bundles/handoff",
                "installation.json",
                "bootstrap.json",
                "quote-summary.json",
                "quote-dashboard.json",
                "handoff.md",
                "handoff.html",
                "README.txt",
                GENERATED_AT,
                List.of("handoff.json")
        );
    }

    private static CustomerDeliveryPackageResponse deliveryPackage(String installationJobId, String customerId) {
        return new CustomerDeliveryPackageResponse(
                installationJobId,
                customerId,
                "packages/install-1",
                "manifest.json",
                "README.txt",
                GENERATED_AT,
                List.of("manifest.json")
        );
    }

    private static CustomerDeliveryPackageDetailResponse deliveryPackageDetail(String installationJobId, String customerId) {
        return new CustomerDeliveryPackageDetailResponse(
                installationJobId,
                customerId,
                GENERATED_AT,
                installation(installationJobId, customerId),
                null,
                null,
                null
        );
    }

    private static CustomerDeliveryPackageExportResponse deliveryExport(String installationJobId) {
        return new CustomerDeliveryPackageExportResponse(
                installationJobId,
                "exports/install-1",
                "package.json",
                "package.html",
                "README.txt",
                GENERATED_AT
        );
    }

    private static CostEstimateRequest costRequest() {
        CostEstimateRequest request = new CostEstimateRequest();
        request.setCustomerId("cust-1");
        request.setMonthlyCallMinutes(1500L);
        request.setDesiredMarginPercent(30.0);
        return request;
    }

    private static InstallationQuoteSummaryResponse summaryForInstallation(String installationJobId, String customerId) {
        return new InstallationQuoteSummaryResponse(
                installationJobId,
                customerId,
                "Acme Install",
                "Acme",
                "SOFTPHONE",
                "COMPLETED",
                0,
                List.of(),
                null,
                new CommercialAssumptionsResponse("preset", 1500L, 200L, 30L, 4.5, 3, 12, 30.0),
                new CostEstimateResponse(customerId, "config-1", 40.0, 72.0, 112.0, 149.5, 30.0),
                GENERATED_AT
        );
    }

    private static QuoteSnapshotResponse snapshot(String quoteSnapshotId, String installationJobId, String customerId) {
        return new QuoteSnapshotResponse(
                quoteSnapshotId,
                installationJobId,
                customerId,
                "config-1",
                "quotes/" + quoteSnapshotId + ".json",
                GENERATED_AT,
                null
        );
    }

    private static QuoteSnapshotDashboardResponse quoteDashboard(String installationJobId, String customerId, String quoteSnapshotId) {
        QuoteSnapshotResponse snapshot = snapshot(quoteSnapshotId, installationJobId, customerId);
        QuoteSnapshotSummaryResponse summary = new QuoteSnapshotSummaryResponse(
                installationJobId,
                customerId,
                1,
                snapshot,
                null,
                149.5,
                149.5,
                149.5,
                112.0,
                null
        );
        return new QuoteSnapshotDashboardResponse(
                installationJobId,
                customerId,
                GENERATED_AT,
                summary,
                List.of(new QuoteSnapshotTimelineEntryResponse(snapshot, null, false, null, null))
        );
    }

    private static CustomerOperationsWorkspaceResponse customerOperationsWorkspaceResponse(String customerId) {
        return invokeCustomerFixture("customerOperationsWorkspaceResponse", new Class<?>[]{String.class}, customerId);
    }

    private static CustomerOperationsWorkspaceBundleResponse customerOperationsWorkspaceBundleResponse(String customerId, String bundleDirectory) {
        return invokeCustomerFixture("customerOperationsWorkspaceBundleResponse", new Class<?>[]{String.class, String.class}, customerId, bundleDirectory);
    }

    private static CustomerHealthResponse customerHealthResponse(String customerId) {
        return invokeCustomerFixture("customerHealthResponse", new Class<?>[]{String.class}, customerId);
    }

    private static CustomerHealthExportResponse customerHealthExportResponse(String customerId, String exportDirectory) {
        return invokeCustomerFixture("customerHealthExportResponse", new Class<?>[]{String.class, String.class}, customerId, exportDirectory);
    }

    private static CustomerHealthBundleResponse customerHealthBundleResponse(String customerId, String bundleDirectory) {
        return invokeCustomerFixture("customerHealthBundleResponse", new Class<?>[]{String.class, String.class}, customerId, bundleDirectory);
    }

    private static CustomerOverviewResponse customerOverviewResponse(String customerId) {
        return invokeCustomerFixture("customerOverviewResponse", new Class<?>[]{String.class}, customerId);
    }

    private static CustomerOverviewExportResponse customerOverviewExportResponse(String customerId, String exportDirectory) {
        return invokeCustomerFixture("customerOverviewExportResponse", new Class<?>[]{String.class, String.class}, customerId, exportDirectory);
    }

    private static CustomerOverviewBundleResponse customerOverviewBundleResponse(String customerId, String bundleDirectory) {
        return invokeCustomerFixture("customerOverviewBundleResponse", new Class<?>[]{String.class, String.class}, customerId, bundleDirectory);
    }

    private static CustomerArtifactCatalogResponse customerArtifactCatalogResponse(String customerId) {
        return invokeCustomerFixture("customerArtifactCatalogResponse", new Class<?>[]{String.class}, customerId);
    }

    private static CustomerArtifactCatalogExportResponse customerArtifactCatalogExportResponse(String customerId, String exportDirectory) {
        return invokeCustomerFixture("customerArtifactCatalogExportResponse", new Class<?>[]{String.class, String.class}, customerId, exportDirectory);
    }

    private static CustomerArtifactCatalogBundleResponse customerArtifactCatalogBundleResponse(String customerId, String bundleDirectory) {
        return invokeCustomerFixture("customerArtifactCatalogBundleResponse", new Class<?>[]{String.class, String.class}, customerId, bundleDirectory);
    }

    private static CustomerDeliveryCenterResponse customerDeliveryCenterResponse(String customerId) {
        return invokeCustomerFixture("customerDeliveryCenterResponse", new Class<?>[]{String.class}, customerId);
    }

    private static CustomerDeliveryCenterExportResponse customerDeliveryCenterExportResponse(String customerId, String exportDirectory) {
        return invokeCustomerFixture("customerDeliveryCenterExportResponse", new Class<?>[]{String.class, String.class}, customerId, exportDirectory);
    }

    private static CustomerDeliveryCenterBundleResponse customerDeliveryCenterBundleResponse(String customerId, String bundleDirectory) {
        return invokeCustomerFixture("customerDeliveryCenterBundleResponse", new Class<?>[]{String.class, String.class}, customerId, bundleDirectory);
    }

    private static CustomerAccountCenterResponse customerAccountCenterResponse(String customerId) {
        return invokeCustomerFixture("customerAccountCenterResponse", new Class<?>[]{String.class}, customerId);
    }

    private static CustomerAccountCenterExportResponse customerAccountCenterExportResponse(String customerId, String exportDirectory) {
        return invokeCustomerFixture("customerAccountCenterExportResponse", new Class<?>[]{String.class, String.class}, customerId, exportDirectory);
    }

    private static CustomerAccountCenterBundleResponse customerAccountCenterBundleResponse(String customerId, String bundleDirectory) {
        return invokeCustomerFixture("customerAccountCenterBundleResponse", new Class<?>[]{String.class, String.class}, customerId, bundleDirectory);
    }

    private static CustomerReportResponse customerReportResponse(String customerId) {
        return new CustomerReportResponse(
                customerId,
                GENERATED_AT,
                customerHealthResponse(customerId),
                customerOverviewResponse(customerId),
                customerDeliveryCenterResponse(customerId),
                false,
                true,
                true,
                "Needs attention",
                "install-1",
                "Acme Softphone",
                "COMPLETED",
                "quote-1",
                149.50
        );
    }

    private static CustomerReportExportResponse customerReportExportResponse(String customerId, String exportDirectory) {
        return invokeCustomerFixture("customerReportExportResponse", new Class<?>[]{String.class, String.class}, customerId, exportDirectory);
    }

    private static CustomerReportBundleResponse customerReportBundleResponse(String customerId, String bundleDirectory) {
        return invokeCustomerFixture("customerReportBundleResponse", new Class<?>[]{String.class, String.class}, customerId, bundleDirectory);
    }

    private static CustomerCommandCenterResponse customerCommandCenterResponse(int totalCustomers, int healthyCustomers) {
        return invokePlatformFixture("customerCommandCenterResponse", new Class<?>[]{int.class, int.class}, totalCustomers, healthyCustomers);
    }

    private static PlatformControlCenterResponse platformControlCenterResponse() {
        return record(PlatformControlCenterResponse.class, Map.of(
                "generatedAt", GENERATED_AT,
                "healthyCustomers", 2,
                "customersWithReports", 2,
                "customersWithArtifactCatalog", 2,
                "healthy", true,
                "statusMessage", "Platform control center ready",
                "latestDeploymentProvider", "ASTERISK",
                "recentDeploymentProviders", List.of("ASTERISK"),
                "latestDeploymentSummary", record(RecentDeploymentSummaryResponse.class, Map.of(
                        "deploymentJobId", "job-2",
                        "provider", "ASTERISK",
                        "status", "DEPLOYED"
                ))
        ));
    }

    private static PlatformWorkspaceResponse platformWorkspaceResponse(PlatformControlCenterResponse controlCenter) {
        return record(PlatformWorkspaceResponse.class, Map.of(
                "generatedAt", GENERATED_AT,
                "controlCenter", controlCenter,
                "healthyCustomers", 2,
                "customersWithReports", 2,
                "customersWithArtifactCatalog", 2,
                "healthy", true,
                "statusMessage", "Platform workspace ready",
                "latestDeploymentProvider", "ASTERISK",
                "recentDeploymentProviders", List.of("ASTERISK")
        ));
    }

    private static PlatformHealthResponse platformHealthResponse() {
        return record(PlatformHealthResponse.class, fields(
                field("generatedAt", GENERATED_AT),
                field("totalCustomers", 3),
                field("healthyCustomers", 2),
                field("customersWithInstallations", 3),
                field("customersWithQuotes", 3),
                field("customersWithDeliveryPackage", 2),
                field("customersWithReports", 2),
                field("customersWithArtifactCatalog", 2),
                field("totalDeployments", 1),
                field("pendingDeployments", 0),
                field("failedDeployments", 0),
                field("healthy", true),
                field("statusMessage", "Platform health ready"),
                field("latestDeploymentProvider", "ASTERISK"),
                field("recentDeploymentProviders", List.of("ASTERISK"))
        ));
    }

    private static PlatformOverviewResponse platformOverviewResponse(PlatformHealthResponse health,
                                                                     PlatformControlCenterResponse controlCenter,
                                                                     PlatformWorkspaceResponse workspace) {
        return record(PlatformOverviewResponse.class, Map.of(
                "generatedAt", GENERATED_AT,
                "health", health,
                "controlCenter", controlCenter,
                "workspace", workspace,
                "healthy", true,
                "statusMessage", "Platform overview ready",
                "latestDeploymentProvider", "ASTERISK",
                "recentDeploymentProviders", List.of("ASTERISK"),
                "report", platformReportResponse(health, controlCenter, workspace, platformArtifactCatalogResponse(), platformDeliveryPackageDetailResponse(health, controlCenter, workspace, platformArtifactCatalogResponse()))
        ));
    }

    private static PlatformArtifactCatalogResponse platformArtifactCatalogResponse() {
        return record(PlatformArtifactCatalogResponse.class, Map.of(
                "generatedAt", GENERATED_AT,
                "totalCustomers", 3,
                "healthyCustomers", 2,
                "customersWithReports", 2,
                "customersWithArtifactCatalog", 2,
                "healthy", true,
                "statusMessage", "Platform artifacts ready",
                "latestDeploymentProvider", "ASTERISK",
                "recentDeploymentProviders", List.of("ASTERISK"),
                "controlCenterExport", new PlatformControlCenterExportResponse("exports/platform-control-center", "control-center.json", "control-center.html", "README.txt", GENERATED_AT)
        ));
    }

    private static PlatformDeliveryPackageDetailResponse platformDeliveryPackageDetailResponse(PlatformHealthResponse health,
                                                                                               PlatformControlCenterResponse controlCenter,
                                                                                               PlatformWorkspaceResponse workspace,
                                                                                               PlatformArtifactCatalogResponse artifactCatalog) {
        return record(PlatformDeliveryPackageDetailResponse.class, Map.of(
                "generatedAt", GENERATED_AT,
                "health", health,
                "controlCenter", controlCenter,
                "workspace", workspace,
                "artifactCatalog", artifactCatalog,
                "healthy", true,
                "statusMessage", "Platform delivery ready",
                "latestDeploymentProvider", "ASTERISK",
                "recentDeploymentProviders", List.of("ASTERISK")
        ));
    }

    private static PlatformReportResponse platformReportResponse(PlatformHealthResponse health,
                                                                 PlatformControlCenterResponse controlCenter,
                                                                 PlatformWorkspaceResponse workspace,
                                                                 PlatformArtifactCatalogResponse artifactCatalog,
                                                                 PlatformDeliveryPackageDetailResponse deliveryPackage) {
        return record(PlatformReportResponse.class, Map.of(
                "generatedAt", GENERATED_AT,
                "health", health,
                "controlCenter", controlCenter,
                "workspace", workspace,
                "artifactCatalog", artifactCatalog,
                "deliveryPackage", deliveryPackage,
                "healthy", true,
                "statusMessage", "Platform report ready",
                "latestDeploymentProvider", "ASTERISK",
                "recentDeploymentProviders", List.of("ASTERISK")
        ));
    }

    private static PlatformReportBundleResponse platformReportBundleResponse() {
        return new PlatformReportBundleResponse(
                "bundles/platform-report",
                "report.json",
                "report.html",
                "README.txt",
                GENERATED_AT,
                List.of("report.json")
        );
    }

    @SafeVarargs
    private static Map<String, Object> fields(Map.Entry<String, Object>... entries) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            values.put(entry.getKey(), entry.getValue());
        }
        return values;
    }

    private static Map.Entry<String, Object> field(String name, Object value) {
        return Map.entry(name, value);
    }

    private static <T> T record(Class<T> type, Map<String, Object> overrides) {
        try {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] parameterTypes = new Class<?>[components.length];
            Object[] args = new Object[components.length];
            for (int index = 0; index < components.length; index++) {
                RecordComponent component = components[index];
                parameterTypes[index] = component.getType();
                args[index] = overrides.containsKey(component.getName())
                        ? overrides.get(component.getName())
                        : defaultValue(component.getType());
            }
            Constructor<T> constructor = type.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to build record " + type.getName(), exception);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            if (Instant.class.equals(type)) {
                return GENERATED_AT;
            }
            if (List.class.isAssignableFrom(type)) {
                return List.of();
            }
            if (Map.class.isAssignableFrom(type)) {
                return Map.of();
            }
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (int.class.equals(type)) {
            return 0;
        }
        if (long.class.equals(type)) {
            return 0L;
        }
        if (double.class.equals(type)) {
            return 0.0d;
        }
        if (float.class.equals(type)) {
            return 0.0f;
        }
        if (short.class.equals(type)) {
            return (short) 0;
        }
        if (byte.class.equals(type)) {
            return (byte) 0;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeCustomerFixture(String methodName, Class<?>[] parameterTypes, Object... args) {
        return (T) invokeFixture("com.vantage.dialer.api.service.CustomerServiceTestFixtures", methodName, parameterTypes, args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokePlatformFixture(String methodName, Class<?>[] parameterTypes, Object... args) {
        return (T) invokeFixture("com.vantage.dialer.api.service.PlatformServiceTestFixtures", methodName, parameterTypes, args);
    }

    private static Object invokeFixture(String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> fixtureClass = Class.forName(className);
            Method method = fixtureClass.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke fixture " + className + "." + methodName, exception);
        }
    }

    private static final class Fixture {
        private final CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        private final CustomerBootstrapBundleService bundleService = mock(CustomerBootstrapBundleService.class);
        private final CustomerAccountCenterService customerAccountCenterService = mock(CustomerAccountCenterService.class);
        private final CustomerArtifactCatalogService customerArtifactCatalogService = mock(CustomerArtifactCatalogService.class);
        private final CustomerCommandCenterService customerCommandCenterService = mock(CustomerCommandCenterService.class);
        private final CustomerDeliveryCenterService customerDeliveryCenterService = mock(CustomerDeliveryCenterService.class);
        private final CustomerDeliveryPackageService deliveryPackageService = mock(CustomerDeliveryPackageService.class);
        private final CustomerHealthService customerHealthService = mock(CustomerHealthService.class);
        private final CustomerOverviewService customerOverviewService = mock(CustomerOverviewService.class);
        private final CustomerOperationsWorkspaceService customerOperationsWorkspaceService = mock(CustomerOperationsWorkspaceService.class);
        private final CustomerPortfolioService customerPortfolioService = mock(CustomerPortfolioService.class);
        private final CustomerReportService customerReportService = mock(CustomerReportService.class);
        private final PlatformControlCenterService platformControlCenterService = mock(PlatformControlCenterService.class);
        private final PlatformArtifactCatalogService platformArtifactCatalogService = mock(PlatformArtifactCatalogService.class);
        private final PlatformDeliveryPackageService platformDeliveryPackageService = mock(PlatformDeliveryPackageService.class);
        private final PlatformHealthService platformHealthService = mock(PlatformHealthService.class);
        private final PlatformOverviewService platformOverviewService = mock(PlatformOverviewService.class);
        private final PlatformReportService platformReportService = mock(PlatformReportService.class);
        private final PlatformWorkspaceService platformWorkspaceService = mock(PlatformWorkspaceService.class);
        private final InstallationHandoffBundleService handoffBundleService = mock(InstallationHandoffBundleService.class);
        private final InstallationDashboardService installationDashboardService = mock(InstallationDashboardService.class);
        private final CustomerQuoteService quoteService = mock(CustomerQuoteService.class);
        private final QuoteSnapshotService quoteSnapshotService = mock(QuoteSnapshotService.class);
        private final MockMvc mockMvc;

        private Fixture() {
            ProvisioningController controller = new ProvisioningController(
                    installationService,
                    bundleService,
                    customerAccountCenterService,
                    customerArtifactCatalogService,
                    customerCommandCenterService,
                    customerDeliveryCenterService,
                    deliveryPackageService,
                    customerHealthService,
                    customerOverviewService,
                    customerOperationsWorkspaceService,
                    customerPortfolioService,
                    customerReportService,
                    platformControlCenterService,
                    platformArtifactCatalogService,
                    platformDeliveryPackageService,
                    platformHealthService,
                    platformOverviewService,
                    platformReportService,
                    platformWorkspaceService,
                    handoffBundleService,
                    installationDashboardService,
                    quoteService,
                    quoteSnapshotService
            );
            this.mockMvc = ControllerTestSupport.mockMvc(controller);
        }
    }
}
