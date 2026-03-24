package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerDeliveryCenterResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
import com.vantage.dialer.api.dto.CustomerOverviewResponse;
import com.vantage.dialer.api.dto.CustomerReportBundleResponse;
import com.vantage.dialer.api.dto.CustomerReportExportResponse;
import com.vantage.dialer.api.dto.CustomerReportResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerReportService {

    private final CustomerHealthService customerHealthService;
    private final CustomerOverviewService customerOverviewService;
    private final CustomerDeliveryCenterService customerDeliveryCenterService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerReportService(CustomerHealthService customerHealthService,
                                 CustomerOverviewService customerOverviewService,
                                 CustomerDeliveryCenterService customerDeliveryCenterService,
                                 ObjectMapper objectMapper,
                                 @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.customerHealthService = customerHealthService;
        this.customerOverviewService = customerOverviewService;
        this.customerDeliveryCenterService = customerDeliveryCenterService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("customer-report");
    }

    public CustomerReportResponse report(String customerId) {
        CustomerHealthResponse health = customerHealthService.health(customerId);
        CustomerOverviewResponse overview = customerOverviewService.overview(customerId);
        CustomerDeliveryCenterResponse deliveryPackage = customerDeliveryCenterService.deliveryPackage(customerId);
        return new CustomerReportResponse(
                customerId,
                Instant.now(),
                health,
                overview,
                deliveryPackage,
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

    public CustomerReportExportResponse export(String customerId) {
        CustomerReportResponse report = report(customerId);
        Instant generatedAt = report.generatedAt();
        try {
            Path exportDirectory = scopeDirectory(customerId).resolve("export");
            Files.createDirectories(exportDirectory);

            Path jsonPath = exportDirectory.resolve("customer-report.json");
            Path htmlPath = exportDirectory.resolve("customer-report.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(jsonPath, json(report));
            Files.writeString(htmlPath, buildHtml(report));
            Files.writeString(readmePath, buildReadme(report, true));

            return new CustomerReportExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    jsonPath.toAbsolutePath().toString(),
                    htmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer report", e);
        }
    }

    public CustomerReportBundleResponse generateBundle(String customerId) {
        CustomerReportResponse report = report(customerId);
        Instant generatedAt = report.generatedAt();
        try {
            Path bundleDirectory = scopeDirectory(customerId).resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "customer-report.json", json(report)));
            files.add(write(bundleDirectory, "customer-report.html", buildHtml(report)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(report, false)));

            return new CustomerReportBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-report.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-report.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer report bundle", e);
        }
    }

    private Path scopeDirectory(String customerId) {
        return exportRoot.resolve(customerId == null || customerId.isBlank() ? "all-customers" : customerId);
    }

    private String buildHtml(CustomerReportResponse report) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Customer Report</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f6efe8; color: #1f2933; }
                    .page { max-width: 1120px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #dccfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 28px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff8f0; border: 1px solid #ead9c8; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #786a5d; }
                    .value { font-size: 26px; font-weight: 700; color: #8b4c1f; margin-top: 8px; }
                    ul { margin: 0; padding-left: 20px; line-height: 1.7; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Customer Report</h1>
                      <div>Customer: %s</div>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Health</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Installations</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Quotes</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Report</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Summary</h2>
                      <ul>
                        <li>Customer health message: %s</li>
                        <li>Latest sell price: %s</li>
                        <li>Overview generated at: %s</li>
                        <li>Artifact catalog ready: %s</li>
                        <li>Delivery package generated at: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(report.customerId() == null ? "ALL" : report.customerId()),
                escapeHtml(report.generatedAt().toString()),
                escapeHtml(report.statusMessage()),
                escapeHtml(report.health().healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(String.valueOf(report.health().totalInstallations())),
                escapeHtml(String.valueOf(report.health().quoteSnapshotCount())),
                escapeHtml(report.deliveryPackage().accountCenter().hasDeliveryPackage() ? "READY" : "PENDING"),
                escapeHtml(report.hasReport() ? "READY" : "PENDING"),
                escapeHtml(report.health().statusMessage()),
                escapeHtml(String.valueOf(report.health().latestSuggestedSellPrice())),
                escapeHtml(report.overview().generatedAt().toString()),
                escapeHtml(report.hasArtifactCatalog() ? "YES" : "NO"),
                escapeHtml(report.deliveryPackage().generatedAt().toString())
        );
    }

    private String buildReadme(CustomerReportResponse report, boolean exported) {
        return """
                Customer report %s
                =================

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
                - customer delivery package
                - healthy: %s
                - report ready: %s
                - artifact catalog ready: %s

                Files:
                - customer-report.json
                - customer-report.html
                - README.txt
                """.formatted(
                exported ? "export" : "bundle",
                report.customerId() == null ? "ALL" : report.customerId(),
                report.generatedAt(),
                report.statusMessage(),
                report.latestInstallationJobId() == null ? "None" : report.latestInstallationJobId(),
                report.latestInstallationName() == null ? "None" : report.latestInstallationName(),
                report.latestInstallationStatus() == null ? "None" : report.latestInstallationStatus(),
                report.latestQuoteSnapshotId() == null ? "None" : report.latestQuoteSnapshotId(),
                report.latestSuggestedSellPrice() == null ? "N/A" : report.latestSuggestedSellPrice(),
                report.healthy(),
                report.hasReport(),
                report.hasArtifactCatalog()
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
            throw new IllegalStateException("Failed to serialize customer report", e);
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
