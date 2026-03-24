package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.CustomerHealthResponse;
import com.vantage.dialer.api.dto.CustomerOperationsWorkspaceResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioBundleResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioEntryResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioExportResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CustomerPortfolioService {

    private final CustomerInstallationService installationService;
    private final CustomerHealthService customerHealthService;
    private final CustomerOperationsWorkspaceService customerOperationsWorkspaceService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerPortfolioService(CustomerInstallationService installationService,
                                    CustomerHealthService customerHealthService,
                                    CustomerOperationsWorkspaceService customerOperationsWorkspaceService,
                                    ObjectMapper objectMapper,
                                    @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.installationService = installationService;
        this.customerHealthService = customerHealthService;
        this.customerOperationsWorkspaceService = customerOperationsWorkspaceService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("customer-portfolio");
    }

    public CustomerPortfolioResponse portfolio() {
        List<CustomerInstallationResponse> allInstallations = installationService.list(null);
        Set<String> customerIds = new LinkedHashSet<>();
        for (CustomerInstallationResponse installation : allInstallations) {
            customerIds.add(normalizeCustomerId(installation.customerId()));
        }

        List<CustomerPortfolioEntryResponse> customers = customerIds.stream()
                .map(this::buildEntry)
                .sorted(Comparator.comparing(CustomerPortfolioEntryResponse::customerId))
                .toList();
        int healthyCustomers = (int) customers.stream().filter(CustomerPortfolioEntryResponse::healthy).count();
        int customersWithDeliveryPackage = (int) customers.stream().filter(CustomerPortfolioEntryResponse::deliveryPackageAvailable).count();
        int customersWithReport = (int) customers.stream().filter(CustomerPortfolioEntryResponse::reportAvailable).count();
        int customersWithArtifactCatalog = (int) customers.stream().filter(CustomerPortfolioEntryResponse::artifactCatalogAvailable).count();
        boolean healthy = !customers.isEmpty() && healthyCustomers == customers.size();
        String statusMessage;
        if (customers.isEmpty()) {
            statusMessage = "No customers provisioned yet";
        } else if (healthyCustomers < customers.size()) {
            statusMessage = "Some customers still need attention";
        } else {
            statusMessage = "Customer portfolio is healthy";
        }

        return new CustomerPortfolioResponse(
                Instant.now(),
                customers.size(),
                healthyCustomers,
                customersWithDeliveryPackage,
                customersWithReport,
                customersWithArtifactCatalog,
                healthy,
                statusMessage,
                customers
        );
    }

    public CustomerPortfolioBundleResponse generateBundle() {
        CustomerPortfolioResponse portfolio = portfolio();
        Instant generatedAt = portfolio.generatedAt();
        try {
            Path bundleDirectory = exportRoot.resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "portfolio.json", json(portfolio)));
            files.add(write(bundleDirectory, "portfolio.html", buildHtml(portfolio)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(portfolio)));

            return new CustomerPortfolioBundleResponse(
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("portfolio.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("portfolio.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer portfolio bundle", e);
        }
    }

    public CustomerPortfolioExportResponse export() {
        CustomerPortfolioResponse portfolio = portfolio();
        Instant generatedAt = portfolio.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve("export");
            Files.createDirectories(exportDirectory);

            Path portfolioJsonPath = exportDirectory.resolve("portfolio.json");
            Path portfolioHtmlPath = exportDirectory.resolve("portfolio.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(portfolioJsonPath, json(portfolio));
            Files.writeString(portfolioHtmlPath, buildHtml(portfolio));
            Files.writeString(readmePath, buildExportReadme(portfolio));

            return new CustomerPortfolioExportResponse(
                    exportDirectory.toAbsolutePath().toString(),
                    portfolioJsonPath.toAbsolutePath().toString(),
                    portfolioHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer portfolio", e);
        }
    }

    private CustomerPortfolioEntryResponse buildEntry(String customerId) {
        String scopedCustomerId = "UNASSIGNED".equals(customerId) ? null : customerId;
        CustomerOperationsWorkspaceResponse workspace = customerOperationsWorkspaceService.workspace(scopedCustomerId);
        CustomerHealthResponse health = customerHealthService.health(scopedCustomerId);
        boolean reportAvailable = workspace.quoteSummary().snapshotCount() > 0 || workspace.deliveryPackageAvailable();
        boolean artifactCatalogAvailable = workspace.latestInstallation() != null || workspace.quoteSummary().snapshotCount() > 0;
        return new CustomerPortfolioEntryResponse(
                customerId,
                workspace.latestInstallation() == null ? null : workspace.latestInstallation().installationJobId(),
                workspace.latestInstallation() == null ? null : workspace.latestInstallation().installationName(),
                workspace.latestInstallationStatus(),
                workspace.installationOverview().dashboard().totalInstallations(),
                workspace.installationOverview().dashboard().completedInstallations(),
                workspace.installationOverview().dashboard().failedInstallations(),
                workspace.quoteSummary().snapshotCount(),
                workspace.latestQuoteSnapshotId(),
                workspace.quoteSummary().latestSuggestedSellPrice(),
                workspace.deliveryPackageAvailable(),
                health.healthy(),
                health.statusMessage(),
                reportAvailable,
                artifactCatalogAvailable
        );
    }

    private String normalizeCustomerId(String customerId) {
        return customerId == null || customerId.isBlank() ? "UNASSIGNED" : customerId;
    }

    private String buildHtml(CustomerPortfolioResponse portfolio) {
        String rows = portfolio.customers().stream()
                .map(customer -> """
                        <tr>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                        </tr>
                        """.formatted(
                        escapeHtml(customer.customerId()),
                        escapeHtml(customer.healthy() ? "HEALTHY" : "ATTENTION"),
                        escapeHtml(String.valueOf(customer.totalInstallations())),
                        escapeHtml(String.valueOf(customer.quoteSnapshotCount())),
                        escapeHtml(customer.latestSuggestedSellPrice() == null ? "N/A" : String.valueOf(customer.latestSuggestedSellPrice())),
                        escapeHtml(customer.deliveryPackageAvailable() ? "READY" : "NOT READY"),
                        escapeHtml(customer.reportAvailable() ? "READY" : "PENDING"),
                        escapeHtml(customer.latestInstallationName() == null ? "None" : customer.latestInstallationName())
                ))
                .reduce("", String::concat);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Customer Portfolio</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f6f1e8; color: #1f2933; }
                    .page { max-width: 1180px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #dccfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 28px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff8ef; border: 1px solid #eadcca; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #786b5e; }
                    .value { font-size: 26px; font-weight: 700; color: #8b4c1f; margin-top: 8px; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 16px; }
                    th, td { padding: 12px; border-bottom: 1px solid #ece3d7; text-align: left; }
                    th { color: #6f6154; font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Customer Portfolio</h1>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Healthy</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery Ready</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Report Ready</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Customer Readiness</h2>
                      <table>
                        <thead>
                          <tr>
                            <th>Customer</th>
                            <th>Health</th>
                            <th>Installations</th>
                            <th>Quotes</th>
                            <th>Latest Quote</th>
                            <th>Delivery</th>
                            <th>Report</th>
                            <th>Latest Installation</th>
                          </tr>
                        </thead>
                        <tbody>%s</tbody>
                      </table>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(portfolio.generatedAt().toString()),
                escapeHtml(portfolio.statusMessage()),
                escapeHtml(String.valueOf(portfolio.totalCustomers())),
                escapeHtml(String.valueOf(portfolio.healthyCustomers())),
                escapeHtml(String.valueOf(portfolio.customersWithDeliveryPackage())),
                escapeHtml(String.valueOf(portfolio.customersWithReport())),
                rows
        );
    }

    private String buildReadme(CustomerPortfolioResponse portfolio) {
        return """
                Customer portfolio bundle
                ========================

                Generated at: %s
                Total customers: %s
                Healthy: %s
                Status: %s

                Files:
                - portfolio.json
                - portfolio.html
                - README.txt
                """.formatted(
                portfolio.generatedAt(),
                portfolio.totalCustomers(),
                portfolio.healthy(),
                portfolio.statusMessage()
        );
    }

    private String buildExportReadme(CustomerPortfolioResponse portfolio) {
        return """
                Customer portfolio export
                ========================

                Generated at: %s
                Total customers: %s
                Status: %s

                Files:
                - portfolio.json
                - portfolio.html
                - README.txt
                """.formatted(
                portfolio.generatedAt(),
                portfolio.totalCustomers(),
                portfolio.statusMessage()
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
            throw new IllegalStateException("Failed to serialize customer portfolio", e);
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
