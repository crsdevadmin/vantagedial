package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.CustomerConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerConfigurationRepository extends JpaRepository<CustomerConfigurationEntity, String> {
}
