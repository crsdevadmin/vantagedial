package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.dto.CommercialAssumptionsResponse;
import com.vantage.dialer.api.dto.CustomerBootstrapBundleResponse;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class CustomerBootstrapBundleService {

    private final CustomerInstallationService installationService;
    private final CustomerConfigurationService customerConfigurationService;
    private final CustomerQuoteService customerQuoteService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public CustomerBootstrapBundleService(CustomerInstallationService installationService,
                                          CustomerConfigurationService customerConfigurationService,
                                          CustomerQuoteService customerQuoteService,
                                          ObjectMapper objectMapper,
                                          @Value("${app.exports.directory:./exports}") String exportDirectory) {
        this.installationService = installationService;
        this.customerConfigurationService = customerConfigurationService;
        this.customerQuoteService = customerQuoteService;
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of(exportDirectory).resolve("installations");
    }

    public CustomerBootstrapBundleResponse generate(String installationJobId) {
        CustomerInstallationResponse installation = installationService.get(installationJobId);
        CustomerConfigurationResponse customerConfig = installation.customerId() == null ? null
                : customerConfigurationService.find(installation.customerId()).orElse(null);
        ProposalPresetResponse preset = customerConfig == null
                ? null
                : customerConfigurationService.findProposalPreset(customerConfig.proposalPreset()).orElse(null);
        InstallationQuoteSummaryResponse quoteSummary = buildQuoteSummary(installation, customerConfig);
        try {
            Files.createDirectories(exportRoot);
            Path bundleDir = exportRoot.resolve(installation.installationJobId());
            Files.createDirectories(bundleDir);

            List<String> files = new ArrayList<>();
            files.add(write(bundleDir, "installation-summary.json", json(installation)));
            files.add(write(bundleDir, "customer-config.json", json(customerConfig)));
            files.add(write(bundleDir, "preset-metadata.json", json(preset)));
            files.add(write(bundleDir, "commercial-profile.json", json(quoteSummary == null ? null : quoteSummary.commercialAssumptions())));
            files.add(write(bundleDir, ".env.app-stack", buildAppStackEnv(installation, customerConfig)));
            files.add(write(bundleDir, "agents.json", json(installation.provisionedAgents())));
            files.add(write(bundleDir, ".env.softphone", buildSoftphoneEnv(installation, customerConfig, preset)));
            files.add(write(bundleDir, "ui-connection.json", json(buildUiConnectionMetadata(installation, customerConfig, preset))));
            files.add(write(bundleDir, "asterisk-handoff.txt", buildAsteriskHandoff(installation, customerConfig)));
            files.add(write(bundleDir, "README.txt", buildReadme(installation, preset)));

            return new CustomerBootstrapBundleResponse(
                    installation.installationJobId(),
                    installation.installationName(),
                    bundleDir.toAbsolutePath().toString(),
                    bundleDir.resolve("installation-summary.json").toAbsolutePath().toString(),
                    bundleDir.resolve("customer-config.json").toAbsolutePath().toString(),
                    bundleDir.resolve("commercial-profile.json").toAbsolutePath().toString(),
                    bundleDir.resolve(".env.app-stack").toAbsolutePath().toString(),
                    bundleDir.resolve("agents.json").toAbsolutePath().toString(),
                    bundleDir.resolve(".env.softphone").toAbsolutePath().toString(),
                    bundleDir.resolve("ui-connection.json").toAbsolutePath().toString(),
                    bundleDir.resolve("asterisk-handoff.txt").toAbsolutePath().toString(),
                    bundleDir.resolve("README.txt").toAbsolutePath().toString(),
                    Instant.now(),
                    files
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate customer bootstrap bundle", e);
        }
    }

    private String buildAppStackEnv(CustomerInstallationResponse installation, CustomerConfigurationResponse customerConfig) {
        String extension = defaultExtension(installation.provisionedAgents());
        return """
                POSTGRES_DB=vantagedial
                POSTGRES_USER=vantagedial
                POSTGRES_PASSWORD=change-me
                ASTERISK_AMI_HOST=%s
                ASTERISK_AMI_PORT=5038
                ASTERISK_AMI_USERNAME=%s
                ASTERISK_AMI_PASSWORD=change-me
                ASTERISK_AMI_ENDPOINT=%s
                ASTERISK_AMI_DIAL_PREFIX=%s
                ASTERISK_AMI_CONTEXT=from-internal
                ASTERISK_AMI_EXTENSION=%s
                ASTERISK_AMI_PRIORITY=1
                APP_LOGGING_LEVEL=MODERATE
                WORKER_APP_LOGGING_LEVEL=MODERATE
                APP_ASTERISK_DEPLOY_ENABLED=true
                APP_ASTERISK_DEPLOY_HOST=%s
                APP_ASTERISK_DEPLOY_PORT=22
                APP_ASTERISK_DEPLOY_USER=%s
                APP_ASTERISK_DEPLOY_PRIVATE_KEY=%s
                APP_ASTERISK_DEPLOY_REMOTE_BASE_DIRECTORY=/tmp/vantage-asterisk
                APP_ASTERISK_DEPLOY_TARGET_DIRECTORY=%s
                """.formatted(
                coalesce(customerConfig == null ? null : customerConfig.serverAPrivateIp(), "<server-a-private-ip>"),
                coalesce(customerConfig == null ? null : customerConfig.amiUsername(), "admin"),
                coalesce(customerConfig == null ? null : customerConfig.amiEndpoint(), "vivphone-endpoint"),
                coalesce(customerConfig == null ? null : customerConfig.dialPrefix(), "91"),
                extension,
                coalesce(customerConfig == null ? null : customerConfig.serverAHost(), "<server-a-host>"),
                coalesce(customerConfig == null ? null : customerConfig.asteriskDeployUser(), "ubuntu"),
                coalesce(customerConfig == null ? null : customerConfig.asteriskDeployPrivateKeyPath(), "/path/to/private/key"),
                coalesce(customerConfig == null ? null : customerConfig.asteriskDeployTargetDirectory(), "/etc/asterisk/generated")
        );
    }

    private String buildSoftphoneEnv(CustomerInstallationResponse installation,
                                     CustomerConfigurationResponse customerConfig,
                                     ProposalPresetResponse preset) {
        String extension = defaultExtension(installation.provisionedAgents());
        return """
                VITE_SOFTPHONE_MODE=%s
                VITE_ASTERISK_SIP_DOMAIN=%s
                VITE_ASTERISK_WS_URL=%s
                VITE_DEFAULT_EXTENSION=%s
                """.formatted(
                coalesce(customerConfig == null ? null : customerConfig.defaultAgentUiMode(),
                        preset == null ? ("WEBRTC".equals(installation.clientType()) ? "jssip" : "mock") : preset.recommendedAgentUiMode()),
                coalesce(customerConfig == null ? null : customerConfig.sipDomain(), "<server-a-host>"),
                coalesce(customerConfig == null ? null : customerConfig.webSocketUrl(), "wss://<server-a-host>:8089/ws"),
                extension
        );
    }

    private String buildReadme(CustomerInstallationResponse installation, ProposalPresetResponse preset) {
        return """
                Vantage customer bootstrap bundle
                ================================

                Installation: %s
                Client type: %s
                Status: %s
                Preset: %s

                Files in this bundle:
                - installation-summary.json
                - customer-config.json
                - preset-metadata.json
                - commercial-profile.json
                - .env.app-stack
                - agents.json
                - .env.softphone
                - ui-connection.json
                - asterisk-handoff.txt
                - README.txt

                Recommended order:
                1. Review installation-summary.json for the recorded install result.
                2. Review customer-config.json for the resolved customer-specific infrastructure values.
                3. Review preset-metadata.json for preset-driven rollout defaults.
                4. Review commercial-profile.json for the resolved quote assumptions used for pricing.
                5. Fill in real secrets and server addresses in .env.app-stack.
                6. Use agents.json as the customer's provisioned agent inventory.
                7. Review asterisk-handoff.txt for generated include, deploy, and rollback guidance.
                8. Review ui-connection.json for agent UI, supervisor UI, and softphone startup metadata.
                9. If using browser softphone, fill in .env.softphone and start dialer-softphone-ui.
                10. If needed, inspect deployment history through /reports/deployments and installation history through /provisioning/installations.
                """.formatted(
                installation.installationName(),
                installation.clientType(),
                installation.status(),
                preset == null ? "<none>" : preset.presetId()
        );
    }

    private Object buildUiConnectionMetadata(CustomerInstallationResponse installation,
                                            CustomerConfigurationResponse customerConfig,
                                            ProposalPresetResponse preset) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("customerId", installation.customerId());
        metadata.put("installationName", installation.installationName());
        metadata.put("clientType", installation.clientType());
        if (preset != null) {
            metadata.put("preset", preset.presetId());
        }
        metadata.put("agentUi", java.util.Map.of(
                "softphoneMode", coalesce(customerConfig == null ? null : customerConfig.defaultAgentUiMode(),
                        preset == null ? ("WEBRTC".equals(installation.clientType()) ? "jssip" : "mock") : preset.recommendedAgentUiMode()),
                "sipDomain", coalesce(customerConfig == null ? null : customerConfig.sipDomain(), "<server-a-host>"),
                "webSocketUrl", coalesce(customerConfig == null ? null : customerConfig.webSocketUrl(), "wss://<server-a-host>:8089/ws"),
                "defaultExtension", defaultExtension(installation.provisionedAgents())
        ));
        metadata.put("supervisorUi", java.util.Map.of(
                "softphoneMode", coalesce(customerConfig == null ? null : customerConfig.defaultSupervisorUiMode(),
                        preset == null ? "MONITOR_ONLY" : preset.recommendedSupervisorUiMode()),
                "recommendedCapabilities", java.util.List.of("agent-state", "call-state", "click-to-call-escalation")
        ));
        metadata.put("api", java.util.Map.of(
                "baseUrl", coalesce(customerConfig == null ? null : customerConfig.apiBaseUrl(), "http://<server-b-host>:8081"),
                "installationJobId", installation.installationJobId(),
                "deploymentJobId", installation.deploymentJobId()
        ));
        return metadata;
    }

    private InstallationQuoteSummaryResponse buildQuoteSummary(CustomerInstallationResponse installation,
                                                               CustomerConfigurationResponse customerConfig) {
        if (installation.customerId() == null && customerConfig == null) {
            return null;
        }
        CostEstimateRequest request = new CostEstimateRequest();
        request.setCustomerId(installation.customerId());
        request.setUseCustomerPresetDefaults(true);
        return customerQuoteService.quote(installation.installationJobId(), request);
    }

    private String buildAsteriskHandoff(CustomerInstallationResponse installation, CustomerConfigurationResponse customerConfig) {
        return """
                Asterisk handoff
                ================

                Installation: %s
                Customer: %s
                Client type: %s
                Package id: %s
                Deployment job id: %s

                Required includes in /etc/asterisk/pjsip.conf:
                - #include generated/agents.generated.conf
                %s

                Recommended checks:
                - curl "%s/agents/asterisk-preflight?clientType=%s&performRemoteChecks=true"
                - curl %s/reports/deployments

                Remote deploy:
                - curl -X POST "%s/agents/asterisk-deploy?clientType=%s&dryRun=false"

                Rollback hint:
                - restore previous generated/agents.generated.conf on server A
                - run: sudo asterisk -rx "pjsip reload"
                %s
                """.formatted(
                installation.installationName(),
                coalesce(customerConfig == null ? null : customerConfig.customerName(),
                        coalesce(installation.customerId(), "<customer>")),
                installation.clientType(),
                safe(installation.packageId()),
                safe(installation.deploymentJobId()),
                "WEBRTC".equals(installation.clientType()) ? "- #include pjsip-webrtc.conf" : "- no extra WebRTC include required",
                coalesce(customerConfig == null ? null : customerConfig.apiBaseUrl(), "http://<server-b-host>:8081"),
                installation.clientType(),
                coalesce(customerConfig == null ? null : customerConfig.apiBaseUrl(), "http://<server-b-host>:8081"),
                coalesce(customerConfig == null ? null : customerConfig.apiBaseUrl(), "http://<server-b-host>:8081"),
                installation.clientType(),
                "WEBRTC".equals(installation.clientType())
                        ? "- for browser clients also verify /etc/asterisk/http.conf and /etc/asterisk/pjsip-webrtc.conf"
                        : ""
        );
    }

    private String defaultExtension(List<Agent> agents) {
        return agents.isEmpty() ? "1001" : agents.get(0).getExtensionNumber();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "<not-generated>" : value;
    }

    private String coalesce(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
            throw new IllegalStateException("Failed to serialize bootstrap bundle payload", e);
        }
    }
}
