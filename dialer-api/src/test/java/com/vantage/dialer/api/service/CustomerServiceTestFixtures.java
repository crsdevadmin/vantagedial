package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerAccountCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogResponse;
import com.vantage.dialer.api.dto.CommercialAssumptionsResponse;
import com.vantage.dialer.api.dto.CostEstimateResponse;
import com.vantage.dialer.api.dto.CustomerBootstrapBundleResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.CustomerHealthBundleResponse;
import com.vantage.dialer.api.dto.CustomerHealthExportResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceExportResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import com.vantage.dialer.api.dto.CustomerOverviewBundleResponse;
import com.vantage.dialer.api.dto.CustomerOverviewExportResponse;
import com.vantage.dialer.api.dto.CustomerOverviewResponse;
import com.vantage.dialer.api.dto.CustomerReportBundleResponse;
import com.vantage.dialer.api.dto.CustomerReportExportResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogResponse;
import com.vantage.dialer.api.dto.InstallationDashboardResponse;
import com.vantage.dialer.api.dto.InstallationHandoffBundleResponse;
import com.vantage.dialer.api.dto.InstallationHandoffExportResponse;
import com.vantage.dialer.api.dto.InstallationHandoffResponse;
import com.vantage.dialer.api.dto.InstallationHealthResponse;
import com.vantage.dialer.api.dto.InstallationOverviewResponse;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.InstallationReportResponse;
import com.vantage.dialer.api.dto.InstallationTimelineBundleResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceExportResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotSummaryResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotTimelineEntryResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

final class CustomerServiceTestFixtures {

    private static final Instant GENERATED_AT = Instant.parse("2026-03-22T12:00:00Z");
    private static final String INSTALLATION_NAME = "Acme Softphone";

    private CustomerServiceTestFixtures() {
    }

    static ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    static CustomerHealthResponse customerHealthResponse(String customerId) {
        return new CustomerHealthResponse(
                customerId,
                GENERATED_AT,
                false,
                "Needs attention",
                3,
                2,
                1,
                4,
                true,
                true,
                true,
                149.50,
                "install-1",
                INSTALLATION_NAME,
                "COMPLETED",
                "quote-1"
        );
    }

    static CustomerOperationsWorkspaceResponse customerOperationsWorkspaceResponse(String customerId) {
        CustomerInstallationResponse installation = customerInstallationResponse("install-1", customerId);
        InstallationWorkspaceResponse installationWorkspace = installationWorkspaceResponse(customerId);
        return new CustomerOperationsWorkspaceResponse(
                customerId,
                GENERATED_AT,
                installation,
                installationWorkspace.overview(),
                quoteSnapshotSummaryResponse(customerId),
                customerDeliveryPackageDetailResponse(customerId, installation.installationJobId()),
                true,
                true,
                true,
                true,
                "Customer workspace is healthy",
                installation.installationJobId(),
                installation.installationName(),
                installation.status(),
                "quote-1",
                149.50
        );
    }

    static CustomerAccountCenterResponse customerAccountCenterResponse(String customerId) {
        CustomerOperationsWorkspaceResponse workspace = customerOperationsWorkspaceResponse(customerId);
        return new CustomerAccountCenterResponse(
                customerId,
                GENERATED_AT,
                workspace,
                quoteSnapshotDashboardResponse(customerId),
                installationWorkspaceResponse(customerId),
                customerDeliveryPackageDetailResponse(customerId, "install-1"),
                false,
                true,
                true,
                true,
                true,
                true,
                "Needs attention",
                "install-1",
                "COMPLETED",
                "quote-1",
                149.50,
                INSTALLATION_NAME
        );
    }

    static CustomerOverviewResponse customerOverviewResponse(String customerId) {
        return new CustomerOverviewResponse(
                customerId,
                GENERATED_AT,
                customerHealthResponse(customerId),
                customerOperationsWorkspaceResponse(customerId),
                customerAccountCenterResponse(customerId),
                false,
                true,
                true,
                "Needs attention",
                "install-1",
                INSTALLATION_NAME,
                "COMPLETED",
                "quote-1",
                149.50
        );
    }

    static CustomerDeliveryCenterResponse customerDeliveryCenterResponse(String customerId) {
        return new CustomerDeliveryCenterResponse(
                customerId,
                GENERATED_AT,
                customerHealthResponse(customerId),
                customerOverviewResponse(customerId),
                customerAccountCenterResponse(customerId),
                customerArtifactCatalogResponse(customerId),
                false,
                true,
                true,
                "Needs attention",
                "install-1",
                INSTALLATION_NAME,
                "COMPLETED",
                "quote-1",
                149.50
        );
    }

    static CustomerInstallationResponse customerInstallationResponse(String installationJobId, String customerId) {
        return customerInstallationResponse(
                installationJobId,
                customerId,
                INSTALLATION_NAME,
                "SOFTPHONE",
                "COMPLETED",
                2,
                "deploy-1",
                "Installation completed",
                null
        );
    }

    static CustomerInstallationResponse customerInstallationResponse(String installationJobId,
                                                                     String customerId,
                                                                     String installationName,
                                                                     String clientType,
                                                                     String status,
                                                                     int agentCount,
                                                                     String deploymentJobId,
                                                                     String message,
                                                                     String errorMessage) {
        return new CustomerInstallationResponse(
                installationJobId,
                customerId,
                installationName,
                clientType,
                status,
                false,
                true,
                true,
                agentCount,
                "pkg-1",
                deploymentJobId,
                List.of(),
                null,
                null,
                GENERATED_AT.minusSeconds(900),
                GENERATED_AT.minusSeconds(300),
                message,
                errorMessage
        );
    }

    static InstallationHandoffResponse installationHandoffResponse(String installationJobId, String customerId) {
        return new InstallationHandoffResponse(
                installationJobId,
                customerId,
                INSTALLATION_NAME,
                GENERATED_AT,
                customerInstallationResponse(installationJobId, customerId),
                customerBootstrapBundleResponse(installationJobId, "bootstrap-bundle"),
                installationQuoteSummaryResponse(installationJobId, customerId),
                quoteSnapshotDashboardResponse(customerId)
        );
    }

    static InstallationWorkspaceResponse installationWorkspaceResponse(String customerId) {
        CustomerInstallationResponse installation = customerInstallationResponse("install-1", customerId);
        InstallationDashboardResponse dashboard = new InstallationDashboardResponse(
                customerId,
                GENERATED_AT,
                1,
                1,
                0,
                0,
                0,
                installation.agentCount(),
                installation,
                List.of(installation)
        );
        InstallationHealthResponse health = new InstallationHealthResponse(
                customerId,
                GENERATED_AT,
                1,
                1,
                0,
                0,
                0,
                Map.of("SOFTPHONE", 1),
                List.of()
        );
        InstallationReportResponse report = new InstallationReportResponse(
                customerId,
                GENERATED_AT,
                dashboard,
                installation,
                List.of()
        );
        InstallationOverviewResponse overview = new InstallationOverviewResponse(
                customerId,
                GENERATED_AT,
                dashboard,
                health,
                report
        );
        return new InstallationWorkspaceResponse(
                customerId,
                GENERATED_AT,
                dashboard,
                new InstallationTimelineBundleResponse(
                        customerId,
                        "timeline-bundle",
                        "timeline-bundle/timeline.json",
                        "timeline-bundle/timeline.csv",
                        "timeline-bundle/timeline.md",
                        "timeline-bundle/timeline.html",
                        "timeline-bundle/README.txt",
                        GENERATED_AT,
                        List.of("timeline.json", "timeline.csv", "timeline.md", "timeline.html", "README.txt")
                ),
                health,
                report,
                overview
        );
    }

    static InstallationArtifactCatalogResponse installationArtifactCatalogResponse(String customerId) {
        return new InstallationArtifactCatalogResponse(
                customerId,
                GENERATED_AT,
                null,
                null,
                null,
                null,
                null,
                null,
                installationWorkspaceBundleResponse(customerId, "workspace-bundle")
        );
    }

    static QuoteSnapshotResponse quoteSnapshotResponse(String customerId) {
        return new QuoteSnapshotResponse(
                "quote-1",
                "install-1",
                customerId,
                "default",
                "quotes/quote-1.json",
                GENERATED_AT.minusSeconds(60),
                null
        );
    }

    static QuoteSnapshotSummaryResponse quoteSnapshotSummaryResponse(String customerId) {
        return new QuoteSnapshotSummaryResponse(
                "install-1",
                customerId,
                1,
                quoteSnapshotResponse(customerId),
                null,
                149.50,
                149.50,
                149.50,
                112.00,
                null
        );
    }

    static QuoteSnapshotDashboardResponse quoteSnapshotDashboardResponse(String customerId) {
        return new QuoteSnapshotDashboardResponse(
                "install-1",
                customerId,
                GENERATED_AT,
                quoteSnapshotSummaryResponse(customerId),
                List.of(new QuoteSnapshotTimelineEntryResponse(
                        quoteSnapshotResponse(customerId),
                        null,
                        false,
                        null,
                        null
                ))
        );
    }

    static CustomerDeliveryPackageDetailResponse customerDeliveryPackageDetailResponse(String customerId, String installationJobId) {
        return new CustomerDeliveryPackageDetailResponse(
                installationJobId,
                customerId,
                GENERATED_AT,
                customerInstallationResponse(installationJobId, customerId),
                installationHandoffResponse(installationJobId, customerId),
                installationWorkspaceResponse(customerId),
                installationArtifactCatalogResponse(customerId)
        );
    }

    static CustomerArtifactCatalogResponse customerArtifactCatalogResponse(String customerId) {
        return new CustomerArtifactCatalogResponse(
                customerId,
                GENERATED_AT,
                false,
                "Needs attention",
                "install-1",
                INSTALLATION_NAME,
                "COMPLETED",
                "quote-1",
                149.50,
                customerHealthExportResponse(customerId, "health-export"),
                customerHealthBundleResponse(customerId, "health-bundle"),
                customerOperationsWorkspaceExportResponse(customerId, "workspace-export"),
                customerOperationsWorkspaceBundleResponse(customerId, "workspace-bundle"),
                customerOverviewExportResponse(customerId, "overview-export"),
                customerOverviewBundleResponse(customerId, "overview-bundle"),
                customerDeliveryCenterExportResponse(customerId, "delivery-center-export"),
                customerDeliveryCenterBundleResponse(customerId, "delivery-center-bundle"),
                customerReportExportResponse(customerId, "report-export"),
                customerReportBundleResponse(customerId, "report-bundle"),
                customerAccountCenterExportResponse(customerId, "account-export"),
                customerAccountCenterBundleResponse(customerId, "account-bundle")
        );
    }

    static CustomerBootstrapBundleResponse customerBootstrapBundleResponse(String installationJobId, String bundleDirectory) {
        return new CustomerBootstrapBundleResponse(
                installationJobId,
                INSTALLATION_NAME,
                bundleDirectory,
                bundleDirectory + "/summary.json",
                bundleDirectory + "/customer-config.json",
                bundleDirectory + "/commercial-profile.json",
                bundleDirectory + "/app-stack.env",
                bundleDirectory + "/agent-inventory.json",
                bundleDirectory + "/softphone.env",
                bundleDirectory + "/ui-connection.json",
                bundleDirectory + "/asterisk-handoff.md",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of(
                        "summary.json",
                        "customer-config.json",
                        "commercial-profile.json",
                        "app-stack.env",
                        "agent-inventory.json",
                        "softphone.env",
                        "ui-connection.json",
                        "asterisk-handoff.md",
                        "README.txt"
                )
        );
    }

    static InstallationQuoteSummaryResponse installationQuoteSummaryResponse(String installationJobId, String customerId) {
        return new InstallationQuoteSummaryResponse(
                installationJobId,
                customerId,
                INSTALLATION_NAME,
                "Acme Corp",
                "SOFTPHONE",
                "COMPLETED",
                2,
                List.of("1001", "1002"),
                null,
                new CommercialAssumptionsResponse(
                        "preset",
                        1000L,
                        2000L,
                        100L,
                        5.0,
                        2,
                        10,
                        30.0
                ),
                new CostEstimateResponse(
                        customerId,
                        "default",
                        40.0,
                        72.0,
                        112.0,
                        149.5,
                        30.0
                ),
                GENERATED_AT
        );
    }

    static CustomerHealthExportResponse customerHealthExportResponse(String customerId, String exportDirectory) {
        return new CustomerHealthExportResponse(
                customerId,
                exportDirectory,
                exportDirectory + "/customer-health.json",
                exportDirectory + "/customer-health.html",
                exportDirectory + "/README.txt",
                GENERATED_AT
        );
    }

    static CustomerHealthBundleResponse customerHealthBundleResponse(String customerId, String bundleDirectory) {
        return new CustomerHealthBundleResponse(
                customerId,
                bundleDirectory,
                bundleDirectory + "/customer-health.json",
                bundleDirectory + "/customer-health.html",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("customer-health.json", "customer-health.html", "README.txt")
        );
    }

    static CustomerOperationsWorkspaceExportResponse customerOperationsWorkspaceExportResponse(String customerId, String exportDirectory) {
        return new CustomerOperationsWorkspaceExportResponse(
                customerId,
                exportDirectory,
                exportDirectory + "/customer-workspace.json",
                exportDirectory + "/customer-workspace.html",
                exportDirectory + "/README.txt",
                GENERATED_AT
        );
    }

    static CustomerOperationsWorkspaceBundleResponse customerOperationsWorkspaceBundleResponse(String customerId, String bundleDirectory) {
        return new CustomerOperationsWorkspaceBundleResponse(
                customerId,
                bundleDirectory,
                bundleDirectory + "/customer-workspace.json",
                bundleDirectory + "/customer-workspace.html",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("customer-workspace.json", "customer-workspace.html", "README.txt")
        );
    }

    static CustomerOverviewExportResponse customerOverviewExportResponse(String customerId, String exportDirectory) {
        return new CustomerOverviewExportResponse(
                customerId,
                exportDirectory,
                exportDirectory + "/customer-overview.json",
                exportDirectory + "/customer-overview.html",
                exportDirectory + "/README.txt",
                GENERATED_AT
        );
    }

    static CustomerOverviewBundleResponse customerOverviewBundleResponse(String customerId, String bundleDirectory) {
        return new CustomerOverviewBundleResponse(
                customerId,
                bundleDirectory,
                bundleDirectory + "/customer-overview.json",
                bundleDirectory + "/customer-overview.html",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("customer-overview.json", "customer-overview.html", "README.txt")
        );
    }

    static CustomerDeliveryCenterExportResponse customerDeliveryCenterExportResponse(String customerId, String exportDirectory) {
        return new CustomerDeliveryCenterExportResponse(
                customerId,
                exportDirectory,
                exportDirectory + "/customer-delivery.json",
                exportDirectory + "/customer-delivery.html",
                exportDirectory + "/README.txt",
                GENERATED_AT
        );
    }

    static CustomerDeliveryCenterBundleResponse customerDeliveryCenterBundleResponse(String customerId, String bundleDirectory) {
        return new CustomerDeliveryCenterBundleResponse(
                customerId,
                bundleDirectory,
                bundleDirectory + "/customer-delivery.json",
                bundleDirectory + "/customer-delivery.html",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("customer-delivery.json", "customer-delivery.html", "README.txt")
        );
    }

    static CustomerReportExportResponse customerReportExportResponse(String customerId, String exportDirectory) {
        return new CustomerReportExportResponse(
                customerId,
                exportDirectory,
                exportDirectory + "/customer-report.json",
                exportDirectory + "/customer-report.html",
                exportDirectory + "/README.txt",
                GENERATED_AT
        );
    }

    static CustomerReportBundleResponse customerReportBundleResponse(String customerId, String bundleDirectory) {
        return new CustomerReportBundleResponse(
                customerId,
                bundleDirectory,
                bundleDirectory + "/customer-report.json",
                bundleDirectory + "/customer-report.html",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("customer-report.json", "customer-report.html", "README.txt")
        );
    }

    static CustomerArtifactCatalogExportResponse customerArtifactCatalogExportResponse(String customerId, String exportDirectory) {
        return new CustomerArtifactCatalogExportResponse(
                customerId,
                exportDirectory,
                exportDirectory + "/customer-artifact-catalog.json",
                exportDirectory + "/customer-artifact-catalog.html",
                exportDirectory + "/README.txt",
                GENERATED_AT
        );
    }

    static CustomerArtifactCatalogBundleResponse customerArtifactCatalogBundleResponse(String customerId, String bundleDirectory) {
        return new CustomerArtifactCatalogBundleResponse(
                customerId,
                bundleDirectory,
                bundleDirectory + "/customer-artifact-catalog.json",
                bundleDirectory + "/customer-artifact-catalog.html",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("customer-artifact-catalog.json", "customer-artifact-catalog.html", "README.txt")
        );
    }

    static CustomerAccountCenterExportResponse customerAccountCenterExportResponse(String customerId, String exportDirectory) {
        return new CustomerAccountCenterExportResponse(
                customerId,
                exportDirectory,
                exportDirectory + "/customer-account.json",
                exportDirectory + "/customer-account.html",
                exportDirectory + "/README.txt",
                GENERATED_AT
        );
    }

    static CustomerAccountCenterBundleResponse customerAccountCenterBundleResponse(String customerId, String bundleDirectory) {
        return new CustomerAccountCenterBundleResponse(
                customerId,
                bundleDirectory,
                bundleDirectory + "/customer-account.json",
                bundleDirectory + "/customer-account.html",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("customer-account.json", "customer-account.html", "README.txt")
        );
    }

    static InstallationHandoffBundleResponse installationHandoffBundleResponse(String installationJobId, String bundleDirectory) {
        return new InstallationHandoffBundleResponse(
                installationJobId,
                INSTALLATION_NAME,
                bundleDirectory,
                bundleDirectory + "/installation.json",
                bundleDirectory + "/bootstrap-bundle.json",
                bundleDirectory + "/quote-summary.json",
                bundleDirectory + "/quote-dashboard.json",
                bundleDirectory + "/handoff.md",
                bundleDirectory + "/handoff.html",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("installation.json", "bootstrap-bundle.json", "quote-summary.json", "quote-dashboard.json", "handoff.md", "handoff.html", "README.txt")
        );
    }

    static InstallationHandoffExportResponse installationHandoffExportResponse(String installationJobId, String exportDirectory) {
        return new InstallationHandoffExportResponse(
                installationJobId,
                exportDirectory,
                exportDirectory + "/handoff.json",
                exportDirectory + "/handoff.html",
                exportDirectory + "/README.txt",
                GENERATED_AT
        );
    }

    static InstallationWorkspaceBundleResponse installationWorkspaceBundleResponse(String customerId, String bundleDirectory) {
        return new InstallationWorkspaceBundleResponse(
                customerId,
                bundleDirectory,
                bundleDirectory + "/workspace-manifest.json",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("workspace-manifest.json", "README.txt")
        );
    }

    static InstallationWorkspaceExportResponse installationWorkspaceExportResponse(String customerId, String exportDirectory) {
        return new InstallationWorkspaceExportResponse(
                customerId,
                exportDirectory,
                exportDirectory + "/installation-workspace.json",
                exportDirectory + "/installation-workspace.html",
                exportDirectory + "/README.txt",
                GENERATED_AT
        );
    }

    static InstallationArtifactCatalogBundleResponse installationArtifactCatalogBundleResponse(String customerId, String bundleDirectory) {
        return new InstallationArtifactCatalogBundleResponse(
                customerId,
                bundleDirectory,
                bundleDirectory + "/installation-artifact-catalog.json",
                bundleDirectory + "/installation-artifact-catalog.html",
                bundleDirectory + "/README.txt",
                GENERATED_AT,
                List.of("installation-artifact-catalog.json", "installation-artifact-catalog.html", "README.txt")
        );
    }
}
