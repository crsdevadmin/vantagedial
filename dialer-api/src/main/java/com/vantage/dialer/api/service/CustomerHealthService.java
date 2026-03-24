package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerHealthBundleResponse;
import com.vantage.dialer.api.dto.CustomerHealthExportResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerHealthService {

    private final CustomerOperationsWorkspaceService customerOperationsWorkspaceService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerHealthService(CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                 ObjectMapper objectMapper,
                                 @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.customerOperationsWorkspaceService = customerOperationsWorkspaceService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("customer-health");
    }

    public CustomerHealthResponse health(String customerId) {
        CustomerOperationsWorkspaceResponse workspace = customerOperationsWorkspaceService.workspace(customerId);

        int totalInstallations = workspace.installationOverview().dashboard().totalInstallations();
        int completedInstallations = workspace.installationOverview().dashboard().completedInstallations();
        int failedInstallations = workspace.installationOverview().dashboard().failedInstallations();
        int quoteSnapshotCount = workspace.quoteSummary().snapshotCount();
        boolean deliveryPackageAvailable = workspace.deliveryPackageAvailable();
        boolean reportAvailable = workspace.hasReport();
        boolean artifactCatalogAvailable = workspace.hasArtifactCatalog();
        Double latestSuggestedSellPrice = workspace.quoteSummary().latestSuggestedSellPrice();
        String latestInstallationJobId = workspace.latestInstallation() == null
                ? null
                : workspace.latestInstallation().installationJobId();
        String latestInstallationName = workspace.latestInstallationName();
        String latestInstallationStatus = workspace.latestInstallationStatus();
        String latestQuoteSnapshotId = workspace.latestQuoteSnapshotId();

        boolean healthy = failedInstallations == 0
                && totalInstallations > 0
                && quoteSnapshotCount > 0;

        String statusMessage;
        if (totalInstallations == 0) {
            statusMessage = "Customer has no installations yet";
        } else if (failedInstallations > 0) {
            statusMessage = "Customer has failed installations";
        } else if (quoteSnapshotCount == 0) {
            statusMessage = "Customer has no saved quote snapshots";
        } else if (!deliveryPackageAvailable) {
            statusMessage = "Customer delivery package has not been generated yet";
        } else {
            statusMessage = "Customer is healthy";
        }

        return new CustomerHealthResponse(
                customerId,
                Instant.now(),
                healthy,
                statusMessage,
                totalInstallations,
                completedInstallations,
                failedInstallations,
                quoteSnapshotCount,
                deliveryPackageAvailable,
                reportAvailable,
                artifactCatalogAvailable,
                latestSuggestedSellPrice,
                latestInstallationJobId,
                latestInstallationName,
                latestInstallationStatus,
                latestQuoteSnapshotId
        );
    }

    public CustomerHealthExportResponse export(String customerId) {
        CustomerHealthResponse health = health(customerId);
        Instant generatedAt = health.generatedAt();
        try {
            Path exportDirectory = scopeDirectory(customerId).resolve("export");
            Files.createDirectories(exportDirectory);

            Path healthJsonPath = exportDirectory.resolve("customer-health.json");
            Path healthHtmlPath = exportDirectory.resolve("customer-health.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(healthJsonPath, json(health));
            Files.writeString(healthHtmlPath, buildHtml(health));
            Files.writeString(readmePath, buildReadme(health));

            return new CustomerHealthExportResponse(
                    customerId,
                    exportDirectory.toAbsolutePath().toString(),
                    healthJsonPath.toAbsolutePath().toString(),
                    healthHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer health", e);
        }
    }

    public CustomerHealthBundleResponse generateBundle(String customerId) {
        CustomerHealthResponse health = health(customerId);
        Instant generatedAt = health.generatedAt();
        try {
            Path bundleDirectory = scopeDirectory(customerId).resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "customer-health.json", json(health)));
            files.add(write(bundleDirectory, "customer-health.html", buildHtml(health)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(health)));

            return new CustomerHealthBundleResponse(
                    customerId,
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-health.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("customer-health.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer health bundle", e);
        }
    }

    private Path scopeDirectory(String customerId) {
        String scope = customerId == null || customerId.isBlank() ? "all-customers" : customerId;
        return exportRoot.resolve(scope);
    }

    private String buildHtml(CustomerHealthResponse health) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Customer Health</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f5efe7; color: #1f2933; }
                    .page { max-width: 1080px; margin: 0 auto; padding: 28px; }
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
                      <h1>Customer Health</h1>
                      <div>Customer: %s</div>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Installations</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Quotes</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failed</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Report</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Catalog</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Health Notes</h2>
                      <ul>
                        <li>%s</li>
                        <li>Latest installation job: %s</li>
                        <li>Latest installation name: %s</li>
                        <li>Latest installation status: %s</li>
                        <li>Latest quote snapshot: %s</li>
                        <li>Latest suggested sell price: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(health.customerId() == null ? "ALL" : health.customerId()),
                escapeHtml(health.generatedAt().toString()),
                escapeHtml(health.healthy() ? "HEALTHY" : "ATTENTION"),
                escapeHtml(String.valueOf(health.totalInstallations())),
                escapeHtml(String.valueOf(health.quoteSnapshotCount())),
                escapeHtml(String.valueOf(health.failedInstallations())),
                escapeHtml(health.deliveryPackageAvailable() ? "READY" : "NOT READY"),
                escapeHtml(health.reportAvailable() ? "READY" : "PENDING"),
                escapeHtml(health.artifactCatalogAvailable() ? "READY" : "PENDING"),
                escapeHtml(health.statusMessage()),
                escapeHtml(health.latestInstallationJobId() == null ? "None" : health.latestInstallationJobId()),
                escapeHtml(health.latestInstallationName() == null ? "None" : health.latestInstallationName()),
                escapeHtml(health.latestInstallationStatus() == null ? "None" : health.latestInstallationStatus()),
                escapeHtml(health.latestQuoteSnapshotId() == null ? "None" : health.latestQuoteSnapshotId()),
                escapeHtml(health.latestSuggestedSellPrice() == null ? "N/A" : String.valueOf(health.latestSuggestedSellPrice()))
        );
    }

    private String buildReadme(CustomerHealthResponse health) {
        return """
                Customer health
                ===============

                Customer: %s
                Generated at: %s
                Report ready: %s
                Artifact catalog ready: %s
                Latest installation name: %s
                Latest installation status: %s
                Latest quote snapshot: %s

                Files:
                - customer-health.json
                - customer-health.html
                - README.txt
                """.formatted(
                health.customerId() == null ? "ALL" : health.customerId(),
                health.generatedAt(),
                health.reportAvailable(),
                health.artifactCatalogAvailable(),
                health.latestInstallationName() == null ? "None" : health.latestInstallationName(),
                health.latestInstallationStatus() == null ? "None" : health.latestInstallationStatus(),
                health.latestQuoteSnapshotId() == null ? "None" : health.latestQuoteSnapshotId()
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
            throw new IllegalStateException("Failed to serialize customer health", e);
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
