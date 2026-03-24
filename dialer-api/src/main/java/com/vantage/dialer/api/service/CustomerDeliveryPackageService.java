package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageDetailResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageExportResponse;
import com.vantage.dialer.api.dto.CustomerDeliveryPackageResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogResponse;
import com.vantage.dialer.api.dto.InstallationArtifactCatalogBundleResponse;
import com.vantage.dialer.api.dto.InstallationHandoffBundleResponse;
import com.vantage.dialer.api.dto.InstallationHandoffExportResponse;
import com.vantage.dialer.api.dto.InstallationHandoffResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceBundleResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceExportResponse;
import com.vantage.dialer.api.dto.InstallationWorkspaceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerDeliveryPackageService {

    private final CustomerInstallationService installationService;
    private final InstallationHandoffBundleService handoffBundleService;
    private final InstallationDashboardService installationDashboardService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerDeliveryPackageService(CustomerInstallationService installationService,
                                          InstallationHandoffBundleService handoffBundleService,
                                          InstallationDashboardService installationDashboardService,
                                          ObjectMapper objectMapper,
                                          @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.installationService = installationService;
        this.handoffBundleService = handoffBundleService;
        this.installationDashboardService = installationDashboardService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("deliveries");
    }

    public CustomerDeliveryPackageResponse generate(String installationJobId) {
        CustomerInstallationResponse installation = installationService.get(installationJobId);
        InstallationHandoffBundleResponse handoffBundle = handoffBundleService.generate(installationJobId);
        InstallationHandoffExportResponse handoffExport = handoffBundleService.export(installationJobId);
        InstallationWorkspaceBundleResponse workspaceBundle =
                installationDashboardService.generateWorkspaceBundle(installation.customerId());
        InstallationWorkspaceExportResponse workspaceExport =
                installationDashboardService.exportWorkspace(installation.customerId());
        InstallationArtifactCatalogBundleResponse artifactCatalogBundle =
                installationDashboardService.generateArtifactCatalogBundle(installation.customerId());
        Instant generatedAt = Instant.now();

        try {
            Path packageDirectory = exportRoot.resolve(installation.installationJobId());
            Files.createDirectories(packageDirectory);

            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("installation", installation);
            manifest.put("handoffBundle", handoffBundle);
            manifest.put("handoffExport", handoffExport);
            manifest.put("workspaceBundle", workspaceBundle);
            manifest.put("workspaceExport", workspaceExport);
            manifest.put("artifactCatalogBundle", artifactCatalogBundle);
            manifest.put("generatedAt", generatedAt);

            List<String> files = new ArrayList<>();
            files.add(write(packageDirectory, "delivery-package.json", json(manifest)));
            files.add(write(packageDirectory, "README.txt", buildReadme(installation, generatedAt)));

            return new CustomerDeliveryPackageResponse(
                    installation.installationJobId(),
                    installation.customerId(),
                    packageDirectory.toAbsolutePath().toString(),
                    packageDirectory.resolve("delivery-package.json").toAbsolutePath().toString(),
                    packageDirectory.resolve("README.txt").toAbsolutePath().toString(),
                    generatedAt,
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer delivery package", e);
        }
    }

    public CustomerDeliveryPackageDetailResponse detail(String installationJobId) {
        CustomerInstallationResponse installation = installationService.get(installationJobId);
        InstallationHandoffResponse handoff = handoffBundleService.handoff(installationJobId);
        InstallationWorkspaceResponse workspace = installationDashboardService.workspace(installation.customerId());
        InstallationArtifactCatalogResponse artifactCatalog =
                installationDashboardService.generateArtifactCatalog(installation.customerId());
        return new CustomerDeliveryPackageDetailResponse(
                installation.installationJobId(),
                installation.customerId(),
                Instant.now(),
                installation,
                handoff,
                workspace,
                artifactCatalog
        );
    }

    public CustomerDeliveryPackageExportResponse export(String installationJobId) {
        CustomerDeliveryPackageDetailResponse detail = detail(installationJobId);
        Instant generatedAt = detail.generatedAt();
        try {
            Path exportDirectory = exportRoot.resolve(detail.installationJobId()).resolve("delivery-package-export");
            Files.createDirectories(exportDirectory);

            Path packageJsonPath = exportDirectory.resolve("delivery-package.json");
            Path packageHtmlPath = exportDirectory.resolve("delivery-package.html");
            Path readmePath = exportDirectory.resolve("README.txt");

            Files.writeString(packageJsonPath, json(detail));
            Files.writeString(packageHtmlPath, buildHtml(detail));
            Files.writeString(readmePath, buildExportReadme(detail));

            return new CustomerDeliveryPackageExportResponse(
                    detail.installationJobId(),
                    exportDirectory.toAbsolutePath().toString(),
                    packageJsonPath.toAbsolutePath().toString(),
                    packageHtmlPath.toAbsolutePath().toString(),
                    readmePath.toAbsolutePath().toString(),
                    generatedAt
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export customer delivery package", e);
        }
    }

    private String buildReadme(CustomerInstallationResponse installation, Instant generatedAt) {
        return """
                Customer delivery package
                ========================

                Installation: %s
                Customer: %s
                Generated at: %s

                Files:
                - delivery-package.json
                - README.txt

                This delivery package references:
                - handoff bundle and export
                - workspace bundle and export
                - installation artifact catalog bundle
                """.formatted(
                installation.installationName(),
                installation.customerId() == null ? "ALL" : installation.customerId(),
                generatedAt
        );
    }

    private String buildHtml(CustomerDeliveryPackageDetailResponse detail) {
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
                      <div>Installation: %s</div>
                      <div>Generated at: %s</div>
                      <div class="grid">
                        <div class="card"><div class="label">Customer</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Status</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Agents</div><div class="value">%s</div></div>
                        <div class="card"><div class="label">Failures</div><div class="value">%s</div></div>
                      </div>
                    </section>
                    <section class="section">
                      <h2>Included Components</h2>
                      <ul>
                        <li>Handoff client type: %s</li>
                        <li>Workspace dashboard total installs: %s</li>
                        <li>Artifact catalog workspace bundle: %s</li>
                      </ul>
                    </section>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(detail.installation().installationName()),
                escapeHtml(detail.generatedAt().toString()),
                escapeHtml(detail.customerId() == null ? "ALL" : detail.customerId()),
                escapeHtml(detail.installation().status()),
                escapeHtml(String.valueOf(detail.installation().agentCount())),
                escapeHtml(String.valueOf(detail.workspace().health().failedInstallations())),
                escapeHtml(detail.handoff().installation().clientType()),
                escapeHtml(String.valueOf(detail.workspace().dashboard().totalInstallations())),
                escapeHtml(detail.artifactCatalog().workspaceBundle().bundleDirectory())
        );
    }

    private String buildExportReadme(CustomerDeliveryPackageDetailResponse detail) {
        return """
                Customer delivery package export
                ===============================

                Installation: %s
                Generated at: %s

                Files:
                - delivery-package.json
                - delivery-package.html
                - README.txt
                """.formatted(
                detail.installation().installationName(),
                detail.generatedAt()
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
