package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.dto.AsteriskClientType;
import com.vantage.dialer.api.dto.AgentProvisionRequest;
import com.vantage.dialer.api.dto.AsteriskAgentConfigResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentExecutionResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPreflightResponse;
import com.vantage.dialer.api.service.AsteriskDeploymentRunnerService;
import com.vantage.dialer.api.service.AsteriskProvisioningService;
import com.vantage.dialer.api.agent.AgentStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private final AgentStore agentStore;
    private final AsteriskProvisioningService provisioningService;
    private final AsteriskDeploymentRunnerService deploymentRunnerService;

    public AgentController(AgentStore agentStore,
                           AsteriskProvisioningService provisioningService,
                           AsteriskDeploymentRunnerService deploymentRunnerService) {
        this.agentStore = agentStore;
        this.provisioningService = provisioningService;
        this.deploymentRunnerService = deploymentRunnerService;
    }

    // See all agents
    @GetMapping
    public List<Agent> list() {
        return agentStore.getAgents();
    }

    @PostMapping
    public Agent createAgent(@RequestBody AgentProvisionRequest request) {
        return agentStore.createOrUpdate(request);
    }

    @PutMapping("/{agentId}")
    public Agent updateAgent(@PathVariable String agentId, @RequestBody AgentProvisionRequest request) {
        request.setAgentId(agentId);
        return agentStore.createOrUpdate(request);
    }

    @DeleteMapping("/{agentId}")
    public String deleteAgent(@PathVariable String agentId) {
        agentStore.delete(agentId);
        return "Agent " + agentId + " deleted";
    }

    // Make agent AVAILABLE
    @PostMapping("/{agentId}/available")
    public String makeAvailable(@PathVariable String agentId) {
        agentStore.releaseAgent(agentId);
        return "Agent " + agentId + " set to AVAILABLE";
    }

    // Make agent BUSY manually
    @PostMapping("/{agentId}/busy")
    public String makeBusy(@PathVariable String agentId) {
        agentStore.markBusy(agentId);
        return "Agent " + agentId + " set to BUSY";
    }

    @GetMapping("/{agentId}/asterisk-config")
    public AsteriskAgentConfigResponse getAsteriskConfig(@PathVariable String agentId,
                                                         @RequestParam(defaultValue = "SOFTPHONE") String clientType) {
        Agent agent = agentStore.findAgent(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown agent: " + agentId));
        return provisioningService.renderAgentConfig(agent, AsteriskClientType.from(clientType));
    }

    @PostMapping("/{agentId}/asterisk-package")
    public AsteriskDeploymentPackageResponse generateAsteriskPackage(@PathVariable String agentId,
                                                                    @RequestParam(defaultValue = "SOFTPHONE") String clientType) {
        Agent agent = agentStore.findAgent(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown agent: " + agentId));
        return provisioningService.generateSingleAgentPackage(agent, AsteriskClientType.from(clientType));
    }

    @PostMapping("/asterisk-package")
    public AsteriskDeploymentPackageResponse generateAllAgentsAsteriskPackage(@RequestParam(defaultValue = "SOFTPHONE") String clientType) {
        return provisioningService.generateAllAgentsPackage(agentStore.getAgents(), AsteriskClientType.from(clientType));
    }

    @PostMapping("/{agentId}/asterisk-deploy")
    public AsteriskDeploymentExecutionResponse deploySingleAgentPackage(@PathVariable String agentId,
                                                                        @RequestParam(defaultValue = "SOFTPHONE") String clientType,
                                                                        @RequestParam(defaultValue = "true") boolean dryRun) {
        Agent agent = agentStore.findAgent(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown agent: " + agentId));
        AsteriskDeploymentPackageResponse deploymentPackage =
                provisioningService.generateSingleAgentPackage(agent, AsteriskClientType.from(clientType));
        return deploymentRunnerService.deploy(deploymentPackage, dryRun);
    }

    @PostMapping("/asterisk-deploy")
    public AsteriskDeploymentExecutionResponse deployAllAgentsPackage(@RequestParam(defaultValue = "SOFTPHONE") String clientType,
                                                                      @RequestParam(defaultValue = "true") boolean dryRun) {
        AsteriskDeploymentPackageResponse deploymentPackage =
                provisioningService.generateAllAgentsPackage(agentStore.getAgents(), AsteriskClientType.from(clientType));
        return deploymentRunnerService.deploy(deploymentPackage, dryRun);
    }

    @GetMapping("/asterisk-preflight")
    public AsteriskDeploymentPreflightResponse preflightDeployment(@RequestParam(defaultValue = "SOFTPHONE") String clientType,
                                                                   @RequestParam(defaultValue = "true") boolean performRemoteChecks) {
        return deploymentRunnerService.preflight(AsteriskClientType.from(clientType), performRemoteChecks);
    }

}
