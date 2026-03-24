package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceExportResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import com.vantage.dialer.api.dto.InstallationOverviewResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerOperationsWorkspaceService {

    private final CustomerInstallationService installationService;
    private final InstallationDashboardService installationDashboardService;
    private final QuoteSnapshotService quoteSnapshotService;
    private final CustomerDeliveryPackageService deliveryPackageService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerOperationsWorkspaceService(CustomerInstallationService installationService,
                                              InstallationDashboardService installationDashboardService,
                                              QuoteSnapshotService quoteSnapshotService,
                                              CustomerDeliveryPackageService deliveryPackageService,
                                              ObjectMapper objectMapper,
                                              @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.installationService = installationService;
        this.installationDashboardService = installationDashboardService;
        this.quoteSnapshotService = quoteSnapshotService;
        this.deliveryPackageService = deliveryPackageService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("customer-operations");
    }

    public CustomerOperationsWorkspaceResponse workspace(String customerId) {
        List<CustomerInstallationResponse> installations = installationService.list(customerId);
        CustomerInstallationResponse latestInstallation = installations.isEmpty() ? null : installations.get(0);
        InstallationOverviewResponse installationOverview = installationDashboardService.overview(customerId);
        QuoteSnapshotSummaryResponse quoteSummary = quoteSnapshotService.summary(null, customerId);
        CustomerDeliveryPackageDetailResponse latestDeliveryPackage = latestInstallation == null
                ? null
                : deliveryPackageService.detail(latestInstallation.installationJobId());
        boolean deliveryPackageAvailable = latestDeliveryPackage != null;
        boolean hasReport = quoteSummary.snapshotCount() > 0 || deliveryPackageAvailable;
        boolean hasArtifactCatalog = latestInstallation != null || quoteSummary.snapshotCount() > 0;
        boolean healthy = installationOverview.dashboard().failedInstallations() == 0
                && installationOverview.dashboard().totalInstallations() > 0
                && hasReport;
        String statusMessage;
        if (installationOverview.dashboard().totalInstallations() == 0) {
            statusMessage = "Customer has no installations yet";
        } else if (installationOverview.dashboard().failedInstallations() > 0) {
            statusMessage = "Customer has failed installations";
        } else if (!hasReport) {
            statusMessage = "Customer report artifacts are not ready yet";
        } else {
            statusMessage = "Customer workspace is healthy";
        }
        String latestInstallationJobId = latestInstallation == null ? null : latestInstallation.installationJobId();
        String latestInstallationName = latestInstallation == null ? null : latestInstallation.installationName();
        String latestInstallationStatus = latestInstallation == null ? null : latestInstallation.status();
        String latestQuoteSnapshotId = quoteSummary.latestSnapshot() == null ? null : quoteSummary.latestSnapshot().quoteSnapshotId();
        Double latestSuggestedSellPrice = quoteSummary.latestSuggestedSellPrice();

        return new CustomerOperationsWorkspaceResponse(
                customerId,
                Instant.now(),
                latestInstallation,
                installationOverview,
                quoteSummary,
                latestDeliveryPackage,
                deliveryPackageAvailable,
                healthy,
                hasReport,
                hasArtifactCatalog,
                statusMessage,
                latestInstallationJobId,
                latestInstallationName,
                latestInstallationStatus,
                latestQuoteSnapshotId,
                latestSuggestedSellPrice
        );
    }

    public CustomerOperationsWorkspaceBundleResponse generateBundle(String customerId) {
        CustomerOperationsWorkspaceResponse workspace = workspace(customerId);
        Instant generatedAt = workspace.generatedAt();
        try {
            Path bundleDirectory = scopeDirectory(customerId).resolve("workspace");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "workspace.json", json(workspace)));
            files.add(write(bundleDirectory, "workspace.html", buildHtml(workspace)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(workspace)));

            return new CustomerOperationsWorkspaceBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("workspace.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("workspace.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer operations workspace bundle", e);
        }
    }

    public CustomerOperationsWorkspaceExportResponse export(String customerId) {
        CustomerOperationsWorkspaceResponse workspace = workspace(customerId);
        Instant generatedAt = workspace.generatedAt();
        try {
            Path exportDirectory = scopeDirectory(customerId).resolve("workspace-export");
            Files.createDirectories(exportDirectory);

            Path workspaceJsonPath = exportDirectory.resolve("workspace.json");
            Path workspaceHtmlPath = exportDirectory.resolve("workspace.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(workspaceJsonPath, json(workspace));
            Files.writeString(workspaceHtmlPath, buildHtml(workspace));
            Files.writeString(readmePath, buildExportReadme(workspace));

            return new CustomerOperationsWorkspaceExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    workspaceJsonPath.toAbsolutePath().toString(),
                    workspaceHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer operations workspace", e);
        }
    }

    private Path scopeDirectory(String customerId) {
        String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
        return exportRoot.resolve(scope);
    }

    private String buildHtml(CustomerOperationsWorkspaceResponse workspace) {
        String customer = workspace.customerId() == null ? "ALL" : workspace.customerId();
        String latestInstallation = workspace.latestInstallation() == null
                ? "None"
                : workspace.latestInstallation().installationName();
        String latestQuote = workspace.quoteSummary().latestSuggestedSellPrice() == null
                ? "N/A"
                : String.valueOf(workspace.quoteSummary().latestSuggestedSellPrice());
        String deliveryStatus = workspace.deliveryPackageAvailable() ? "READY" : "NOT READY";
        String reportStatus = workspace.hasReport() ? "READY" : "PENDING";
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Customer Operations Workspace</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f5f1ea; color: #1f2933; }
                    .page { max-width: 1120px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #ddd0c1; border-radius: 24px; padding: 24px; box-shadow: 0 14px 30px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff9f2; border: 1px solid #ead9c7; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #77685b; }
                    .value { font-size: 26px; font-weight: 700; color: #8d4e23; margin-top: 8px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Customer Operations Workspace</h1>
                      <div>Customer: %s</div>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Health</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Installations</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Latest Installation</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Latest Quote</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery Package</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Report</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Workspace Scope</h2>
                      <ul>
                        <li>Completed installations: %s</li>
                        <li>Failed installations: %s</li>
                        <li>Quote snapshots: %s</li>
                        <li>Artifact catalog ready: %s</li>
                        <li>Total provisioned agents: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(customer),
                escapeHtml(workspace.generatedAt().toString()),
                escapeHtml(workspace.statusMessage()),
                escapeHtml(workspace.healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(String.valueOf(workspace.installationOverview().dashboard().totalInstallations())),
                escapeHtml(latestInstallation),
                escapeHtml(latestQuote),
                escapeHtml(deliveryStatus),
                escapeHtml(reportStatus),
                escapeHtml(String.valueOf(workspace.installationOverview().dashboard().completedInstallations())),
                escapeHtml(String.valueOf(workspace.installationOverview().dashboard().failedInstallations())),
                escapeHtml(String.valueOf(workspace.quoteSummary().snapshotCount())),
                escapeHtml(workspace.hasArtifactCatalog() ? "YES" : "NO"),
                escapeHtml(String.valueOf(workspace.installationOverview().dashboard().totalProvisionedAgents()))
        );
    }

    private String buildReadme(CustomerOperationsWorkspaceResponse workspace) {
        return """
                Customer operations workspace bundle
                ===================================

                Customer: %s
                Generated at: %s
                Status: %s
                Latest installation: %s
                Latest installation name: %s
                Latest installation status: %s
                Latest quote snapshot: %s
                Latest suggested sell price: %s

                Files:
                - workspace.json
                - workspace.html
                - README.txt

                This workspace combines:
                - installation overview
                - quote summary
                - latest delivery package details
                - healthy: %s
                - report ready: %s
                - artifact catalog ready: %s
                """.formatted(
                workspace.customerId() == null ? "ALL" : workspace.customerId(),
                workspace.generatedAt(),
                workspace.statusMessage(),
                workspace.latestInstallationJobId() == null ? "None" : workspace.latestInstallationJobId(),
                workspace.latestInstallationName() == null ? "None" : workspace.latestInstallationName(),
                workspace.latestInstallationStatus() == null ? "None" : workspace.latestInstallationStatus(),
                workspace.latestQuoteSnapshotId() == null ? "None" : workspace.latestQuoteSnapshotId(),
                workspace.latestSuggestedSellPrice() == null ? "N/A" : workspace.latestSuggestedSellPrice(),
                workspace.healthy(),
                workspace.hasReport(),
                workspace.hasArtifactCatalog()
        );
    }

    private String buildExportReadme(CustomerOperationsWorkspaceResponse workspace) {
        return """
                Customer operations workspace export
                ===================================

                Customer: %s
                Generated at: %s

                Files:
                - workspace.json
                - workspace.html
                - README.txt
                """.formatted(
                workspace.customerId() == null ? "ALL" : workspace.customerId(),
                workspace.generatedAt()
        );
    }

    private String write(Path directory, String fileName, String content) throws IOException {
        Path output = directory.resolve(fileName);
        Files.writeString(output, content);
        return output.getFileName().toString();
    }

    private String json(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize customer operations workspace", e);
        }
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
