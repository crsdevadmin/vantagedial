package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerAccountCenterResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
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
public class CustomerDeliveryCenterService {

    private final CustomerHealthService customerHealthService;
    private final CustomerOverviewService customerOverviewService;
    private final CustomerAccountCenterService customerAccountCenterService;
    private final CustomerArtifactCatalogService customerArtifactCatalogService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerDeliveryCenterService(CustomerHealthService customerHealthService,
                                         CustomerOverviewService customerOverviewService,
                                         CustomerAccountCenterService customerAccountCenterService,
                                         CustomerArtifactCatalogService customerArtifactCatalogService,
                                         ObjectMapper objectMapper,
                                         @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.customerHealthService = customerHealthService;
        this.customerOverviewService = customerOverviewService;
        this.customerAccountCenterService = customerAccountCenterService;
        this.customerArtifactCatalogService = customerArtifactCatalogService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("customer-delivery-center");
    }

    public CustomerDeliveryCenterResponse deliveryPackage(String customerId) {
        CustomerHealthResponse health = customerHealthService.health(customerId);
        CustomerOverviewResponse overview = customerOverviewService.overview(customerId);
        CustomerAccountCenterResponse accountCenter = customerAccountCenterService.account(customerId);
        CustomerArtifactCatalogResponse artifactCatalog = customerArtifactCatalogService.catalog(customerId);
        return new CustomerDeliveryCenterResponse(
                customerId,
                Instant.now(),
                health,
                overview,
                accountCenter,
                artifactCatalog,
                health.healthy(),
                overview.hasReport(),
                overview.hasArtifactCatalog(),
                health.statusMessage(),
                health.latestInstallationJobId(),
                overview.latestInstallationName(),
                overview.latestInstallationStatus(),
                overview.latestQuoteSnapshotId(),
                health.latestSuggestedSellPrice()
        );
    }

    public CustomerDeliveryCenterExportResponse export(String customerId) {
        CustomerDeliveryCenterResponse detail = deliveryPackage(customerId);
        Instant generatedAt = detail.generatedAt();
        try {
            Path exportDirectory = scopeDirectory(customerId).resolve("export");
            Files.createDirectories(exportDirectory);

            Path jsonPath = exportDirectory.resolve("customer-delivery-package.json");
            Path htmlPath = exportDirectory.resolve("customer-delivery-package.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(jsonPath, json(detail));
            Files.writeString(htmlPath, buildHtml(detail));
            Files.writeString(readmePath, buildReadme(detail, true));

            return new CustomerDeliveryCenterExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    jsonPath.toAbsolutePath().toString(),
                    htmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer delivery package", e);
        }
    }

    public CustomerDeliveryCenterBundleResponse generateBundle(String customerId) {
        CustomerDeliveryCenterResponse detail = deliveryPackage(customerId);
        Instant generatedAt = detail.generatedAt();
        try {
            Path bundleDirectory = scopeDirectory(customerId).resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "customer-delivery-package.json", json(detail)));
            files.add(write(bundleDirectory, "customer-delivery-package.html", buildHtml(detail)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(detail, false)));

            return new CustomerDeliveryCenterBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-delivery-package.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-delivery-package.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer delivery package bundle", e);
        }
    }

    private Path scopeDirectory(String customerId) {
        return exportRoot.resolve(customerId == null || customerId.isBlank() ? "all-customers" : customerId);
    }

    private String buildHtml(CustomerDeliveryCenterResponse detail) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Customer Delivery Package</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f4efe7; color: #1f2933; }
                    .page { max-width: 1100px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: white; border: 1px solid #d9cdbf; border-radius: 24px; padding: 24px; box-shadow: 0 16px 36px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff9f0; border: 1px solid #e2d4c3; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #7b6d60; }
                    .value { font-size: 26px; margin-top: 8px; color: #8b4c1f; font-weight: 700; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Customer Delivery Package</h1>
                      <div>Customer: %s</div>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Health</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Installations</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Quotes</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery Ready</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Report</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Catalog</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Included Components</h2>
                      <ul>
                        <li>Customer health status: %s</li>
                        <li>Overview workspace generated at: %s</li>
                        <li>Account center includes delivery package: %s</li>
                        <li>Artifact catalog export root: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(detail.customerId() == null ? "ALL" : detail.customerId()),
                escapeHtml(detail.generatedAt().toString()),
                escapeHtml(detail.statusMessage()),
                escapeHtml(detail.health().healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(String.valueOf(detail.health().totalInstallations())),
                escapeHtml(String.valueOf(detail.health().quoteSnapshotCount())),
                escapeHtml(detail.accountCenter().hasDeliveryPackage() ? "YES" : "PENDING"),
                escapeHtml(detail.hasReport() ? "READY" : "PENDING"),
                escapeHtml(detail.hasArtifactCatalog() ? "READY" : "PENDING"),
                escapeHtml(detail.health().statusMessage()),
                escapeHtml(detail.overview().workspace().generatedAt().toString()),
                escapeHtml(detail.accountCenter().hasDeliveryPackage() ? "YES" : "NO"),
                escapeHtml(detail.artifactCatalog().workspaceExport().exportDirectory())
        );
    }

    private String buildReadme(CustomerDeliveryCenterResponse detail, boolean exported) {
        return """
                Customer delivery package %s
                ============================

                Customer: %s
                Generated at: %s
                Status: %s
                Latest installation: %s
                Latest installation name: %s
                Latest installation status: %s
                Latest quote snapshot: %s
                Latest suggested sell price: %s

                Included:
                - customer health
                - customer overview
                - customer account center
                - customer artifact catalog
                - healthy: %s
                - report ready: %s
                - artifact catalog ready: %s

                Files:
                - customer-delivery-package.json
                - customer-delivery-package.html
                - README.txt
                """.formatted(
                exported ? "export" : "bundle",
                detail.customerId() == null ? "ALL" : detail.customerId(),
                detail.generatedAt(),
                detail.statusMessage(),
                detail.latestInstallationJobId() == null ? "None" : detail.latestInstallationJobId(),
                detail.latestInstallationName() == null ? "None" : detail.latestInstallationName(),
                detail.latestInstallationStatus() == null ? "None" : detail.latestInstallationStatus(),
                detail.latestQuoteSnapshotId() == null ? "None" : detail.latestQuoteSnapshotId(),
                detail.latestSuggestedSellPrice() == null ? "N/A" : detail.latestSuggestedSellPrice(),
                detail.healthy(),
                detail.hasReport(),
                detail.hasArtifactCatalog()
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
            throw new IllegalStateException("Failed to serialize customer delivery package", e);
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
