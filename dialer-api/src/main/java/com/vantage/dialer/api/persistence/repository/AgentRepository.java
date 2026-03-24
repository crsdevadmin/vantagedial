package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.persistence.model.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentRepository extends JpaRepository<AgentEntity, String> {
    List<AgentEntity> findByStatusOrderByAgentIdAsc(AgentStatus status);
}
