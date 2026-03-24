package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.CallEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CallEventRepository extends JpaRepository<CallEventEntity, String> {
    List<CallEventEntity> findByCallSessionIdOrderByEventTimestampAsc(String callSessionId);

    List<CallEventEntity> findByCampaignIdAndEventTimestampBetweenOrderByEventTimestampAsc(String campaignId, Instant from, Instant to);

    List<CallEventEntity> findByAgentIdAndEventTimestampBetweenOrderByEventTimestampAsc(String agentId, Instant from, Instant to);
}
