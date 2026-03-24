package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerAccountCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerAccountCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogExportResponse;
import com.vantage.dialer.api.dto.CustomerArtifactCatalogResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerHealthBundleResponse;
import com.vantage.dialer.api.dto.CustomerHealthExportResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceExportResponse;
import com.vantage.dialer.api.dto.CustomerOverviewBundleResponse;
import com.vantage.dialer.api.dto.CustomerOverviewExportResponse;
import com.vantage.dialer.api.dto.CustomerReportBundleResponse;
import com.vantage.dialer.api.dto.CustomerReportExportResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerArtifactCatalogService {

    private final CustomerHealthService customerHealthService;
    private final CustomerOperationsWorkspaceService customerOperationsWorkspaceService;
    private final CustomerOverviewService customerOverviewService;
    private final CustomerDeliveryCenterService customerDeliveryCenterService;
    private final CustomerReportService customerReportService;
    private final CustomerAccountCenterService customerAccountCenterService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerArtifactCatalogService(CustomerHealthService customerHealthService,
                                          CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                          CustomerOverviewService customerOverviewService,
                                          CustomerDeliveryCenterService customerDeliveryCenterService,
                                          CustomerReportService customerReportService,
                                          CustomerAccountCenterService customerAccountCenterService,
                                          ObjectMapper objectMapper,
                                          @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.customerHealthService = customerHealthService;
        this.customerOperationsWorkspaceService = customerOperationsWorkspaceService;
        this.customerOverviewService = customerOverviewService;
        this.customerDeliveryCenterService = customerDeliveryCenterService;
        this.customerReportService = customerReportService;
        this.customerAccountCenterService = customerAccountCenterService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("customer-artifacts");
    }

    public CustomerArtifactCatalogResponse catalog(String customerId) {
        Instant generatedAt = Instant.now();
        var health = customerHealthService.health(customerId);
        CustomerHealthExportResponse healthExport = customerHealthService.export(customerId);
        CustomerHealthBundleResponse healthBundle = customerHealthService.generateBundle(customerId);
        CustomerOperationsWorkspaceExportResponse workspaceExport = customerOperationsWorkspaceService.export(customerId);
        CustomerOperationsWorkspaceBundleResponse workspaceBundle = customerOperationsWorkspaceService.generateBundle(customerId);
        CustomerOverviewExportResponse overviewExport = customerOverviewService.export(customerId);
        CustomerOverviewBundleResponse overviewBundle = customerOverviewService.generateBundle(customerId);
        CustomerDeliveryCenterExportResponse deliveryPackageExport = customerDeliveryCenterService.export(customerId);
        CustomerDeliveryCenterBundleResponse deliveryPackageBundle = customerDeliveryCenterService.generateBundle(customerId);
        CustomerReportExportResponse reportExport = customerReportService.export(customerId);
        CustomerReportBundleResponse reportBundle = customerReportService.generateBundle(customerId);
        CustomerAccountCenterExportResponse accountExport = customerAccountCenterService.export(customerId);
        CustomerAccountCenterBundleResponse accountBundle = customerAccountCenterService.generateBundle(customerId);

        return new CustomerArtifactCatalogResponse(
                customerId,
                generatedAt,
                health.healthy(),
                health.statusMessage(),
                health.latestInstallationJobId(),
                health.latestInstallationName(),
                health.latestInstallationStatus(),
                health.latestQuoteSnapshotId(),
                health.latestSuggestedSellPrice(),
                healthExport,
                healthBundle,
                workspaceExport,
                workspaceBundle,
                overviewExport,
                overviewBundle,
                deliveryPackageExport,
                deliveryPackageBundle,
                reportExport,
                reportBundle,
                accountExport,
                accountBundle
        );
    }

    public CustomerArtifactCatalogExportResponse export(String customerId) {
        CustomerArtifactCatalogResponse catalog = catalog(customerId);
        Instant generatedAt = catalog.generatedAt();
        try {
            Path exportDirectory = scopeDirectory(customerId).resolve("export");
            Files.createDirectories(exportDirectory);

            Path catalogJsonPath = exportDirectory.resolve("customer-artifact-catalog.json");
            Path catalogHtmlPath = exportDirectory.resolve("customer-artifact-catalog.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(catalogJsonPath, json(catalog));
            Files.writeString(catalogHtmlPath, buildHtml(catalog));
            Files.writeString(readmePath, buildReadme(catalog));

            return new CustomerArtifactCatalogExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    catalogJsonPath.toAbsolutePath().toString(),
                    catalogHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer artifact catalog", e);
        }
    }

    public CustomerArtifactCatalogBundleResponse generateBundle(String customerId) {
        CustomerArtifactCatalogResponse catalog = catalog(customerId);
        Instant generatedAt = catalog.generatedAt();
        try {
            Path bundleDirectory = scopeDirectory(customerId).resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "customer-artifact-catalog.json", json(catalog)));
            files.add(write(bundleDirectory, "customer-artifact-catalog.html", buildHtml(catalog)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(catalog)));

            return new CustomerArtifactCatalogBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-artifact-catalog.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-artifact-catalog.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer artifact catalog bundle", e);
        }
    }

    private Path scopeDirectory(String customerId) {
        String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
        return exportRoot.resolve(scope);
    }

    private String buildHtml(CustomerArtifactCatalogResponse catalog) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Customer Artifact Catalog</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f5efe7; color: #1f2933; }
                    .page { max-width: 1100px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #dccfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 28px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.8; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Customer Artifact Catalog</h1>
                      <div>Customer: %s</div>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <ul>
                        <li>Healthy: %s</li>
                        <li>Latest installation: %s</li>
                        <li>Latest installation name: %s</li>
                        <li>Latest installation status: %s</li>
                        <li>Latest quote snapshot: %s</li>
                        <li>Latest suggested sell price: %s</li>
                      </ul>
                    </section>
                    <section class="section">
                      <h2>Included Exports</h2>
                      <ul>
                        <li>Customer health export: %s</li>
                        <li>Customer workspace export: %s</li>
                        <li>Customer overview export: %s</li>
                        <li>Customer delivery package export: %s</li>
                        <li>Customer report export: %s</li>
                        <li>Customer account export: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(catalog.customerId() == null ? "ALL" : catalog.customerId()),
                escapeHtml(catalog.generatedAt().toString()),
                escapeHtml(catalog.statusMessage()),
                escapeHtml(catalog.healthy() ? "YES" : "NO"),
                escapeHtml(catalog.latestInstallationJobId() == null ? "None" : catalog.latestInstallationJobId()),
                escapeHtml(catalog.latestInstallationName() == null ? "None" : catalog.latestInstallationName()),
                escapeHtml(catalog.latestInstallationStatus() == null ? "None" : catalog.latestInstallationStatus()),
                escapeHtml(catalog.latestQuoteSnapshotId() == null ? "None" : catalog.latestQuoteSnapshotId()),
                escapeHtml(catalog.latestSuggestedSellPrice() == null ? "N/A" : String.valueOf(catalog.latestSuggestedSellPrice())),
                escapeHtml(catalog.healthExport().exportDirectory()),
                escapeHtml(catalog.workspaceExport().exportDirectory()),
                escapeHtml(catalog.overviewExport().exportDirectory()),
                escapeHtml(catalog.deliveryPackageExport().exportDirectory()),
                escapeHtml(catalog.reportExport().exportDirectory()),
                escapeHtml(catalog.accountExport().exportDirectory())
        );
    }

    private String buildReadme(CustomerArtifactCatalogResponse catalog) {
        return """
                Customer artifact catalog
                ========================

                Customer: %s
                Generated at: %s
                Healthy: %s
                Status: %s
                Latest installation: %s
                Latest installation name: %s
                Latest installation status: %s
                Latest quote snapshot: %s
                Latest suggested sell price: %s

                Files:
                - customer-artifact-catalog.json
                - customer-artifact-catalog.html
                - README.txt
                """.formatted(
                catalog.customerId() == null ? "ALL" : catalog.customerId(),
                catalog.generatedAt(),
                catalog.healthy(),
                catalog.statusMessage(),
                catalog.latestInstallationJobId() == null ? "None" : catalog.latestInstallationJobId(),
                catalog.latestInstallationName() == null ? "None" : catalog.latestInstallationName(),
                catalog.latestInstallationStatus() == null ? "None" : catalog.latestInstallationStatus(),
                catalog.latestQuoteSnapshotId() == null ? "None" : catalog.latestQuoteSnapshotId(),
                catalog.latestSuggestedSellPrice() == null ? "N/A" : catalog.latestSuggestedSellPrice()
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
            throw new IllegalStateException("Failed to serialize customer artifact catalog", e);
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
