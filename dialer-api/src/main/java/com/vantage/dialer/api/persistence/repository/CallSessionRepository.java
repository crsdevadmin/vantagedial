package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.CallSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CallSessionRepository extends JpaRepository<CallSessionEntity, String> {
    List<CallSessionEntity> findByCampaignIdOrderByCreatedAtDesc(String campaignId);

    List<CallSessionEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    List<CallSessionEntity> findByCampaignIdAndCreatedAtBetweenOrderByCreatedAtDesc(String campaignId, Instant from, Instant to);
}
