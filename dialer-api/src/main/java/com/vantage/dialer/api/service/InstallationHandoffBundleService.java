package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CommercialAssumptionsResponse;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CustomerBootstrapBundleResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.InstallationHandoffExportResponse;
import com.vantage.dialer.api.dto.InstallationHandoffBundleResponse;
import com.vantage.dialer.api.dto.InstallationHandoffResponse;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
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
public class InstallationHandoffBundleService {

    private final CustomerInstallationService installationService;
    private final CustomerBootstrapBundleService bootstrapBundleService;
    private final CustomerQuoteService customerQuoteService;
    private final QuoteSnapshotService quoteSnapshotService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public InstallationHandoffBundleService(CustomerInstallationService installationService,
                                            CustomerBootstrapBundleService bootstrapBundleService,
                                            CustomerQuoteService customerQuoteService,
                                            QuoteSnapshotService quoteSnapshotService,
                                            ObjectMapper objectMapper,
                                            @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.installationService = installationService;
        this.bootstrapBundleService = bootstrapBundleService;
        this.customerQuoteService = customerQuoteService;
        this.quoteSnapshotService = quoteSnapshotService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("installations");
    }

    public InstallationHandoffBundleResponse generate(String installationJobId) {
        InstallationHandoffResponse handoff = handoff(installationJobId);
        CustomerInstallationResponse installation = handoff.installation();
        CustomerBootstrapBundleResponse bootstrapBundle = handoff.bootstrapBundle();
        InstallationQuoteSummaryResponse quoteSummary = handoff.quoteSummary();
        QuoteSnapshotDashboardResponse quoteDashboard = handoff.quoteDashboard();
        Instant generatedAt = handoff.generatedAt();

        try {
            Path bundleDirectory = exportRoot.resolve(installation.installationJobId()).resolve("handoff");
            Files.createDirectories(bundleDirectory);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDirectory, "installation.json", json(installation)));
            files.add(write(bundleDirectory, "bootstrap-bundle.json", json(bootstrapBundle)));
            files.add(write(bundleDirectory, "quote-summary.json", json(quoteSummary)));
            files.add(write(bundleDirectory, "quote-dashboard.json", json(quoteDashboard)));
            files.add(write(bundleDirectory, "handoff.md", buildHandoffMarkdown(installation, bootstrapBundle, quoteSummary, quoteDashboard, generatedAt)));
            files.add(write(bundleDirectory, "handoff.html", buildHandoffHtml(installation, bootstrapBundle, quoteSummary, quoteDashboard, generatedAt)));
            files.add(write(bundleDirectory, "README.txt", buildReadme(installation, quoteDashboard, generatedAt)));

            return new InstallationHandoffBundleResponse(
                    installation.installationJobId(),
                    installation.installationName(),
                    bundleDirectory.toAbsolutePath().toString(),
                    bundleDirectory.resolve("installation.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("bootstrap-bundle.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("quote-summary.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("quote-dashboard.json").toAbsolutePath().toString(),
                    bundleDirectory.resolve("handoff.md").toAbsolutePath().toString(),
                    bundleDirectory.resolve("handoff.html").toAbsolutePath().toString(),
                    bundleDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate installation handoff bundle", e);
        }
    }

    public InstallationHandoffResponse handoff(String installationJobId) {
        CustomerInstallationResponse installation = installationService.get(installationJobId);
        return new InstallationHandoffResponse(
                installation.installationJobId(),
                installation.customerId(),
                installation.installationName(),
                Instant.now(),
                installation,
                bootstrapBundleService.generate(installationJobId),
                customerQuoteService.quote(installationJobId, defaultQuoteRequest(installation)),
                quoteSnapshotService.dashboard(installation.installationJobId(), installation.customerId())
        );
    }

    public InstallationHandoffExportResponse export(String installationJobId) {
        InstallationHandoffResponse handoff = handoff(installationJobId);
        Instant generatedAt = handoff.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve(handoff.installationJobId()).resolve("handoff-export");
            Files.createDirectories(exportDirectory);

            Path handoffJsonPath = exportDirectory.resolve("handoff.json");
            Path handoffHtmlPath = exportDirectory.resolve("handoff.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(handoffJsonPath, json(handoff));
            Files.writeString(handoffHtmlPath, buildHandoffHtml(
                    handoff.installation(),
                    handoff.bootstrapBundle(),
                    handoff.quoteSummary(),
                    handoff.quoteDashboard(),
                    generatedAt));
            Files.writeString(readmePath, buildExportReadme(handoff));

            return new InstallationHandoffExportResponse(
                    handoff.installationJobId(),
                    exportDirectory.toAbsolutePath().toString(),
                    handoffJsonPath.toAbsolutePath().toString(),
                    handoffHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export installation handoff", e);
        }
    }

    private CostEstimateRequest defaultQuoteRequest(CustomerInstallationResponse installation) {
        CostEstimateRequest request = new CostEstimateRequest();
        request.setCustomerId(installation.customerId());
        request.setUseCustomerPresetDefaults(true);
        request.setAgentCount(installation.agentCount());
        return request;
    }

    private String buildHandoffMarkdown(CustomerInstallationResponse installation,
                                        CustomerBootstrapBundleResponse bootstrapBundle,
                                        InstallationQuoteSummaryResponse quoteSummary,
                                        QuoteSnapshotDashboardResponse quoteDashboard,
                                        Instant generatedAt) {
        CommercialAssumptionsResponse assumptions = quoteSummary.commercialAssumptions();
        return """
                # Installation Handoff

                - Generated at: %s
                - Installation: %s
                - Customer: %s
                - Client type: %s
                - Status: %s
                - Dry run: %s
                - Agents provisioned: %s
                - Deployment job id: %s

                ## Delivery Summary

                - Bootstrap bundle directory: `%s`
                - Quote snapshot count: %s
                - Latest suggested sell price: %s
                - Latest estimated cost: %s

                ## Commercial Assumptions

                - Source: %s
                - Monthly call minutes: %s
                - Monthly TTS units: %s
                - Monthly STT minutes: %s
                - Monthly recording GB: %s
                - Agent count: %s
                - Concurrent channels: %s
                - Desired margin percent: %s

                ## Recommended Delivery Steps

                1. Review `installation.json` for the recorded install outcome and deploy ids.
                2. Open `bootstrap-bundle.json` and then the referenced bootstrap files for env/config handoff.
                3. Review `quote-summary.json` before sharing pricing with the customer.
                4. Review `quote-dashboard.json` for quote history, trends, and previous comparisons.
                5. Use the generated bootstrap bundle to configure server B and the agent softphone/web UI.
                6. If deployment has not been applied on server A, use the stored deploy endpoints or runbook from the bootstrap files.
                """.formatted(
                generatedAt,
                installation.installationName(),
                safe(quoteSummary.customerName(), safe(installation.customerId(), "<none>")),
                installation.clientType(),
                installation.status(),
                installation.dryRun(),
                installation.agentCount(),
                safe(installation.deploymentJobId(), "<not-generated>"),
                bootstrapBundle.bundleDirectory(),
                quoteDashboard.summary() == null ? 0 : quoteDashboard.summary().snapshotCount(),
                quoteSummary.estimate().suggestedSellPrice(),
                quoteSummary.estimate().totalEstimatedCost(),
                assumptions == null ? "<unknown>" : safe(assumptions.source(), "<unknown>"),
                assumptions == null ? 0 : assumptions.monthlyCallMinutes(),
                assumptions == null ? 0 : assumptions.monthlyTtsUnits(),
                assumptions == null ? 0 : assumptions.monthlySttMinutes(),
                assumptions == null ? 0D : assumptions.monthlyRecordingGb(),
                assumptions == null ? 0 : assumptions.agentCount(),
                assumptions == null ? 0 : assumptions.concurrentChannels(),
                assumptions == null ? 0D : assumptions.desiredMarginPercent()
        );
    }

    private String buildHandoffHtml(CustomerInstallationResponse installation,
                                    CustomerBootstrapBundleResponse bootstrapBundle,
                                    InstallationQuoteSummaryResponse quoteSummary,
                                    QuoteSnapshotDashboardResponse quoteDashboard,
                                    Instant generatedAt) {
        CommercialAssumptionsResponse assumptions = quoteSummary.commercialAssumptions();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Installation Handoff</title>
                  <style>
                    body { font-family: "Segoe UI", Tahoma, sans-serif; margin: 0; background: #f5f2eb; color: #1f2933; }
                    .page { max-width: 1040px; margin: 0 auto; padding: 28px; }
                    .hero { background: linear-gradient(135deg, #fffdf8, #efe4d2); border: 1px solid #d8c7b0; border-radius: 24px; padding: 28px; box-shadow: 0 18px 40px rgba(0,0,0,0.08); }
                    .hero h1 { margin: 0 0 8px; font-size: 38px; }
                    .sub { color: #665a4d; font-size: 16px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 22px; }
                    .card { background: white; border: 1px solid #dccdb7; border-radius: 18px; padding: 16px; }
                    .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: #7a6e61; }
                    .value { font-size: 24px; margin-top: 8px; color: #8b4c1f; font-weight: 700; }
                    .layout { display: grid; grid-template-columns: 1.3fr 0.7fr; gap: 18px; margin-top: 18px; }
                    .section { background: white; border: 1px solid #dccdb7; border-radius: 20px; padding: 22px; }
                    h2 { margin-top: 0; font-size: 22px; }
                    ul, ol { margin: 0; padding-left: 20px; line-height: 1.7; }
                    code { background: #f5eee2; padding: 2px 6px; border-radius: 8px; }
                    @media (max-width: 820px) { .layout { grid-template-columns: 1fr; } }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <section class="hero">
                      <h1>Installation Handoff</h1>
                      <div class="sub">%s for %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Client Type</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Status</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Agents</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Quoted Price</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <div class="layout">
                      <section class="section">
                        <h2>Delivery Summary</h2>
                        <ul>
                          <li>Generated at: %s</li>
                          <li>Bootstrap bundle directory: <code>%s</code></li>
                          <li>Deployment job id: <code>%s</code></li>
                          <li>Quote snapshot count: %s</li>
                          <li>Latest estimated cost: %s</li>
                        </ul>
                        <h2 style="margin-top:22px;">Recommended Delivery Steps</h2>
                        <ol>
                          <li>Review <code>installation.json</code> for install and deployment state.</li>
                          <li>Open <code>bootstrap-bundle.json</code> and follow the referenced bootstrap files.</li>
                          <li>Use <code>quote-summary.json</code> for the current commercial baseline.</li>
                          <li>Use <code>quote-dashboard.json</code> for quote history and trend review.</li>
                          <li>Apply server B env values and complete any missing secrets before launch.</li>
                          <li>Use the Asterisk deploy/runbook metadata from the bootstrap files if server A still needs config application.</li>
                        </ol>
                      </section>
                      <aside class="section">
                        <h2>Commercial Assumptions</h2>
                        <ul>
                          <li>Source: %s</li>
                          <li>Monthly call minutes: %s</li>
                          <li>Monthly TTS units: %s</li>
                          <li>Monthly STT minutes: %s</li>
                          <li>Monthly recording GB: %s</li>
                          <li>Concurrent channels: %s</li>
                          <li>Desired margin: %s</li>
                        </ul>
                      </aside>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(installation.installationName()),
                escapeHtml(safe(quoteSummary.customerName(), safe(installation.customerId(), "<customer>"))),
                escapeHtml(installation.clientType()),
                escapeHtml(installation.status()),
                escapeHtml(String.valueOf(installation.agentCount())),
                escapeHtml(String.valueOf(quoteSummary.estimate().suggestedSellPrice())),
                escapeHtml(generatedAt.toString()),
                escapeHtml(bootstrapBundle.bundleDirectory()),
                escapeHtml(safe(installation.deploymentJobId(), "<not-generated>")),
                escapeHtml(String.valueOf(quoteDashboard.summary() == null ? 0 : quoteDashboard.summary().snapshotCount())),
                escapeHtml(String.valueOf(quoteSummary.estimate().totalEstimatedCost())),
                escapeHtml(assumptions == null ? "<unknown>" : safe(assumptions.source(), "<unknown>")),
                escapeHtml(String.valueOf(assumptions == null ? 0 : assumptions.monthlyCallMinutes())),
                escapeHtml(String.valueOf(assumptions == null ? 0 : assumptions.monthlyTtsUnits())),
                escapeHtml(String.valueOf(assumptions == null ? 0 : assumptions.monthlySttMinutes())),
                escapeHtml(String.valueOf(assumptions == null ? 0D : assumptions.monthlyRecordingGb())),
                escapeHtml(String.valueOf(assumptions == null ? 0 : assumptions.concurrentChannels())),
                escapeHtml(String.valueOf(assumptions == null ? 0D : assumptions.desiredMarginPercent()))
        );
    }

    private String buildReadme(CustomerInstallationResponse installation,
                               QuoteSnapshotDashboardResponse quoteDashboard,
                               Instant generatedAt) {
        return """
                Installation handoff bundle
                ===========================

                Installation: %s
                Generated at: %s

                Files in this bundle:
                - installation.json
                - bootstrap-bundle.json
                - quote-summary.json
                - quote-dashboard.json
                - handoff.md
                - handoff.html
                - README.txt

                Notes:
                - quote-dashboard.json summarizes any saved quote snapshot history for this installation/customer scope.
                - If no quote snapshots have been saved yet, the dashboard summary will show a snapshot count of %s.
                - bootstrap-bundle.json points to the lower-level rollout files already generated for app-stack, softphone, and Asterisk handoff.
                """.formatted(
                installation.installationName(),
                generatedAt,
                quoteDashboard.summary() == null ? 0 : quoteDashboard.summary().snapshotCount()
        );
    }

    private String buildExportReadme(InstallationHandoffResponse handoff) {
        return """
                Installation handoff export
                ===========================

                Installation: %s
                Generated at: %s

                Files:
                - handoff.json
                - handoff.html
                - README.txt
                """.formatted(
                handoff.installationName(),
                handoff.generatedAt()
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
            throw new IllegalStateException("Failed to serialize installation handoff payload", e);
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
