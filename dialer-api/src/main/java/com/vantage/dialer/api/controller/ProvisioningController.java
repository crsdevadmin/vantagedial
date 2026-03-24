package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.CustomerBootstrapBundleResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageExportResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterResponse;
import com.vantage.dialer.api.dto.CustomerHealthBundleResponse;
import com.vantage.dialer.api.dto.CustomerHealthExportResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
import com.vantage.dialer.api.dto.CustomerOverviewBundleResponse;
import com.vantage.dialer.api.dto.CustomerOverviewExportResponse;
import com.vantage.dialer.api.dto.CustomerOverviewResponse;
import com.vantage.dialer.api.dto.CustomerReportBundleResponse;
import com.vantage.dialer.api.dto.CustomerReportExportResponse;
import com.vantage.dialer.api.dto.CustomerReportResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceExportResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioBundleResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioExportResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterBundleResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterExportResponse;
import com.vantage.dialer.api.dto.PlatformControlCenterResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.PlatformArtifactCatalogResponse;
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
import com.vantage.dialer.api.dto.PlatformWorkspaceExportResponse;
import com.vantage.dialer.api.dto.PlatformWorkspaceResponse;
import com.vantage.dialer.api.dto.CustomerInstallationRequest;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.CommercialAssumptionsResponse;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.InstallationHandoffBundleResponse;
import com.vantage.dialer.api.dto.InstallationHandoffExportResponse;
import com.vantage.dialer.api.dto.InstallationHandoffResponse;
import com.vantage.dialer.api.dto.InstallationDashboardBundleResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.InstallationDashboardExportResponse;
import com.vantage.dialer.api.dto.InstallationDashboardResponse;
import com.vantage.dialer.api.dto.InstallationHealthBundleResponse;
import com.vantage.dialer.api.dto.InstallationHealthExportResponse;
import com.vantage.dialer.api.dto.InstallationHealthResponse;
import com.vantage.dialer.api.dto.InstallationOverviewBundleResponse;
import com.vantage.dialer.api.dto.InstallationOverviewExportResponse;
import com.vantage.dialer.api.dto.InstallationOverviewResponse;
import com.vantage.dialer.api.dto.InstallationReportBundleResponse;
import com.vantage.dialer.api.dto.InstallationReportExportResponse;
import com.vantage.dialer.api.dto.InstallationReportResponse;
import com.vantage.dialer.api.dto.InstallationTimelineBundleResponse;
import com.vantage.dialer.api.dto.InstallationTimelineEntryResponse;
import com.vantage.dialer.api.dto.InstallationTimelineExportResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceExportResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceResponse;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.QuoteProposalResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotComparisonBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotComparisonResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardExportResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotPreviousComparisonResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotTimelineEntryResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotTimelineBundleResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotSummaryResponse;
import com.vantage.dialer.api.service.CustomerBootstrapBundleService;
import com.vantage.dialer.api.service.CustomerAccountCenterService;
import com.vantage.dialer.api.service.CustomerArtifactCatalogService;
import com.vantage.dialer.api.service.CustomerCommandCenterService;
import com.vantage.dialer.api.service.CustomerDeliveryPackageService;
import com.vantage.dialer.api.service.CustomerDeliveryCenterService;
import com.vantage.dialer.api.service.CustomerHealthService;
import com.vantage.dialer.api.service.CustomerInstallationService;
import com.vantage.dialer.api.service.CustomerOverviewService;
import com.vantage.dialer.api.service.CustomerOperationsWorkspaceService;
import com.vantage.dialer.api.service.CustomerPortfolioService;
import com.vantage.dialer.api.service.CustomerReportService;
import com.vantage.dialer.api.service.PlatformControlCenterService;
import com.vantage.dialer.api.service.PlatformArtifactCatalogService;
import com.vantage.dialer.api.service.PlatformDeliveryPackageService;
import com.vantage.dialer.api.service.PlatformHealthService;
import com.vantage.dialer.api.service.PlatformOverviewService;
import com.vantage.dialer.api.service.PlatformReportService;
import com.vantage.dialer.api.service.PlatformWorkspaceService;
import com.vantage.dialer.api.service.CustomerQuoteService;
import com.vantage.dialer.api.service.InstallationHandoffBundleService;
import com.vantage.dialer.api.service.InstallationDashboardService;
import com.vantage.dialer.api.service.QuoteSnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/provisioning")
public class ProvisioningController {

    private final CustomerInstallationService installationService;
    private final CustomerBootstrapBundleService bundleService;
    private final CustomerAccountCenterService customerAccountCenterService;
    private final CustomerArtifactCatalogService customerArtifactCatalogService;
    private final CustomerCommandCenterService customerCommandCenterService;
    private final CustomerDeliveryCenterService customerDeliveryCenterService;
    private final CustomerDeliveryPackageService deliveryPackageService;
    private final CustomerHealthService customerHealthService;
    private final CustomerOverviewService customerOverviewService;
    private final CustomerOperationsWorkspaceService customerOperationsWorkspaceService;
    private final CustomerPortfolioService customerPortfolioService;
    private final CustomerReportService customerReportService;
    private final PlatformControlCenterService platformControlCenterService;
    private final PlatformArtifactCatalogService platformArtifactCatalogService;
    private final PlatformDeliveryPackageService platformDeliveryPackageService;
    private final PlatformHealthService platformHealthService;
    private final PlatformOverviewService platformOverviewService;
    private final PlatformReportService platformReportService;
    private final PlatformWorkspaceService platformWorkspaceService;
    private final InstallationHandoffBundleService handoffBundleService;
    private final InstallationDashboardService installationDashboardService;
    private final CustomerQuoteService quoteService;
    private final QuoteSnapshotService quoteSnapshotService;

    public ProvisioningController(CustomerInstallationService installationService,
                                  CustomerBootstrapBundleService bundleService,
                                  CustomerAccountCenterService customerAccountCenterService,
                                  CustomerArtifactCatalogService customerArtifactCatalogService,
                                  CustomerCommandCenterService customerCommandCenterService,
                                  CustomerDeliveryCenterService customerDeliveryCenterService,
                                  CustomerDeliveryPackageService deliveryPackageService,
                                  CustomerHealthService customerHealthService,
                                  CustomerOverviewService customerOverviewService,
                                  CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                  CustomerPortfolioService customerPortfolioService,
                                  CustomerReportService customerReportService,
                                  PlatformControlCenterService platformControlCenterService,
                                  PlatformArtifactCatalogService platformArtifactCatalogService,
                                  PlatformDeliveryPackageService platformDeliveryPackageService,
                                  PlatformHealthService platformHealthService,
                                  PlatformOverviewService platformOverviewService,
                                  PlatformReportService platformReportService,
                                  PlatformWorkspaceService platformWorkspaceService,
                                  InstallationHandoffBundleService handoffBundleService,
                                  InstallationDashboardService installationDashboardService,
                                  CustomerQuoteService quoteService,
                                  QuoteSnapshotService quoteSnapshotService) {
        this.installationService = installationService;
        this.bundleService = bundleService;
        this.customerAccountCenterService = customerAccountCenterService;
        this.customerArtifactCatalogService = customerArtifactCatalogService;
        this.customerCommandCenterService = customerCommandCenterService;
        this.customerDeliveryCenterService = customerDeliveryCenterService;
        this.deliveryPackageService = deliveryPackageService;
        this.customerHealthService = customerHealthService;
        this.customerOverviewService = customerOverviewService;
        this.customerOperationsWorkspaceService = customerOperationsWorkspaceService;
        this.customerPortfolioService = customerPortfolioService;
        this.customerReportService = customerReportService;
        this.platformControlCenterService = platformControlCenterService;
        this.platformArtifactCatalogService = platformArtifactCatalogService;
        this.platformDeliveryPackageService = platformDeliveryPackageService;
        this.platformHealthService = platformHealthService;
        this.platformOverviewService = platformOverviewService;
        this.platformReportService = platformReportService;
        this.platformWorkspaceService = platformWorkspaceService;
        this.handoffBundleService = handoffBundleService;
        this.installationDashboardService = installationDashboardService;
        this.quoteService = quoteService;
        this.quoteSnapshotService = quoteSnapshotService;
    }

    @PostMapping("/installations")
    public CustomerInstallationResponse install(@RequestBody CustomerInstallationRequest request) {
        return installationService.install(request);
    }

    @GetMapping("/installations")
    public List<CustomerInstallationResponse> list(@RequestParam(required = false) String customerId) {
        return installationService.list(customerId);
    }

    @GetMapping("/installations/{installationJobId}")
    public CustomerInstallationResponse get(@PathVariable String installationJobId) {
        return installationService.get(installationJobId);
    }

    @PostMapping("/installations/{installationJobId}/bootstrap-bundle")
    public CustomerBootstrapBundleResponse generateBootstrapBundle(@PathVariable String installationJobId) {
        return bundleService.generate(installationJobId);
    }

    @PostMapping("/installations/{installationJobId}/handoff-bundle")
    public InstallationHandoffBundleResponse generateHandoffBundle(@PathVariable String installationJobId) {
        return handoffBundleService.generate(installationJobId);
    }

    @PostMapping("/installations/{installationJobId}/delivery-package")
    public CustomerDeliveryPackageResponse generateCustomerDeliveryPackage(@PathVariable String installationJobId) {
        return deliveryPackageService.generate(installationJobId);
    }

    @GetMapping("/installations/{installationJobId}/delivery-package")
    public CustomerDeliveryPackageDetailResponse customerDeliveryPackage(@PathVariable String installationJobId) {
        return deliveryPackageService.detail(installationJobId);
    }

    @PostMapping("/installations/{installationJobId}/delivery-package/export")
    public CustomerDeliveryPackageExportResponse exportCustomerDeliveryPackage(@PathVariable String installationJobId) {
        return deliveryPackageService.export(installationJobId);
    }

    @GetMapping("/customers/{customerId}/workspace")
    public CustomerOperationsWorkspaceResponse customerOperationsWorkspace(@PathVariable String customerId) {
        return customerOperationsWorkspaceService.workspace(customerId);
    }

    @PostMapping("/customers/{customerId}/workspace/export")
    public CustomerOperationsWorkspaceExportResponse exportCustomerOperationsWorkspace(@PathVariable String customerId) {
        return customerOperationsWorkspaceService.export(customerId);
    }

    @PostMapping("/customers/{customerId}/workspace/bundle")
    public CustomerOperationsWorkspaceBundleResponse generateCustomerOperationsWorkspaceBundle(@PathVariable String customerId) {
        return customerOperationsWorkspaceService.generateBundle(customerId);
    }

    @GetMapping("/customers/{customerId}/health")
    public CustomerHealthResponse customerHealth(@PathVariable String customerId) {
        return customerHealthService.health(customerId);
    }

    @PostMapping("/customers/{customerId}/health/export")
    public CustomerHealthExportResponse exportCustomerHealth(@PathVariable String customerId) {
        return customerHealthService.export(customerId);
    }

    @PostMapping("/customers/{customerId}/health/bundle")
    public CustomerHealthBundleResponse generateCustomerHealthBundle(@PathVariable String customerId) {
        return customerHealthService.generateBundle(customerId);
    }

    @GetMapping("/customers/{customerId}/overview")
    public CustomerOverviewResponse customerOverview(@PathVariable String customerId) {
        return customerOverviewService.overview(customerId);
    }

    @PostMapping("/customers/{customerId}/overview/export")
    public CustomerOverviewExportResponse exportCustomerOverview(@PathVariable String customerId) {
        return customerOverviewService.export(customerId);
    }

    @PostMapping("/customers/{customerId}/overview/bundle")
    public CustomerOverviewBundleResponse generateCustomerOverviewBundle(@PathVariable String customerId) {
        return customerOverviewService.generateBundle(customerId);
    }

    @PostMapping("/customers/{customerId}/artifacts/catalog")
    public CustomerArtifactCatalogResponse customerArtifactCatalog(@PathVariable String customerId) {
        return customerArtifactCatalogService.catalog(customerId);
    }

    @PostMapping("/customers/{customerId}/artifacts/catalog/export")
    public CustomerArtifactCatalogExportResponse exportCustomerArtifactCatalog(@PathVariable String customerId) {
        return customerArtifactCatalogService.export(customerId);
    }

    @PostMapping("/customers/{customerId}/artifacts/catalog/bundle")
    public CustomerArtifactCatalogBundleResponse generateCustomerArtifactCatalogBundle(@PathVariable String customerId) {
        return customerArtifactCatalogService.generateBundle(customerId);
    }

    @GetMapping("/customers/{customerId}/delivery-package")
    public CustomerDeliveryCenterResponse customerDeliveryCenter(@PathVariable String customerId) {
        return customerDeliveryCenterService.deliveryPackage(customerId);
    }

    @PostMapping("/customers/{customerId}/delivery-package/export")
    public CustomerDeliveryCenterExportResponse exportCustomerDeliveryCenter(@PathVariable String customerId) {
        return customerDeliveryCenterService.export(customerId);
    }

    @PostMapping("/customers/{customerId}/delivery-package/bundle")
    public CustomerDeliveryCenterBundleResponse generateCustomerDeliveryCenterBundle(@PathVariable String customerId) {
        return customerDeliveryCenterService.generateBundle(customerId);
    }

    @GetMapping("/customers/{customerId}/report")
    public CustomerReportResponse customerReport(@PathVariable String customerId) {
        return customerReportService.report(customerId);
    }

    @PostMapping("/customers/{customerId}/report/export")
    public CustomerReportExportResponse exportCustomerReport(@PathVariable String customerId) {
        return customerReportService.export(customerId);
    }

    @PostMapping("/customers/{customerId}/report/bundle")
    public CustomerReportBundleResponse generateCustomerReportBundle(@PathVariable String customerId) {
        return customerReportService.generateBundle(customerId);
    }

    @GetMapping("/customers/{customerId}/account")
    public CustomerAccountCenterResponse customerAccountCenter(@PathVariable String customerId) {
        return customerAccountCenterService.account(customerId);
    }

    @PostMapping("/customers/{customerId}/account/export")
    public CustomerAccountCenterExportResponse exportCustomerAccountCenter(@PathVariable String customerId) {
        return customerAccountCenterService.export(customerId);
    }

    @PostMapping("/customers/{customerId}/account/bundle")
    public CustomerAccountCenterBundleResponse generateCustomerAccountCenterBundle(@PathVariable String customerId) {
        return customerAccountCenterService.generateBundle(customerId);
    }

    @GetMapping("/customers/portfolio")
    public CustomerPortfolioResponse customerPortfolio() {
        return customerPortfolioService.portfolio();
    }

    @PostMapping("/customers/portfolio/export")
    public CustomerPortfolioExportResponse exportCustomerPortfolio() {
        return customerPortfolioService.export();
    }

    @PostMapping("/customers/portfolio/bundle")
    public CustomerPortfolioBundleResponse generateCustomerPortfolioBundle() {
        return customerPortfolioService.generateBundle();
    }

    @GetMapping("/customers/command-center")
    public CustomerCommandCenterResponse customerCommandCenter() {
        return customerCommandCenterService.commandCenter();
    }

    @PostMapping("/customers/command-center/export")
    public CustomerCommandCenterExportResponse exportCustomerCommandCenter() {
        return customerCommandCenterService.export();
    }

    @PostMapping("/customers/command-center/bundle")
    public CustomerCommandCenterBundleResponse generateCustomerCommandCenterBundle() {
        return customerCommandCenterService.generateBundle();
    }

    @GetMapping("/platform/control-center")
    public PlatformControlCenterResponse platformControlCenter() {
        return platformControlCenterService.controlCenter();
    }

    @PostMapping("/platform/control-center/export")
    public PlatformControlCenterExportResponse exportPlatformControlCenter() {
        return platformControlCenterService.export();
    }

    @PostMapping("/platform/control-center/bundle")
    public PlatformControlCenterBundleResponse generatePlatformControlCenterBundle() {
        return platformControlCenterService.generateBundle();
    }

    @GetMapping("/platform/workspace")
    public PlatformWorkspaceResponse platformWorkspace() {
        return platformWorkspaceService.workspace();
    }

    @PostMapping("/platform/workspace/export")
    public PlatformWorkspaceExportResponse exportPlatformWorkspace() {
        return platformWorkspaceService.export();
    }

    @PostMapping("/platform/workspace/bundle")
    public PlatformWorkspaceBundleResponse generatePlatformWorkspaceBundle() {
        return platformWorkspaceService.generateBundle();
    }

    @PostMapping("/platform/artifacts/catalog")
    public PlatformArtifactCatalogResponse platformArtifactCatalog() {
        return platformArtifactCatalogService.catalog();
    }

    @PostMapping("/platform/artifacts/catalog/export")
    public PlatformArtifactCatalogExportResponse exportPlatformArtifactCatalog() {
        return platformArtifactCatalogService.export();
    }

    @PostMapping("/platform/artifacts/catalog/bundle")
    public PlatformArtifactCatalogBundleResponse generatePlatformArtifactCatalogBundle() {
        return platformArtifactCatalogService.generateBundle();
    }

    @GetMapping("/platform/delivery-package")
    public PlatformDeliveryPackageDetailResponse platformDeliveryPackage() {
        return platformDeliveryPackageService.detail();
    }

    @PostMapping("/platform/delivery-package/export")
    public PlatformDeliveryPackageExportResponse exportPlatformDeliveryPackage() {
        return platformDeliveryPackageService.export();
    }

    @PostMapping("/platform/delivery-package")
    public PlatformDeliveryPackageResponse generatePlatformDeliveryPackage() {
        return platformDeliveryPackageService.generate();
    }

    @GetMapping("/platform/report")
    public PlatformReportResponse platformReport() {
        return platformReportService.report();
    }

    @PostMapping("/platform/report/export")
    public PlatformReportExportResponse exportPlatformReport() {
        return platformReportService.export();
    }

    @PostMapping("/platform/report/bundle")
    public PlatformReportBundleResponse generatePlatformReportBundle() {
        return platformReportService.generateBundle();
    }

    @GetMapping("/platform/health")
    public PlatformHealthResponse platformHealth() {
        return platformHealthService.health();
    }

    @PostMapping("/platform/health/export")
    public PlatformHealthExportResponse exportPlatformHealth() {
        return platformHealthService.export();
    }

    @PostMapping("/platform/health/bundle")
    public PlatformHealthBundleResponse generatePlatformHealthBundle() {
        return platformHealthService.generateBundle();
    }

    @GetMapping("/platform/overview")
    public PlatformOverviewResponse platformOverview() {
        return platformOverviewService.overview();
    }

    @PostMapping("/platform/overview/export")
    public PlatformOverviewExportResponse exportPlatformOverview() {
        return platformOverviewService.export();
    }

    @PostMapping("/platform/overview/bundle")
    public PlatformOverviewBundleResponse generatePlatformOverviewBundle() {
        return platformOverviewService.generateBundle();
    }

    @GetMapping("/installations/{installationJobId}/handoff")
    public InstallationHandoffResponse installationHandoff(@PathVariable String installationJobId) {
        return handoffBundleService.handoff(installationJobId);
    }

    @PostMapping("/installations/{installationJobId}/handoff-export")
    public InstallationHandoffExportResponse exportInstallationHandoff(@PathVariable String installationJobId) {
        return handoffBundleService.export(installationJobId);
    }

    @GetMapping("/installations/dashboard")
    public InstallationDashboardResponse installationDashboard(@RequestParam(required = false) String customerId) {
        return installationDashboardService.dashboard(customerId);
    }

    @PostMapping("/installations/dashboard/export")
    public InstallationDashboardExportResponse exportInstallationDashboard(@RequestParam(required = false) String customerId) {
        return installationDashboardService.exportDashboard(customerId);
    }

    @PostMapping("/installations/dashboard/bundle")
    public InstallationDashboardBundleResponse generateInstallationDashboardBundle(@RequestParam(required = false) String customerId) {
        return installationDashboardService.generateBundle(customerId);
    }

    @GetMapping("/installations/timeline")
    public List<InstallationTimelineEntryResponse> installationTimeline(@RequestParam(required = false) String customerId) {
        return installationDashboardService.timeline(customerId);
    }

    @PostMapping("/installations/timeline/export")
    public InstallationTimelineExportResponse exportInstallationTimeline(@RequestParam(required = false) String customerId) {
        return installationDashboardService.exportTimeline(customerId);
    }

    @PostMapping("/installations/timeline/bundle")
    public InstallationTimelineBundleResponse generateInstallationTimelineBundle(@RequestParam(required = false) String customerId) {
        return installationDashboardService.generateTimelineBundle(customerId);
    }

    @GetMapping("/installations/report")
    public InstallationReportResponse installationReport(@RequestParam(required = false) String customerId) {
        return installationDashboardService.report(customerId);
    }

    @PostMapping("/installations/report/export")
    public InstallationReportExportResponse exportInstallationReport(@RequestParam(required = false) String customerId) {
        return installationDashboardService.exportReport(customerId);
    }

    @GetMapping("/installations/health")
    public InstallationHealthResponse installationHealth(@RequestParam(required = false) String customerId) {
        return installationDashboardService.health(customerId);
    }

    @GetMapping("/installations/overview")
    public InstallationOverviewResponse installationOverview(@RequestParam(required = false) String customerId) {
        return installationDashboardService.overview(customerId);
    }

    @PostMapping("/installations/overview/export")
    public InstallationOverviewExportResponse exportInstallationOverview(@RequestParam(required = false) String customerId) {
        return installationDashboardService.exportOverview(customerId);
    }

    @PostMapping("/installations/health/export")
    public InstallationHealthExportResponse exportInstallationHealth(@RequestParam(required = false) String customerId) {
        return installationDashboardService.exportHealth(customerId);
    }

    @PostMapping("/installations/health/bundle")
    public InstallationHealthBundleResponse generateInstallationHealthBundle(@RequestParam(required = false) String customerId) {
        return installationDashboardService.generateHealthBundle(customerId);
    }

    @PostMapping("/installations/report/bundle")
    public InstallationReportBundleResponse generateInstallationReportBundle(@RequestParam(required = false) String customerId) {
        return installationDashboardService.generateReportBundle(customerId);
    }

    @PostMapping("/installations/overview/bundle")
    public InstallationOverviewBundleResponse generateInstallationOverviewBundle(@RequestParam(required = false) String customerId) {
        return installationDashboardService.generateOverviewBundle(customerId);
    }

    @PostMapping("/installations/workspace/bundle")
    public InstallationWorkspaceBundleResponse generateInstallationWorkspaceBundle(@RequestParam(required = false) String customerId) {
        return installationDashboardService.generateWorkspaceBundle(customerId);
    }

    @GetMapping("/installations/workspace")
    public InstallationWorkspaceResponse installationWorkspace(@RequestParam(required = false) String customerId) {
        return installationDashboardService.workspace(customerId);
    }

    @PostMapping("/installations/workspace/export")
    public InstallationWorkspaceExportResponse exportInstallationWorkspace(@RequestParam(required = false) String customerId) {
        return installationDashboardService.exportWorkspace(customerId);
    }

    @PostMapping("/installations/artifacts/catalog")
    public InstallationArtifactCatalogResponse generateInstallationArtifactCatalog(@RequestParam(required = false) String customerId) {
        return installationDashboardService.generateArtifactCatalog(customerId);
    }

    @PostMapping("/installations/artifacts/catalog/export")
    public InstallationArtifactCatalogExportResponse exportInstallationArtifactCatalog(@RequestParam(required = false) String customerId) {
        return installationDashboardService.exportArtifactCatalog(customerId);
    }

    @PostMapping("/installations/artifacts/catalog/bundle")
    public InstallationArtifactCatalogBundleResponse generateInstallationArtifactCatalogBundle(@RequestParam(required = false) String customerId) {
        return installationDashboardService.generateArtifactCatalogBundle(customerId);
    }

    @PostMapping("/installations/{installationJobId}/quote-summary")
    public InstallationQuoteSummaryResponse quoteSummary(@PathVariable String installationJobId,
                                                         @RequestBody CostEstimateRequest request) {
        return quoteService.quote(installationJobId, request);
    }

    @PostMapping("/installations/{installationJobId}/quote-snapshots")
    public QuoteSnapshotResponse createQuoteSnapshot(@PathVariable String installationJobId,
                                                     @RequestBody CostEstimateRequest request) {
        return quoteSnapshotService.create(installationJobId, request);
    }

    @GetMapping("/quote-snapshots")
    public List<QuoteSnapshotResponse> listQuoteSnapshots(@RequestParam(required = false) String installationJobId,
                                                          @RequestParam(required = false) String customerId) {
        return quoteSnapshotService.list(installationJobId, customerId);
    }

    @GetMapping("/quote-snapshots/summary")
    public QuoteSnapshotSummaryResponse quoteSnapshotSummary(@RequestParam(required = false) String installationJobId,
                                                             @RequestParam(required = false) String customerId) {
        return quoteSnapshotService.summary(installationJobId, customerId);
    }

    @GetMapping("/quote-snapshots/dashboard")
    public QuoteSnapshotDashboardResponse quoteSnapshotDashboard(@RequestParam(required = false) String installationJobId,
                                                                 @RequestParam(required = false) String customerId) {
        return quoteSnapshotService.dashboard(installationJobId, customerId);
    }

    @PostMapping("/quote-snapshots/dashboard-export")
    public QuoteSnapshotDashboardExportResponse exportQuoteSnapshotDashboard(@RequestParam(required = false) String installationJobId,
                                                                             @RequestParam(required = false) String customerId) {
        return quoteSnapshotService.exportDashboard(installationJobId, customerId);
    }

    @PostMapping("/quote-snapshots/dashboard-bundle")
    public QuoteSnapshotDashboardBundleResponse generateQuoteSnapshotDashboardBundle(@RequestParam(required = false) String installationJobId,
                                                                                     @RequestParam(required = false) String customerId) {
        return quoteSnapshotService.generateDashboardBundle(installationJobId, customerId);
    }

    @GetMapping("/quote-snapshots/timeline")
    public List<QuoteSnapshotTimelineEntryResponse> quoteSnapshotTimeline(@RequestParam(required = false) String installationJobId,
                                                                          @RequestParam(required = false) String customerId) {
        return quoteSnapshotService.timeline(installationJobId, customerId);
    }

    @PostMapping("/quote-snapshots/timeline/bundle")
    public QuoteSnapshotTimelineBundleResponse generateQuoteSnapshotTimelineBundle(@RequestParam(required = false) String installationJobId,
                                                                                   @RequestParam(required = false) String customerId) {
        return quoteSnapshotService.generateTimelineBundle(installationJobId, customerId);
    }

    @GetMapping("/quote-snapshots/{quoteSnapshotId}")
    public QuoteSnapshotResponse getQuoteSnapshot(@PathVariable String quoteSnapshotId) {
        return quoteSnapshotService.get(quoteSnapshotId);
    }

    @GetMapping("/quote-snapshots/{quoteSnapshotId}/assumptions")
    public CommercialAssumptionsResponse getQuoteSnapshotAssumptions(@PathVariable String quoteSnapshotId) {
        return quoteSnapshotService.getAssumptions(quoteSnapshotId);
    }

    @GetMapping("/quote-snapshots/compare")
    public QuoteSnapshotComparisonResponse compareQuoteSnapshots(@RequestParam String baseQuoteSnapshotId,
                                                                 @RequestParam String targetQuoteSnapshotId) {
        return quoteSnapshotService.compare(baseQuoteSnapshotId, targetQuoteSnapshotId);
    }

    @GetMapping("/quote-snapshots/{quoteSnapshotId}/compare-previous")
    public QuoteSnapshotPreviousComparisonResponse compareQuoteSnapshotToPrevious(@PathVariable String quoteSnapshotId) {
        return quoteSnapshotService.compareToPrevious(quoteSnapshotId);
    }

    @PostMapping("/quote-snapshots/{quoteSnapshotId}/compare-previous-bundle")
    public QuoteSnapshotComparisonBundleResponse generatePreviousComparisonBundle(@PathVariable String quoteSnapshotId) {
        return quoteSnapshotService.generatePreviousComparisonBundle(quoteSnapshotId);
    }

    @PostMapping("/quote-snapshots/{quoteSnapshotId}/bundle")
    public QuoteSnapshotBundleResponse generateQuoteBundle(@PathVariable String quoteSnapshotId) {
        return quoteSnapshotService.generateBundle(quoteSnapshotId);
    }

    @PostMapping("/quote-snapshots/{quoteSnapshotId}/proposal")
    public QuoteProposalResponse generateQuoteProposal(@PathVariable String quoteSnapshotId) {
        return quoteSnapshotService.generateProposal(quoteSnapshotId);
    }
}
