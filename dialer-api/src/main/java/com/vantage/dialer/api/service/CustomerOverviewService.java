package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerOverviewBundleResponse;
import com.vantage.dialer.api.dto.CustomerOverviewExportResponse;
import com.vantage.dialer.api.dto.CustomerOverviewResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerOverviewService {

    private final CustomerHealthService customerHealthService;
    private final CustomerOperationsWorkspaceService customerOperationsWorkspaceService;
    private final CustomerAccountCenterService customerAccountCenterService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerOverviewService(CustomerHealthService customerHealthService,
                                   CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                   CustomerAccountCenterService customerAccountCenterService,
                                   ObjectMapper objectMapper,
                                   @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.customerHealthService = customerHealthService;
        this.customerOperationsWorkspaceService = customerOperationsWorkspaceService;
        this.customerAccountCenterService = customerAccountCenterService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("customer-overview");
    }

    public CustomerOverviewResponse overview(String customerId) {
        var health = customerHealthService.health(customerId);
        var workspace = customerOperationsWorkspaceService.workspace(customerId);
        var accountCenter = customerAccountCenterService.account(customerId);
        return new CustomerOverviewResponse(
                customerId,
                Instant.now(),
                health,
                workspace,
                accountCenter,
                health.healthy(),
                accountCenter.hasReport(),
                accountCenter.hasArtifactCatalog(),
                health.statusMessage(),
                health.latestInstallationJobId(),
                accountCenter.latestInstallationName(),
                accountCenter.latestInstallationStatus(),
                accountCenter.latestQuoteSnapshotId(),
                health.latestSuggestedSellPrice()
        );
    }

    public CustomerOverviewExportResponse export(String customerId) {
        CustomerOverviewResponse overview = overview(customerId);
        Instant generatedAt = overview.generatedAt();
        try {
            Path exportDirectory = scopeDirectory(customerId).resolve("export");
            Files.createDirectories(exportDirectory);

            Path overviewJsonPath = exportDirectory.resolve("customer-overview.json");
            Path overviewHtmlPath = exportDirectory.resolve("customer-overview.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(overviewJsonPath, json(overview));
            Files.writeString(overviewHtmlPath, buildHtml(overview));
            Files.writeString(readmePath, buildReadme(overview));

            return new CustomerOverviewExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    overviewJsonPath.toAbsolutePath().toString(),
                    overviewHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer overview", e);
        }
    }

    public CustomerOverviewBundleResponse generateBundle(String customerId) {
        CustomerOverviewResponse overview = overview(customerId);
        Instant generatedAt = overview.generatedAt();
        try {
            Path bundleDirectory = scopeDirectory(customerId).resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "customer-overview.json", json(overview)));
            files.add(write(bundleDirectory, "customer-overview.html", buildHtml(overview)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(overview)));

            return new CustomerOverviewBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-overview.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-overview.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer overview bundle", e);
        }
    }

    private Path scopeDirectory(String customerId) {
        String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
        return exportRoot.resolve(scope);
    }

    private String buildHtml(CustomerOverviewResponse overview) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Customer Overview</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f5efe7; color: #1f2933; }
                    .page { max-width: 1100px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #dccfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 28px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff8f0; border: 1px solid #ead9c8; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #786a5d; }
                    .value { font-size: 26px; font-weight: 700; color: #8b4c1f; margin-top: 8px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.8; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Customer Overview</h1>
                      <div>Customer: %s</div>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Health</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Installations</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Quotes</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Latest Quote</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Report</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Overview</h2>
                      <ul>
                        <li>%s</li>
                        <li>Latest installation: %s</li>
                        <li>Customer account has delivery package: %s</li>
                        <li>Customer artifact catalog ready: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(overview.customerId() == null ? "ALL" : overview.customerId()),
                escapeHtml(overview.generatedAt().toString()),
                escapeHtml(overview.health().healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(overview.healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(String.valueOf(overview.health().totalInstallations())),
                escapeHtml(String.valueOf(overview.health().quoteSnapshotCount())),
                escapeHtml(overview.health().deliveryPackageAvailable() ? "READY" : "NOT READY"),
                escapeHtml(overview.health().latestSuggestedSellPrice() == null ? "N/A" : String.valueOf(overview.health().latestSuggestedSellPrice())),
                escapeHtml(overview.hasReport() ? "READY" : "PENDING"),
                escapeHtml(overview.health().statusMessage()),
                escapeHtml(overview.health().latestInstallationJobId() == null ? "None" : overview.health().latestInstallationJobId()),
                escapeHtml(overview.accountCenter().hasDeliveryPackage() ? "YES" : "NO"),
                escapeHtml(overview.hasArtifactCatalog() ? "YES" : "NO")
        );
    }

    private String buildReadme(CustomerOverviewResponse overview) {
        return """
                Customer overview
                =================

                Customer: %s
                Generated at: %s
                Healthy: %s
                Report ready: %s
                Artifact catalog ready: %s
                Status: %s
                Latest installation: %s
                Latest installation name: %s
                Latest installation status: %s
                Latest quote snapshot: %s
                Latest suggested sell price: %s

                Files:
                - customer-overview.json
                - customer-overview.html
                - README.txt
                """.formatted(
                overview.customerId() == null ? "ALL" : overview.customerId(),
                overview.generatedAt(),
                overview.healthy(),
                overview.hasReport(),
                overview.hasArtifactCatalog(),
                overview.statusMessage(),
                overview.latestInstallationJobId() == null ? "None" : overview.latestInstallationJobId(),
                overview.latestInstallationName() == null ? "None" : overview.latestInstallationName(),
                overview.latestInstallationStatus() == null ? "None" : overview.latestInstallationStatus(),
                overview.latestQuoteSnapshotId() == null ? "None" : overview.latestQuoteSnapshotId(),
                overview.latestSuggestedSellPrice() == null ? "N/A" : overview.latestSuggestedSellPrice()
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
            throw new IllegalStateException("Failed to serialize customer overview", e);
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
