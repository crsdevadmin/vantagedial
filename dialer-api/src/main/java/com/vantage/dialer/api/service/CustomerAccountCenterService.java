package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerAccountCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceResponse;
import com.vantage.dialer.api.dto.QuoteSnapshotDashboardResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerAccountCenterService {

    private final CustomerInstallationService installationService;
    private final CustomerHealthService customerHealthService;
    private final CustomerOperationsWorkspaceService customerOperationsWorkspaceService;
    private final InstallationDashboardService installationDashboardService;
    private final QuoteSnapshotService quoteSnapshotService;
    private final CustomerDeliveryPackageService deliveryPackageService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerAccountCenterService(CustomerInstallationService installationService,
                                        CustomerHealthService customerHealthService,
                                        CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                        InstallationDashboardService installationDashboardService,
                                        QuoteSnapshotService quoteSnapshotService,
                                        CustomerDeliveryPackageService deliveryPackageService,
                                        ObjectMapper objectMapper,
                                        @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.installationService = installationService;
        this.customerHealthService = customerHealthService;
        this.customerOperationsWorkspaceService = customerOperationsWorkspaceService;
        this.installationDashboardService = installationDashboardService;
        this.quoteSnapshotService = quoteSnapshotService;
        this.deliveryPackageService = deliveryPackageService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("customer-account-center");
    }

    public CustomerAccountCenterResponse account(String customerId) {
        List<CustomerInstallationResponse> installations = installationService.list(customerId);
        CustomerInstallationResponse latestInstallation = installations.isEmpty() ? null : installations.get(0);
        CustomerOperationsWorkspaceResponse operationsWorkspace = customerOperationsWorkspaceService.workspace(customerId);
        InstallationWorkspaceResponse installationWorkspace = installationDashboardService.workspace(customerId);
        QuoteSnapshotDashboardResponse quoteDashboard = quoteSnapshotService.dashboard(null, customerId);
        CustomerHealthResponse health = customerHealthService.health(customerId);
        CustomerDeliveryPackageDetailResponse latestDeliveryPackage = latestInstallation == null
                ? null
                : deliveryPackageService.detail(latestInstallation.installationJobId());
        boolean hasQuotes = quoteDashboard.summary() != null && quoteDashboard.summary().snapshotCount() > 0;
        boolean hasDeliveryPackage = latestDeliveryPackage != null;
        boolean hasReport = hasQuotes || hasDeliveryPackage;
        boolean hasArtifactCatalog = latestInstallation != null || hasQuotes;
        String statusMessage = operationsWorkspace.statusMessage();
        String latestInstallationJobId = latestInstallation == null ? null : latestInstallation.installationJobId();
        String latestInstallationName = latestInstallation == null ? null : latestInstallation.installationName();
        String latestInstallationStatus = latestInstallation == null ? null : latestInstallation.status();
        String latestQuoteSnapshotId = quoteDashboard.summary() == null || quoteDashboard.summary().latestSnapshot() == null
                ? null
                : quoteDashboard.summary().latestSnapshot().quoteSnapshotId();
        Double latestSuggestedSellPrice = quoteDashboard.summary() == null
                ? null
                : quoteDashboard.summary().latestSuggestedSellPrice();

        return new CustomerAccountCenterResponse(
                customerId,
                Instant.now(),
                operationsWorkspace,
                quoteDashboard,
                installationWorkspace,
                latestDeliveryPackage,
                health.healthy(),
                latestInstallation != null,
                hasQuotes,
                hasDeliveryPackage,
                hasReport,
                hasArtifactCatalog,
                statusMessage,
                latestInstallationJobId,
                latestInstallationStatus,
                latestQuoteSnapshotId,
                latestSuggestedSellPrice,
                latestInstallationName
        );
    }

    public CustomerAccountCenterBundleResponse generateBundle(String customerId) {
        CustomerAccountCenterResponse account = account(customerId);
        Instant generatedAt = account.generatedAt();
        try {
            Path bundleDirectory = scopeDirectory(customerId).resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "account-center.json", json(account)));
            files.add(write(bundleDirectory, "account-center.html", buildHtml(account)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(account)));

            return new CustomerAccountCenterBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("account-center.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("account-center.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer account center bundle", e);
        }
    }

    public CustomerAccountCenterExportResponse export(String customerId) {
        CustomerAccountCenterResponse account = account(customerId);
        Instant generatedAt = account.generatedAt();
        try {
            Path exportDirectory = scopeDirectory(customerId).resolve("export");
            Files.createDirectories(exportDirectory);

            Path accountJsonPath = exportDirectory.resolve("account-center.json");
            Path accountHtmlPath = exportDirectory.resolve("account-center.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(accountJsonPath, json(account));
            Files.writeString(accountHtmlPath, buildHtml(account));
            Files.writeString(readmePath, buildExportReadme(account));

            return new CustomerAccountCenterExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    accountJsonPath.toAbsolutePath().toString(),
                    accountHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer account center", e);
        }
    }

    private Path scopeDirectory(String customerId) {
        String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
        return exportRoot.resolve(scope);
    }

    private String buildHtml(CustomerAccountCenterResponse account) {
        String customer = account.customerId() == null ? "ALL" : account.customerId();
        String latestQuote = account.quoteDashboard().summary() == null || account.quoteDashboard().summary().latestSuggestedSellPrice() == null
                ? "N/A"
                : String.valueOf(account.quoteDashboard().summary().latestSuggestedSellPrice());
        String latestInstallation = account.operationsWorkspace().latestInstallation() == null
                ? "None"
                : account.operationsWorkspace().latestInstallation().installationName();
        String deliveryStatus = account.hasDeliveryPackage() ? "READY" : "NOT READY";
        String reportStatus = account.hasReport() ? "READY" : "PENDING";
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Customer Account Center</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f6f1e7; color: #1f2933; }
                    .page { max-width: 1140px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #ddcfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 28px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff9f1; border: 1px solid #ebdcc9; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #77695d; }
                    .value { font-size: 26px; font-weight: 700; color: #8b4c1f; margin-top: 8px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Customer Account Center</h1>
                      <div>Customer: %s</div>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Health</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Latest Installation</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Installations</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Latest Quote</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Report</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Coverage</h2>
                      <ul>
                        <li>Completed installations: %s</li>
                        <li>Failed installations: %s</li>
                        <li>Quote snapshots: %s</li>
                        <li>Artifact catalog ready: %s</li>
                        <li>Timeline entries: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(customer),
                escapeHtml(account.generatedAt().toString()),
                escapeHtml(account.statusMessage()),
                escapeHtml(account.healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(latestInstallation),
                escapeHtml(String.valueOf(account.installationWorkspace().dashboard().totalInstallations())),
                escapeHtml(latestQuote),
                escapeHtml(deliveryStatus),
                escapeHtml(reportStatus),
                escapeHtml(String.valueOf(account.installationWorkspace().dashboard().completedInstallations())),
                escapeHtml(String.valueOf(account.installationWorkspace().dashboard().failedInstallations())),
                escapeHtml(String.valueOf(account.quoteDashboard().summary() == null ? 0 : account.quoteDashboard().summary().snapshotCount())),
                escapeHtml(account.hasArtifactCatalog() ? "YES" : "NO"),
                escapeHtml(String.valueOf(account.quoteDashboard().timeline() == null ? 0 : account.quoteDashboard().timeline().size()))
        );
    }

    private String buildReadme(CustomerAccountCenterResponse account) {
        return """
                Customer account center bundle
                ============================

                Customer: %s
                Generated at: %s
                Status: %s
                Latest installation: %s
                Latest installation name: %s
                Latest installation status: %s
                Latest quote snapshot: %s
                Latest suggested sell price: %s

                Files:
                - account-center.json
                - account-center.html
                - README.txt

                This account center combines:
                - customer operations workspace
                - quote dashboard
                - installation workspace
                - latest delivery package details
                """.formatted(
                account.customerId() == null ? "ALL" : account.customerId(),
                account.generatedAt(),
                account.statusMessage(),
                account.latestInstallationJobId() == null ? "None" : account.latestInstallationJobId(),
                account.latestInstallationName() == null ? "None" : account.latestInstallationName(),
                account.latestInstallationStatus() == null ? "None" : account.latestInstallationStatus(),
                account.latestQuoteSnapshotId() == null ? "None" : account.latestQuoteSnapshotId(),
                account.latestSuggestedSellPrice() == null ? "N/A" : account.latestSuggestedSellPrice()
        );
    }

    private String buildExportReadme(CustomerAccountCenterResponse account) {
        return """
                Customer account center export
                ============================

                Customer: %s
                Generated at: %s
                Status: %s

                Files:
                - account-center.json
                - account-center.html
                - README.txt
                """.formatted(
                account.customerId() == null ? "ALL" : account.customerId(),
                account.generatedAt(),
                account.statusMessage()
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
            throw new IllegalStateException("Failed to serialize customer account center", e);
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
