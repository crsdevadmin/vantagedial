package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.dto.CommercialAssumptionsResponse;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CostEstimateResponse;
import com.vantage.dialer.api.dto.CustomerBootstrapBundleResponse;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerBootstrapBundleServiceTest {

    @Test
    void generateWritesBootstrapArtifactsAndResolvedEnvFiles(@TempDir Path tempDir) throws Exception {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        CustomerConfigurationService customerConfigurationService = mock(CustomerConfigurationService.class);
        CustomerQuoteService customerQuoteService = mock(CustomerQuoteService.class);
        CustomerBootstrapBundleService service = new CustomerBootstrapBundleService(
                installationService,
                customerConfigurationService,
                customerQuoteService,
                CustomerServiceTestFixtures.objectMapper(),
                tempDir.toString()
        );

        CustomerInstallationResponse installation = installation();
        CustomerConfigurationResponse configuration = configuration();
        ProposalPresetResponse preset = preset();
        InstallationQuoteSummaryResponse quoteSummary = quoteSummary(configuration);

        when(installationService.get("install-1")).thenReturn(installation);
        when(customerConfigurationService.find("customer-1")).thenReturn(Optional.of(configuration));
        when(customerConfigurationService.findProposalPreset("FULL_SUITE")).thenReturn(Optional.of(preset));
        when(customerQuoteService.quote(eq("install-1"), org.mockito.ArgumentMatchers.any(CostEstimateRequest.class)))
                .thenReturn(quoteSummary);

        CustomerBootstrapBundleResponse bundle = service.generate("install-1");

        ArgumentCaptor<CostEstimateRequest> quoteRequestCaptor = ArgumentCaptor.forClass(CostEstimateRequest.class);
        verify(customerQuoteService).quote(eq("install-1"), quoteRequestCaptor.capture());

        JsonNode configJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.customerConfigPath())));
        JsonNode uiConnectionJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.uiConnectionPath())));
        JsonNode commercialJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.commercialProfilePath())));
        JsonNode agentsJson = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(bundle.agentInventoryPath())));
        String appStackEnv = Files.readString(Path.of(bundle.appStackEnvPath()));
        String softphoneEnv = Files.readString(Path.of(bundle.softphoneEnvPath()));
        String handoff = Files.readString(Path.of(bundle.asteriskHandoffPath()));
        String readme = Files.readString(Path.of(bundle.readmePath()));

        assertEquals("customer-1", quoteRequestCaptor.getValue().getCustomerId());
        assertTrue(Boolean.TRUE.equals(quoteRequestCaptor.getValue().getUseCustomerPresetDefaults()));
        assertEquals(10, bundle.files().size());
        assertEquals("Acme Corp", configJson.get("customerName").asText());
        assertEquals("FULL_SUITE", configJson.get("proposalPreset").asText());
        assertEquals("preset", commercialJson.get("source").asText());
        assertEquals("1001", agentsJson.get(0).get("extensionNumber").asText());
        assertEquals("1001", uiConnectionJson.get("agentUi").get("defaultExtension").asText());
        assertEquals("http://api.acme.test:8081", uiConnectionJson.get("api").get("baseUrl").asText());
        assertTrue(appStackEnv.contains("APP_ASTERISK_DEPLOY_HOST=pbx.acme.test"));
        assertTrue(appStackEnv.contains("ASTERISK_AMI_EXTENSION=1001"));
        assertTrue(softphoneEnv.contains("VITE_SOFTPHONE_MODE=jssip"));
        assertTrue(softphoneEnv.contains("VITE_DEFAULT_EXTENSION=1001"));
        assertTrue(handoff.contains("#include pjsip-webrtc.conf"));
        assertTrue(handoff.contains("curl -X POST \"http://api.acme.test:8081/agents/asterisk-deploy?clientType=WEBRTC&dryRun=false\""));
        assertTrue(readme.contains("Preset: FULL_SUITE"));
        assertTrue(readme.contains(".env.softphone"));
    }

    private CustomerInstallationResponse installation() {
        Agent agent = new Agent("agent-1", "Agent One", "PJSIP/1001", "1001", "user1", "pass1", AgentStatus.AVAILABLE);
        return new CustomerInstallationResponse(
                "install-1",
                "customer-1",
                "Acme Softphone",
                "WEBRTC",
                "COMPLETED",
                false,
                true,
                true,
                1,
                "pkg-1",
                "deploy-1",
                List.of(agent),
                null,
                null,
                Instant.parse("2026-03-22T11:00:00Z"),
                Instant.parse("2026-03-22T12:00:00Z"),
                "Installation completed",
                null
        );
    }

    private CustomerConfigurationResponse configuration() {
        return new CustomerConfigurationResponse(
                "customer-1",
                "Acme Corp",
                "pbx.acme.test",
                "10.0.0.10",
                "api.acme.test",
                "ubuntu",
                "/keys/pbx.pem",
                "/etc/asterisk/generated",
                "admin",
                "vivphone-endpoint",
                "91",
                "pbx.acme.test",
                "wss://pbx.acme.test/ws",
                "http://api.acme.test:8081",
                "jssip",
                "MONITOR_ONLY",
                "Acme Corp",
                null,
                "#1f2a2a",
                "#1c7c54",
                "FULL_SUITE",
                "STANDARD",
                "Vantage Dialer Proposal",
                "Outbound rollout",
                true,
                true,
                true,
                true,
                true,
                true,
                25000L,
                120000L,
                5000L,
                35.0,
                2,
                10,
                35.0,
                "Net 30"
        );
    }

    private ProposalPresetResponse preset() {
        return new ProposalPresetResponse(
                "FULL_SUITE",
                "Full rollout",
                true,
                true,
                true,
                true,
                true,
                true,
                "WEBRTC",
                "jssip",
                "MONITOR_ONLY",
                true,
                true,
                25000L,
                120000L,
                5000L,
                35.0,
                2,
                10,
                35.0
        );
    }

    private InstallationQuoteSummaryResponse quoteSummary(CustomerConfigurationResponse configuration) {
        return new InstallationQuoteSummaryResponse(
                "install-1",
                "customer-1",
                "Acme Softphone",
                "Acme Corp",
                "WEBRTC",
                "COMPLETED",
                1,
                List.of("1001"),
                configuration,
                new CommercialAssumptionsResponse(
                        "preset",
                        25000L,
                        120000L,
                        5000L,
                        35.0,
                        2,
                        10,
                        35.0
                ),
                new CostEstimateResponse(
                        "customer-1",
                        "default",
                        40.0,
                        72.0,
                        112.0,
                        149.5,
                        35.0
                ),
                Instant.parse("2026-03-22T12:00:00Z")
        );
    }
}
