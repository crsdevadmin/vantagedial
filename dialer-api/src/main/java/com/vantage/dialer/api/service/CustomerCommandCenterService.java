package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerAccountCenterResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterBundleResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterEntryResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterExportResponse;
import com.vantage.dialer.api.dto.CustomerCommandCenterResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioEntryResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerCommandCenterService {

    private final CustomerPortfolioService customerPortfolioService;
    private final CustomerAccountCenterService customerAccountCenterService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerCommandCenterService(CustomerPortfolioService customerPortfolioService,
                                        CustomerAccountCenterService customerAccountCenterService,
                                        ObjectMapper objectMapper,
                                        @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.customerPortfolioService = customerPortfolioService;
        this.customerAccountCenterService = customerAccountCenterService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("customer-command-center");
    }

    public CustomerCommandCenterResponse commandCenter() {
        CustomerPortfolioResponse portfolio = customerPortfolioService.portfolio();
        List<CustomerCommandCenterEntryResponse> customers = portfolio.customers().stream()
                .map(this::toEntry)
                .toList();
        int healthyCustomers = (int) customers.stream().filter(CustomerCommandCenterEntryResponse::healthy).count();
        int customersWithInstallations = (int) customers.stream().filter(CustomerCommandCenterEntryResponse::hasInstallations).count();
        int customersWithQuotes = (int) customers.stream().filter(CustomerCommandCenterEntryResponse::hasQuotes).count();
        int customersWithDeliveryPackage = (int) customers.stream().filter(CustomerCommandCenterEntryResponse::hasDeliveryPackage).count();
        int customersWithReport = (int) customers.stream().filter(CustomerCommandCenterEntryResponse::hasReport).count();
        int customersWithArtifactCatalog = (int) customers.stream().filter(CustomerCommandCenterEntryResponse::hasArtifactCatalog).count();
        boolean healthy = !customers.isEmpty() && healthyCustomers == customers.size();
        String statusMessage;
        if (customers.isEmpty()) {
            statusMessage = "No customers provisioned yet";
        } else if (healthyCustomers < customers.size()) {
            statusMessage = "Some customers still need attention";
        } else {
            statusMessage = "Customer command center is healthy";
        }

        return new CustomerCommandCenterResponse(
                Instant.now(),
                portfolio,
                customers.size(),
                healthyCustomers,
                customersWithInstallations,
                customersWithQuotes,
                customersWithDeliveryPackage,
                customersWithReport,
                customersWithArtifactCatalog,
                healthy,
                statusMessage,
                customers
        );
    }

    public CustomerCommandCenterBundleResponse generateBundle() {
        CustomerCommandCenterResponse commandCenter = commandCenter();
        Instant generatedAt = commandCenter.generatedAt();
        try {
            Path bundleDirectory = exportRoot.resolve("bundle");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "command-center.json", json(commandCenter)));
            files.add(write(bundleDirectory, "command-center.html", buildHtml(commandCenter)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(commandCenter)));

            return new CustomerCommandCenterBundleResponse(
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("command-center.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("command-center.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer command center bundle", e);
        }
    }

    public CustomerCommandCenterExportResponse export() {
        CustomerCommandCenterResponse commandCenter = commandCenter();
        Instant generatedAt = commandCenter.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve("export");
            Files.createDirectories(exportDirectory);

            Path commandCenterJsonPath = exportDirectory.resolve("command-center.json");
            Path commandCenterHtmlPath = exportDirectory.resolve("command-center.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(commandCenterJsonPath, json(commandCenter));
            Files.writeString(commandCenterHtmlPath, buildHtml(commandCenter));
            Files.writeString(readmePath, buildExportReadme(commandCenter));

            return new CustomerCommandCenterExportResponse(
                    exportDirectory.toAbsolutePath().toString(),
                    commandCenterJsonPath.toAbsolutePath().toString(),
                    commandCenterHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer command center", e);
        }
    }

    private CustomerCommandCenterEntryResponse toEntry(CustomerPortfolioEntryResponse portfolioEntry) {
        String customerId = "UNASSIGNED".equals(portfolioEntry.customerId()) ? null : portfolioEntry.customerId();
        CustomerAccountCenterResponse account = customerAccountCenterService.account(customerId);
        return new CustomerCommandCenterEntryResponse(
                portfolioEntry.customerId(),
                portfolioEntry,
                account,
                portfolioEntry.healthy(),
                account.hasInstallations(),
                account.hasQuotes(),
                account.hasDeliveryPackage(),
                portfolioEntry.reportAvailable(),
                portfolioEntry.artifactCatalogAvailable(),
                portfolioEntry.healthStatusMessage(),
                portfolioEntry.latestInstallationJobId(),
                portfolioEntry.latestInstallationName(),
                portfolioEntry.latestInstallationStatus(),
                portfolioEntry.latestQuoteSnapshotId(),
                portfolioEntry.latestSuggestedSellPrice()
        );
    }

    private String buildHtml(CustomerCommandCenterResponse commandCenter) {
        String rows = commandCenter.customers().stream()
                .map(entry -> """
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
                        escapeHtml(entry.customerId()),
                        escapeHtml(entry.healthy() ? "HEALTHY" : "ATTENTION"),
                        escapeHtml(String.valueOf(entry.portfolio().totalInstallations())),
                        escapeHtml(String.valueOf(entry.portfolio().quoteSnapshotCount())),
                        escapeHtml(entry.hasDeliveryPackage() ? "READY" : "NOT READY"),
                        escapeHtml(entry.portfolio().latestSuggestedSellPrice() == null ? "N/A" : String.valueOf(entry.portfolio().latestSuggestedSellPrice())),
                        escapeHtml(entry.hasReport() ? "READY" : "PENDING"),
                        escapeHtml(entry.portfolio().latestInstallationName() == null ? "None" : entry.portfolio().latestInstallationName())
                ))
                .reduce("", String::concat);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Customer Command Center</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f5efe6; color: #1f2933; }
                    .page { max-width: 1180px; margin: 0 auto; padding: 28px; }
                    .hero, .section { background: #fff; border: 1px solid #ddcfbe; border-radius: 24px; padding: 24px; box-shadow: 0 14px 30px rgba(0,0,0,0.08); }
                    .section { margin-top: 18px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
                    .card { background: #fff9f1; border: 1px solid #eadac8; border-radius: 16px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #75675b; }
                    .value { font-size: 26px; font-weight: 700; color: #8b4c1f; margin-top: 8px; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 16px; }
                    th, td { padding: 12px; border-bottom: 1px solid #ece2d5; text-align: left; }
                    th { color: #6f6154; font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Customer Command Center</h1>
                      <div>Generated at: %s</div>
                      <div>Status: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Customers</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Healthy</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">With Installs</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">With Quotes</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Delivery Ready</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Report Ready</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Customer Overview</h2>
                      <table>
                        <thead>
                          <tr>
                            <th>Customer</th>
                            <th>Health</th>
                            <th>Installations</th>
                            <th>Quotes</th>
                            <th>Delivery</th>
                            <th>Latest Quote</th>
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
                escapeHtml(commandCenter.generatedAt().toString()),
                escapeHtml(commandCenter.statusMessage()),
                escapeHtml(String.valueOf(commandCenter.totalCustomers())),
                escapeHtml(String.valueOf(commandCenter.healthyCustomers())),
                escapeHtml(String.valueOf(commandCenter.customersWithInstallations())),
                escapeHtml(String.valueOf(commandCenter.customersWithQuotes())),
                escapeHtml(String.valueOf(commandCenter.customersWithDeliveryPackage())),
                escapeHtml(String.valueOf(commandCenter.customersWithReport())),
                rows
        );
    }

    private String buildReadme(CustomerCommandCenterResponse commandCenter) {
        return """
                Customer command center bundle
                =============================

                Generated at: %s
                Total customers: %s
                Healthy: %s
                Status: %s

                Files:
                - command-center.json
                - command-center.html
                - README.txt
                """.formatted(
                commandCenter.generatedAt(),
                commandCenter.totalCustomers(),
                commandCenter.healthy(),
                commandCenter.statusMessage()
        );
    }

    private String buildExportReadme(CustomerCommandCenterResponse commandCenter) {
        return """
                Customer command center export
                =============================

                Generated at: %s
                Total customers: %s
                Status: %s

                Files:
                - command-center.json
                - command-center.html
                - README.txt
                """.formatted(
                commandCenter.generatedAt(),
                commandCenter.totalCustomers(),
                commandCenter.statusMessage()
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
            throw new IllegalStateException("Failed to serialize customer command center", e);
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
