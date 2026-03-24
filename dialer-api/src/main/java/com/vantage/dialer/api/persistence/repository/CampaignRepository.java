package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<CampaignEntity, String> {
}
