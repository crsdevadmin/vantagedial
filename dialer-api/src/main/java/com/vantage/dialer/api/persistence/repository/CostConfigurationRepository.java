package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.CostConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostConfigurationRepository extends JpaRepository<CostConfigurationEntity, String> {
    java.util.Optional<CostConfigurationEntity> findByCustomerId(String customerId);
}
