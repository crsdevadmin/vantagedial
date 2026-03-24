package com.vantage.dialer.api.agent;

import com.vantage.dialer.api.dto.AgentProvisionRequest;
import com.vantage.dialer.api.persistence.model.AgentEntity;
import com.vantage.dialer.api.persistence.repository.AgentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class AgentStore {

    private final AgentRepository agentRepository;

    public AgentStore(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @PostConstruct
    public void init() {
        seedAgent("A1", "Agent 1", "1001", "1001", "StrongPassword1001", "PJSIP/1001");
        seedAgent("A2", "Agent 2", "1002", "1002", "StrongPassword1002", "PJSIP/1002");
        seedAgent("A3", "Agent 3", "1003", "1003", "StrongPassword1003", "PJSIP/1003");
    }

    @Transactional
    public synchronized Optional<Agent> acquireAvailableAgent() {
        return agentRepository.findByStatusOrderByAgentIdAsc(AgentStatus.AVAILABLE).stream()
                .findFirst()
                .map(agent -> {
                    agent.setStatus(AgentStatus.BUSY);
                    return toDomain(agentRepository.save(agent));
                });
    }

    @Transactional
    public synchronized Optional<Agent> acquireAgent(String agentId) {
        return agentRepository.findById(agentId)
                .filter(agent -> agent.getStatus() == AgentStatus.AVAILABLE)
                .map(agent -> {
                    agent.setStatus(AgentStatus.BUSY);
                    return toDomain(agentRepository.save(agent));
                });
    }

    @Transactional(readOnly = true)
    public synchronized Optional<Agent> findAgent(String agentId) {
        return agentRepository.findById(agentId).map(this::toDomain);
    }

    @Transactional
    public synchronized void releaseAgent(String agentId) {
        agentRepository.findById(agentId).ifPresent(agent -> {
            agent.setStatus(AgentStatus.AVAILABLE);
            agentRepository.save(agent);
        });
    }

    @Transactional
    public synchronized void markBusy(String agentId) {
        agentRepository.findById(agentId).ifPresent(agent -> {
            agent.setStatus(AgentStatus.BUSY);
            agentRepository.save(agent);
        });
    }

    @Transactional(readOnly = true)
    public List<Agent> getAgents() {
        return agentRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Transactional
    public Agent createOrUpdate(AgentProvisionRequest request) {
        AgentEntity agent = request.getAgentId() != null && agentRepository.existsById(request.getAgentId())
                ? agentRepository.findById(request.getAgentId()).orElseThrow()
                : new AgentEntity();

        String extensionNumber = normalize(request.getExtensionNumber());
        String sipUsername = normalize(request.getSipUsername());
        if (sipUsername == null) {
            sipUsername = extensionNumber;
        }

        if (agent.getAgentId() == null) {
            agent.setAgentId(normalize(request.getAgentId()));
        }
        agent.setAgentName(normalize(request.getAgentName()));
        agent.setExtensionNumber(extensionNumber);
        agent.setSipUsername(sipUsername);
        agent.setSipPassword(normalize(request.getSipPassword()));
        agent.setChannel(normalize(request.getChannel()) == null ? "PJSIP/" + extensionNumber : normalize(request.getChannel()));
        if (agent.getStatus() == null) {
            agent.setStatus(AgentStatus.AVAILABLE);
        }
        return toDomain(agentRepository.save(agent));
    }

    @Transactional
    public void delete(String agentId) {
        agentRepository.deleteById(agentId);
    }

    @Transactional(readOnly = true)
    public long countAvailable() {
        return agentRepository.findByStatusOrderByAgentIdAsc(AgentStatus.AVAILABLE).size();
    }

    private void seedAgent(String agentId,
                           String name,
                           String extensionNumber,
                           String sipUsername,
                           String sipPassword,
                           String channel) {
        if (agentRepository.existsById(agentId)) {
            return;
        }
        AgentEntity agent = new AgentEntity();
        agent.setAgentId(agentId);
        agent.setAgentName(name);
        agent.setExtensionNumber(extensionNumber);
        agent.setSipUsername(sipUsername);
        agent.setSipPassword(sipPassword);
        agent.setChannel(channel);
        agent.setStatus(AgentStatus.AVAILABLE);
        agentRepository.save(agent);
    }

    private Agent toDomain(AgentEntity entity) {
        return new Agent(
                entity.getAgentId(),
                entity.getAgentName(),
                entity.getChannel(),
                entity.getExtensionNumber(),
                entity.getSipUsername(),
                entity.getSipPassword(),
                entity.getStatus()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
