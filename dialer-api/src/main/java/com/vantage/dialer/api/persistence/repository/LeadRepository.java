package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.campaign.LeadStatus;
import com.vantage.dialer.api.persistence.model.LeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LeadRepository extends JpaRepository<LeadEntity, String> {
    List<LeadEntity> findByCampaignIdOrderByCreatedAtAsc(String campaignId);

    long countByCampaignIdAndStatusIn(String campaignId, Collection<LeadStatus> statuses);

    List<LeadEntity> findByCampaignIdAndStatusOrderByCreatedAtAsc(String campaignId, LeadStatus status);
}
