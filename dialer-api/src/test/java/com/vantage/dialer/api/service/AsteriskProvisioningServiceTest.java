package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.dto.AsteriskAgentConfigResponse;
import com.vantage.dialer.api.dto.AsteriskClientType;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsteriskProvisioningServiceTest {

    @Test
    void renderAgentConfigSupportsSoftphoneAndWebRtc() {
        AsteriskProvisioningService service = new AsteriskProvisioningService(CustomerServiceTestFixtures.objectMapper());
        Agent agent = agent("agent-1", "1001");

        AsteriskAgentConfigResponse defaultConfig = service.renderAgentConfig(agent);
        AsteriskAgentConfigResponse softphone = service.renderAgentConfig(agent, AsteriskClientType.SOFTPHONE);
        AsteriskAgentConfigResponse webRtc = service.renderAgentConfig(agent, AsteriskClientType.WEBRTC);

        assertEquals("SOFTPHONE", defaultConfig.clientType());
        assertEquals("1001", defaultConfig.extensionNumber());
        assertEquals("SOFTPHONE", softphone.clientType());
        assertEquals("default-udp", softphone.transportName());
        assertTrue(softphone.endpointSnippet().contains("allow=ulaw,alaw"));
        assertTrue(softphone.includeHint().contains("generated/agents.generated.conf"));

        assertEquals("WEBRTC", webRtc.clientType());
        assertEquals("transport-wss", webRtc.transportName());
        assertTrue(webRtc.webSocketPath().contains("8089/ws"));
        assertTrue(webRtc.endpointSnippet().contains("webrtc=yes"));
        assertTrue(webRtc.endpointSnippet().contains("media_encryption=dtls"));
        assertTrue(webRtc.includeHint().contains("#include pjsip-webrtc.conf"));
    }

    @Test
    void generateAllAgentsPackageWritesManifestAndWebRtcSupportFiles(@TempDir Path tempDir) throws Exception {
        AsteriskProvisioningService service = new AsteriskProvisioningService(CustomerServiceTestFixtures.objectMapper());
        setField(service, "exportRoot", tempDir.resolve("asterisk"));

        AsteriskDeploymentPackageResponse deploymentPackage = service.generateAllAgentsPackage(
                List.of(agent("agent-1", "1001"), agent("agent-2", "1002")),
                AsteriskClientType.WEBRTC
        );

        JsonNode manifest = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(deploymentPackage.manifestPath())));
        String generatedConfig = Files.readString(Path.of(deploymentPackage.pjsipConfigPath()));
        String reloadScript = Files.readString(Path.of(deploymentPackage.reloadScriptPath()));
        String softphoneEnv = Files.readString(Path.of(deploymentPackage.packageDirectory()).resolve("dialer-softphone.env"));

        assertEquals("WEBRTC", deploymentPackage.clientType());
        assertTrue(deploymentPackage.bundledFiles().contains("http.conf"));
        assertTrue(deploymentPackage.bundledFiles().contains("rtp.conf"));
        assertTrue(deploymentPackage.bundledFiles().contains("pjsip-webrtc.conf"));
        assertTrue(deploymentPackage.bundledFiles().contains("modules.conf.append"));
        assertTrue(deploymentPackage.bundledFiles().contains("dialer-softphone.env"));
        assertTrue(deploymentPackage.bundledFiles().contains("README-WEBRTC.txt"));
        assertTrue(generatedConfig.contains("[1001]"));
        assertTrue(generatedConfig.contains("[1002]"));
        assertTrue(generatedConfig.contains("transport=transport-wss"));
        assertTrue(reloadScript.contains("http reload"));
        assertTrue(reloadScript.contains("pjsip-webrtc.conf"));
        assertTrue(softphoneEnv.contains("VITE_SOFTPHONE_MODE=jssip"));
        assertEquals("WEBRTC", manifest.get("clientType").asText());
        assertEquals(2, manifest.get("agents").size());
    }

    @Test
    void generateSingleAgentSoftphonePackageOmitsWebRtcArtifacts(@TempDir Path tempDir) throws Exception {
        AsteriskProvisioningService service = new AsteriskProvisioningService(CustomerServiceTestFixtures.objectMapper());
        setField(service, "exportRoot", tempDir.resolve("asterisk"));

        AsteriskDeploymentPackageResponse deploymentPackage = service.generateSingleAgentPackage(
                agent("agent-1", "1001"),
                AsteriskClientType.SOFTPHONE
        );

        JsonNode manifest = CustomerServiceTestFixtures.objectMapper()
                .readTree(Files.readString(Path.of(deploymentPackage.manifestPath())));
        String generatedConfig = Files.readString(Path.of(deploymentPackage.pjsipConfigPath()));
        String reloadScript = Files.readString(Path.of(deploymentPackage.reloadScriptPath()));

        assertEquals("SOFTPHONE", deploymentPackage.clientType());
        assertEquals(List.of("agent-1"), deploymentPackage.agentIds());
        assertEquals(List.of("agents.generated.conf", "apply-and-reload.sh", "manifest.json"), deploymentPackage.bundledFiles());
        assertFalse(deploymentPackage.bundledFiles().contains("http.conf"));
        assertFalse(deploymentPackage.bundledFiles().contains("dialer-softphone.env"));
        assertTrue(generatedConfig.contains("allow=ulaw,alaw"));
        assertFalse(generatedConfig.contains("transport=transport-wss"));
        assertTrue(reloadScript.contains("pjsip reload"));
        assertFalse(reloadScript.contains("http reload"));
        assertEquals("single-agent", manifest.get("packageType").asText());
        assertEquals("SOFTPHONE", manifest.get("clientType").asText());
        assertEquals(1, manifest.get("agents").size());
        assertEquals("agent-1", manifest.get("agents").get(0).get("agentId").asText());
    }

    @Test
    void generateSingleAgentWebRtcPackageSetsDefaultExtensionInEnvFile(@TempDir Path tempDir) throws Exception {
        AsteriskProvisioningService service = new AsteriskProvisioningService(CustomerServiceTestFixtures.objectMapper());
        setField(service, "exportRoot", tempDir.resolve("asterisk"));

        AsteriskDeploymentPackageResponse deploymentPackage = service.generateSingleAgentPackage(
                agent("agent-9", "1099"),
                AsteriskClientType.WEBRTC
        );

        String softphoneEnv = Files.readString(Path.of(deploymentPackage.packageDirectory()).resolve("dialer-softphone.env"));

        assertTrue(softphoneEnv.contains("VITE_SOFTPHONE_MODE=jssip"));
        assertTrue(softphoneEnv.contains("VITE_DEFAULT_EXTENSION=1099"));
    }

    private Agent agent(String agentId, String extension) {
        return new Agent(agentId, "Agent " + extension, "PJSIP/" + extension, extension, "user" + extension, "pass" + extension, AgentStatus.AVAILABLE);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
