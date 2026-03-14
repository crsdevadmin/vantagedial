package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.agent.AgentStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private final AgentStore agentStore;

    public AgentController(AgentStore agentStore) {
        this.agentStore = agentStore;
    }

    // See all agents
    @GetMapping
    public List<Agent> list() {
        return agentStore.getAgents();
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
        agentStore.getAgents().stream()
                .filter(a -> a.getAgentId().equals(agentId))
                .findFirst()
                .ifPresent(a -> a.setStatus(AgentStatus.BUSY));

        return "Agent " + agentId + " set to BUSY";
    }

}