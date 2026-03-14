package com.vantage.dialer.api.agent;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AgentStore {

    private final List<Agent> agents = new ArrayList<>();

    @PostConstruct
    public void init() {
        agents.add(new Agent("A1", "Agent 1", "PJSIP/1001", AgentStatus.AVAILABLE));
        agents.add(new Agent("A2", "Agent 2", "PJSIP/1002", AgentStatus.AVAILABLE));
        agents.add(new Agent("A3", "Agent 3", "PJSIP/1003", AgentStatus.AVAILABLE));
    }

    public synchronized Optional<Agent> acquireAvailableAgent() {
        return agents.stream()
                .filter(a -> a.getStatus() == AgentStatus.AVAILABLE)
                .findFirst()
                .map(agent -> {
                    agent.setStatus(AgentStatus.BUSY);
                    return agent;
                });
    }

    public synchronized Optional<Agent> acquireAgent(String agentId) {
        return agents.stream()
                .filter(a -> a.getAgentId().equals(agentId))
                .filter(a -> a.getStatus() == AgentStatus.AVAILABLE)
                .findFirst()
                .map(agent -> {
                    agent.setStatus(AgentStatus.BUSY);
                    return agent;
                });
    }

    public synchronized Optional<Agent> findAgent(String agentId) {
        return agents.stream()
                .filter(a -> a.getAgentId().equals(agentId))
                .findFirst();
    }

    public synchronized void releaseAgent(String agentId) {
        agents.stream()
                .filter(a -> a.getAgentId().equals(agentId))
                .findFirst()
                .ifPresent(a -> a.setStatus(AgentStatus.AVAILABLE));
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public long countAvailable() {
        return agents.stream().filter(a -> a.getStatus() == AgentStatus.AVAILABLE).count();
    }
    
}
