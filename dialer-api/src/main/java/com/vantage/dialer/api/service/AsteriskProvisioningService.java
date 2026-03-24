package com.vantage.dialer.api.service;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.dto.AsteriskClientType;
import com.vantage.dialer.api.dto.AsteriskAgentConfigResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AsteriskProvisioningService {

    private final ObjectMapper objectMapper;
    private final Path exportRoot;

    public AsteriskProvisioningService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.exportRoot = Path.of("./exports/asterisk");
    }

    public AsteriskAgentConfigResponse renderAgentConfig(Agent agent) {
        return renderAgentConfig(agent, AsteriskClientType.SOFTPHONE);
    }

    public AsteriskAgentConfigResponse renderAgentConfig(Agent agent, AsteriskClientType clientType) {
        String extension = agent.getExtensionNumber();
        String snippet = clientType == AsteriskClientType.WEBRTC
                ? buildWebRtcSnippet(agent, extension)
                : buildSoftphoneSnippet(agent, extension);
        String transportName = clientType == AsteriskClientType.WEBRTC ? "transport-wss" : "default-udp";
        String webSocketPath = clientType == AsteriskClientType.WEBRTC ? "wss://<asterisk-host>:8089/ws" : "";
        String includeHint = clientType == AsteriskClientType.WEBRTC
                ? "#include pjsip-webrtc.conf" + System.lineSeparator() + "#include generated/agents.generated.conf"
                : "#include generated/agents.generated.conf";

        return new AsteriskAgentConfigResponse(
                agent.getAgentId(),
                extension,
                clientType.name(),
                snippet,
                agent.getSipUsername(),
                agent.getSipPassword(),
                transportName,
                webSocketPath,
                includeHint
        );
    }

    public AsteriskDeploymentPackageResponse generateSingleAgentPackage(Agent agent, AsteriskClientType clientType) {
        return writePackage("single-agent", List.of(agent), clientType);
    }

    public AsteriskDeploymentPackageResponse generateAllAgentsPackage(List<Agent> agents, AsteriskClientType clientType) {
        return writePackage("all-agents", agents, clientType);
    }

    private AsteriskDeploymentPackageResponse writePackage(String packageType, List<Agent> agents, AsteriskClientType clientType) {
        try {
            Files.createDirectories(exportRoot);
            String packageId = packageType + "-" + clientType.name().toLowerCase() + "-" +
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC));
            Path packageDir = exportRoot.resolve(packageId);
            Files.createDirectories(packageDir);

            String combinedConfig = agents.stream()
                    .map(agent -> renderAgentConfig(agent, clientType))
                    .map(AsteriskAgentConfigResponse::endpointSnippet)
                    .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

            Path pjsipPath = packageDir.resolve("agents.generated.conf");
            Files.writeString(pjsipPath, combinedConfig + System.lineSeparator());

            Path reloadScript = packageDir.resolve("apply-and-reload.sh");
            Files.writeString(reloadScript, buildReloadScript(packageType, clientType));

            List<String> bundledFiles = new ArrayList<>();
            bundledFiles.add(pjsipPath.getFileName().toString());
            bundledFiles.add(reloadScript.getFileName().toString());

            if (clientType == AsteriskClientType.WEBRTC) {
                bundledFiles.addAll(writeWebRtcSupportFiles(packageDir, agents));
            }

            Path manifestPath = packageDir.resolve("manifest.json");
            Files.writeString(manifestPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    buildManifest(packageId, packageType, clientType, agents, pjsipPath, reloadScript, bundledFiles)));
            bundledFiles.add(manifestPath.getFileName().toString());

            return new AsteriskDeploymentPackageResponse(
                    packageId,
                    packageType,
                    clientType.name(),
                    packageDir.toAbsolutePath().toString(),
                    pjsipPath.toAbsolutePath().toString(),
                    reloadScript.toAbsolutePath().toString(),
                    manifestPath.toAbsolutePath().toString(),
                    Instant.now(),
                    agents.stream().map(Agent::getAgentId).toList(),
                    bundledFiles
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate Asterisk deployment package", e);
        }
    }

    private Map<String, Object> buildManifest(String packageId,
                                              String packageType,
                                              AsteriskClientType clientType,
                                              List<Agent> agents,
                                              Path pjsipPath,
                                              Path reloadScript,
                                              List<String> bundledFiles) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("packageId", packageId);
        manifest.put("packageType", packageType);
        manifest.put("clientType", clientType.name());
        manifest.put("generatedAt", Instant.now().toString());
        manifest.put("pjsipConfigPath", pjsipPath.toAbsolutePath().toString());
        manifest.put("reloadScriptPath", reloadScript.toAbsolutePath().toString());
        manifest.put("bundledFiles", bundledFiles);
        manifest.put("agents", agents.stream().map(agent -> Map.of(
                "agentId", agent.getAgentId(),
                "extensionNumber", agent.getExtensionNumber(),
                "sipUsername", agent.getSipUsername(),
                "channel", agent.getChannel()
        )).toList());
        return manifest;
    }

    private String buildReloadScript(String packageType, AsteriskClientType clientType) {
        String webRtcReloadSteps = clientType == AsteriskClientType.WEBRTC
                ? """
                sudo cp "$(cd "$(dirname "$0")" && pwd)/http.conf" /etc/asterisk/http.conf
                sudo cp "$(cd "$(dirname "$0")" && pwd)/rtp.conf" /etc/asterisk/rtp.conf
                sudo cp "$(cd "$(dirname "$0")" && pwd)/pjsip-webrtc.conf" /etc/asterisk/pjsip-webrtc.conf
                sudo asterisk -rx "module reload"
                sudo asterisk -rx "http reload"
                """
                : "";

        return """
                #!/usr/bin/env bash
                set -euo pipefail

                TARGET_DIR=${1:-/etc/asterisk/generated}
                SOURCE_FILE="$(cd "$(dirname "$0")" && pwd)/agents.generated.conf"
                TARGET_FILE="${TARGET_DIR}/agents.generated.conf"

                sudo mkdir -p "${TARGET_DIR}"
                sudo cp "${SOURCE_FILE}" "${TARGET_FILE}"
                sudo chown asterisk:asterisk "${TARGET_FILE}"
                sudo chmod 640 "${TARGET_FILE}"
                %s
                sudo asterisk -rx "pjsip reload"
                echo "Applied %s package to ${TARGET_FILE} and reloaded PJSIP"
                """.formatted(webRtcReloadSteps, packageType);
    }

    private List<String> writeWebRtcSupportFiles(Path packageDir, List<Agent> agents) throws IOException {
        List<String> bundled = new ArrayList<>();
        bundled.add(writeSupportFile(packageDir, "http.conf", defaultHttpConfig()));
        bundled.add(writeSupportFile(packageDir, "rtp.conf", defaultRtpConfig()));
        bundled.add(writeSupportFile(packageDir, "pjsip-webrtc.conf", defaultPjsipWebRtcConfig()));
        bundled.add(writeSupportFile(packageDir, "modules.conf.append", defaultModulesConfig()));
        bundled.add(writeSupportFile(packageDir, "dialer-softphone.env", defaultSoftphoneEnv(agents)));
        bundled.add(writeSupportFile(packageDir, "README-WEBRTC.txt", defaultWebRtcReadme()));
        return bundled;
    }

    private String writeSupportFile(Path packageDir, String fileName, String content) throws IOException {
        Path output = packageDir.resolve(fileName);
        Files.writeString(output, content);
        return output.getFileName().toString();
    }

    private String buildSoftphoneSnippet(Agent agent, String extension) {
        return """
                [%s]
                type=endpoint
                context=from-internal
                disallow=all
                allow=ulaw,alaw
                auth=%s-auth
                aors=%s
                callerid=%s <%s>
                rtp_symmetric=yes
                force_rport=yes
                rewrite_contact=yes
                direct_media=no

                [%s-auth]
                type=auth
                auth_type=userpass
                username=%s
                password=%s

                [%s]
                type=aor
                max_contacts=1
                remove_existing=yes
                """.formatted(
                extension,
                extension,
                extension,
                agent.getAgentName(),
                extension,
                extension,
                agent.getSipUsername(),
                agent.getSipPassword(),
                extension
        );
    }

    private String buildWebRtcSnippet(Agent agent, String extension) {
        return """
                [%s]
                type=endpoint
                ; Requires pjsip-webrtc.conf to be included in pjsip.conf
                transport=transport-wss
                context=from-internal
                disallow=all
                allow=opus,ulaw,alaw
                auth=%s-auth
                aors=%s
                callerid=%s <%s>
                webrtc=yes
                use_avpf=yes
                media_encryption=dtls
                dtls_auto_generate_cert=yes
                dtls_verify=fingerprint
                dtls_setup=actpass
                ice_support=yes
                media_use_received_transport=yes
                rtcp_mux=yes
                force_rport=yes
                rewrite_contact=yes
                direct_media=no

                [%s-auth]
                type=auth
                auth_type=userpass
                username=%s
                password=%s

                [%s]
                type=aor
                max_contacts=1
                remove_existing=yes
                """.formatted(
                extension,
                extension,
                extension,
                agent.getAgentName(),
                extension,
                extension,
                agent.getSipUsername(),
                agent.getSipPassword(),
                extension
        );
    }

    private String defaultHttpConfig() {
        return """
                [general]
                enabled=yes
                bindaddr=0.0.0.0
                bindport=8088

                [ws]
                enabled=yes
                bindaddr=0.0.0.0
                bindport=8089
                """;
    }

    private String defaultRtpConfig() {
        return """
                [general]
                rtpstart=10000
                rtpend=20000
                icesupport=true
                stunaddr=
                """;
    }

    private String defaultPjsipWebRtcConfig() {
        return """
                ; Base WebRTC transport/profile for browser softphones.
                ; Include this alongside your generated agent endpoint config.

                [transport-wss]
                type=transport
                protocol=wss
                bind=0.0.0.0
                allow_reload=yes

                [webrtc-template](!)
                type=endpoint
                transport=transport-wss
                context=from-internal
                disallow=all
                allow=opus,ulaw,alaw
                webrtc=yes
                use_avpf=yes
                media_encryption=dtls
                dtls_auto_generate_cert=yes
                dtls_verify=fingerprint
                dtls_setup=actpass
                ice_support=yes
                media_use_received_transport=yes
                rtcp_mux=yes
                force_rport=yes
                rewrite_contact=yes
                direct_media=no
                """;
    }

    private String defaultModulesConfig() {
        return """
                ; Ensure WebSocket and PJSIP modules required for WebRTC are loaded.
                load => res_http_websocket.so
                load => res_pjsip_transport_websocket.so
                load => res_pjsip.so
                load => res_rtp_asterisk.so
                load => res_srtp.so
                """;
    }

    private String defaultSoftphoneEnv(List<Agent> agents) {
        String defaultExtension = agents.size() == 1 ? agents.get(0).getExtensionNumber() : "";
        return """
                VITE_SOFTPHONE_MODE=jssip
                VITE_ASTERISK_SIP_DOMAIN=<asterisk-host>
                VITE_ASTERISK_WS_URL=wss://<asterisk-host>:8089/ws
                VITE_DEFAULT_EXTENSION=%s
                """.formatted(defaultExtension);
    }

    private String defaultWebRtcReadme() {
        return """
                WebRTC package contents
                ======================

                1. Copy http.conf, rtp.conf, and pjsip-webrtc.conf to /etc/asterisk/.
                2. Ensure pjsip.conf includes:
                   #include pjsip-webrtc.conf
                   #include generated/agents.generated.conf
                3. Merge modules.conf.append entries into your active modules.conf if they are missing.
                4. Run ./apply-and-reload.sh on the Asterisk host.
                5. Start dialer-softphone-ui with:
                   VITE_SOFTPHONE_MODE=jssip
                6. Use a browser-reachable WSS endpoint such as:
                   wss://<asterisk-host>:8089/ws

                Browsers require valid TLS when using WSS in production.
                """;
    }
}
