package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.dto.AgentProvisionRequest;
import com.vantage.dialer.api.dto.AsteriskAgentConfigResponse;
import com.vantage.dialer.api.dto.AsteriskClientType;
import com.vantage.dialer.api.dto.AsteriskDeploymentExecutionResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPreflightResponse;
import com.vantage.dialer.api.dto.AsteriskPreflightCheckResponse;
import com.vantage.dialer.api.service.AsteriskDeploymentRunnerService;
import com.vantage.dialer.api.service.AsteriskProvisioningService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentControllerTest {

    @Test
    void listCreateAndUpdateEndpointsUseAgentStore() throws Exception {
        AgentStore agentStore = mock(AgentStore.class);
        AsteriskProvisioningService provisioningService = mock(AsteriskProvisioningService.class);
        AsteriskDeploymentRunnerService deploymentRunnerService = mock(AsteriskDeploymentRunnerService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new AgentController(agentStore, provisioningService, deploymentRunnerService));

        Agent agent = agent("A1", "Alice", AgentStatus.AVAILABLE);
        when(agentStore.getAgents()).thenReturn(List.of(agent));
        when(agentStore.createOrUpdate(any(AgentProvisionRequest.class)))
                .thenAnswer(invocation -> {
                    AgentProvisionRequest request = invocation.getArgument(0);
                    return new Agent(request.getAgentId(), request.getAgentName(), "PJSIP/2001", "2001", "2001", "secret", AgentStatus.AVAILABLE);
                });

        AgentProvisionRequest createRequest = new AgentProvisionRequest();
        createRequest.setAgentId("A1");
        createRequest.setAgentName("Alice");
        createRequest.setExtensionNumber("2001");

        mockMvc.perform(get("/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agentId").value("A1"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        mockMvc.perform(post("/agents")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value("A1"))
                .andExpect(jsonPath("$.agentName").value("Alice"));

        mockMvc.perform(put("/agents/A9")
                        .contentType(APPLICATION_JSON)
                        .content(ControllerTestSupport.json(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value("A9"));

        ArgumentCaptor<AgentProvisionRequest> captor = ArgumentCaptor.forClass(AgentProvisionRequest.class);
        verify(agentStore, org.mockito.Mockito.times(2)).createOrUpdate(captor.capture());
        assertEquals("A1", captor.getAllValues().get(0).getAgentId());
        assertEquals("A9", captor.getAllValues().get(1).getAgentId());
    }

    @Test
    void deleteAndManualStatusEndpointsReturnCurrentStringPayloads() throws Exception {
        AgentStore agentStore = mock(AgentStore.class);
        AsteriskProvisioningService provisioningService = mock(AsteriskProvisioningService.class);
        AsteriskDeploymentRunnerService deploymentRunnerService = mock(AsteriskDeploymentRunnerService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new AgentController(agentStore, provisioningService, deploymentRunnerService));

        mockMvc.perform(delete("/agents/A1"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"Agent A1 deleted\""));

        mockMvc.perform(post("/agents/A1/available"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"Agent A1 set to AVAILABLE\""));

        mockMvc.perform(post("/agents/A1/busy"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"Agent A1 set to BUSY\""));

        verify(agentStore).delete("A1");
        verify(agentStore).releaseAgent("A1");
        verify(agentStore).markBusy("A1");
    }

    @Test
    void configAndPackageEndpointsResolveAgentAndClientType() throws Exception {
        AgentStore agentStore = mock(AgentStore.class);
        AsteriskProvisioningService provisioningService = mock(AsteriskProvisioningService.class);
        AsteriskDeploymentRunnerService deploymentRunnerService = mock(AsteriskDeploymentRunnerService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new AgentController(agentStore, provisioningService, deploymentRunnerService));
        Agent agent = agent("A1", "Alice", AgentStatus.AVAILABLE);
        AsteriskDeploymentPackageResponse singlePackage = new AsteriskDeploymentPackageResponse(
                "pkg-1", "SINGLE_AGENT", "WEBRTC", "pkg-dir", "pjsip.conf", "reload.sh", "manifest.json",
                Instant.parse("2026-03-22T10:00:00Z"), List.of("A1"), List.of("manifest.json")
        );
        AsteriskDeploymentPackageResponse allPackage = new AsteriskDeploymentPackageResponse(
                "pkg-all", "ALL_AGENTS", "SOFTPHONE", "pkg-all", "pjsip.conf", "reload.sh", "manifest.json",
                Instant.parse("2026-03-22T10:05:00Z"), List.of("A1"), List.of("manifest.json")
        );

        when(agentStore.findAgent("A1")).thenReturn(Optional.of(agent));
        when(agentStore.getAgents()).thenReturn(List.of(agent));
        when(provisioningService.renderAgentConfig(agent, AsteriskClientType.WEBRTC))
                .thenReturn(new AsteriskAgentConfigResponse("A1", "1001", "WEBRTC", "endpoint", "alice", "secret", "wss", "/ws", "hint"));
        when(provisioningService.generateSingleAgentPackage(agent, AsteriskClientType.WEBRTC)).thenReturn(singlePackage);
        when(provisioningService.generateAllAgentsPackage(List.of(agent), AsteriskClientType.SOFTPHONE)).thenReturn(allPackage);

        mockMvc.perform(get("/agents/A1/asterisk-config").queryParam("clientType", "webrtc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value("A1"))
                .andExpect(jsonPath("$.clientType").value("WEBRTC"));

        mockMvc.perform(post("/agents/A1/asterisk-package").queryParam("clientType", "webrtc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageId").value("pkg-1"))
                .andExpect(jsonPath("$.clientType").value("WEBRTC"));

        mockMvc.perform(post("/agents/asterisk-package"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageId").value("pkg-all"))
                .andExpect(jsonPath("$.clientType").value("SOFTPHONE"));
    }

    @Test
    void deployAndPreflightEndpointsDelegateToRunnerWithParsedFlags() throws Exception {
        AgentStore agentStore = mock(AgentStore.class);
        AsteriskProvisioningService provisioningService = mock(AsteriskProvisioningService.class);
        AsteriskDeploymentRunnerService deploymentRunnerService = mock(AsteriskDeploymentRunnerService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new AgentController(agentStore, provisioningService, deploymentRunnerService));
        Agent agent = agent("A1", "Alice", AgentStatus.AVAILABLE);
        AsteriskDeploymentPackageResponse singlePackage = new AsteriskDeploymentPackageResponse(
                "pkg-1", "SINGLE_AGENT", "WEBRTC", "pkg-dir", "pjsip.conf", "reload.sh", "manifest.json",
                Instant.parse("2026-03-22T10:00:00Z"), List.of("A1"), List.of("manifest.json")
        );
        AsteriskDeploymentPackageResponse allPackage = new AsteriskDeploymentPackageResponse(
                "pkg-all", "ALL_AGENTS", "SOFTPHONE", "pkg-all", "pjsip.conf", "reload.sh", "manifest.json",
                Instant.parse("2026-03-22T10:05:00Z"), List.of("A1"), List.of("manifest.json")
        );
        AsteriskDeploymentExecutionResponse execution = new AsteriskDeploymentExecutionResponse(
                "deploy-1", "pkg-1", "SINGLE_AGENT", "WEBRTC", false, true, "host-1", 22,
                "/remote", "/remote/pkg", "/target", List.of("scp"), List.of("manifest.json"),
                Instant.parse("2026-03-22T10:00:00Z"), Instant.parse("2026-03-22T10:01:00Z"), "ok"
        );
        AsteriskDeploymentExecutionResponse allExecution = new AsteriskDeploymentExecutionResponse(
                "deploy-all", "pkg-all", "ALL_AGENTS", "SOFTPHONE", true, false, "host-2", 22,
                "/remote", "/remote/pkg", "/target", List.of("scp"), List.of("manifest.json"),
                Instant.parse("2026-03-22T10:05:00Z"), Instant.parse("2026-03-22T10:06:00Z"), "dry run"
        );
        AsteriskDeploymentPreflightResponse preflight = new AsteriskDeploymentPreflightResponse(
                true, false, true, "host-3", 22, "deploy", "/remote", "/target",
                List.of(new AsteriskPreflightCheckResponse("ssh", true, "reachable")),
                List.of("ssh deploy@host-3"), Instant.parse("2026-03-22T10:10:00Z")
        );

        when(agentStore.findAgent("A1")).thenReturn(Optional.of(agent));
        when(agentStore.getAgents()).thenReturn(List.of(agent));
        when(provisioningService.generateSingleAgentPackage(agent, AsteriskClientType.WEBRTC)).thenReturn(singlePackage);
        when(provisioningService.generateAllAgentsPackage(List.of(agent), AsteriskClientType.SOFTPHONE)).thenReturn(allPackage);
        when(deploymentRunnerService.deploy(singlePackage, false)).thenReturn(execution);
        when(deploymentRunnerService.deploy(allPackage, true)).thenReturn(allExecution);
        when(deploymentRunnerService.preflight(AsteriskClientType.WEBRTC, false)).thenReturn(preflight);

        mockMvc.perform(post("/agents/A1/asterisk-deploy")
                        .queryParam("clientType", "webrtc")
                        .queryParam("dryRun", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deploymentJobId").value("deploy-1"))
                .andExpect(jsonPath("$.dryRun").value(false));

        mockMvc.perform(post("/agents/asterisk-deploy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deploymentJobId").value("deploy-all"))
                .andExpect(jsonPath("$.dryRun").value(true));

        mockMvc.perform(get("/agents/asterisk-preflight")
                        .queryParam("clientType", "webrtc")
                        .queryParam("performRemoteChecks", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.checks[0].checkName").value("ssh"));
    }

    @Test
    void agentSpecificAsteriskEndpointsPropagateUnknownAgentErrors() {
        AgentStore agentStore = mock(AgentStore.class);
        AsteriskProvisioningService provisioningService = mock(AsteriskProvisioningService.class);
        AsteriskDeploymentRunnerService deploymentRunnerService = mock(AsteriskDeploymentRunnerService.class);
        MockMvc mockMvc = ControllerTestSupport.mockMvc(new AgentController(agentStore, provisioningService, deploymentRunnerService));

        when(agentStore.findAgent("missing")).thenReturn(Optional.empty());

        ServletException configException = org.junit.jupiter.api.Assertions.assertThrows(
                ServletException.class,
                () -> mockMvc.perform(get("/agents/missing/asterisk-config")).andReturn()
        );
        ServletException packageException = org.junit.jupiter.api.Assertions.assertThrows(
                ServletException.class,
                () -> mockMvc.perform(post("/agents/missing/asterisk-package")).andReturn()
        );
        ServletException deployException = org.junit.jupiter.api.Assertions.assertThrows(
                ServletException.class,
                () -> mockMvc.perform(post("/agents/missing/asterisk-deploy")).andReturn()
        );

        assertEquals("Unknown agent: missing", assertInstanceOf(IllegalArgumentException.class, configException.getCause()).getMessage());
        assertEquals("Unknown agent: missing", assertInstanceOf(IllegalArgumentException.class, packageException.getCause()).getMessage());
        assertEquals("Unknown agent: missing", assertInstanceOf(IllegalArgumentException.class, deployException.getCause()).getMessage());
        verify(provisioningService, never()).renderAgentConfig(any(), any());
        verify(provisioningService, never()).generateSingleAgentPackage(any(), any());
        verify(deploymentRunnerService, never()).deploy(any(), anyBoolean());
    }

    private Agent agent(String agentId, String agentName, AgentStatus status) {
        return new Agent(agentId, agentName, "PJSIP/1001", "1001", "1001", "secret", status);
    }
}
