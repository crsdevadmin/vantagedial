package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.CustomerInstallationJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerInstallationJobRepository extends JpaRepository<CustomerInstallationJobEntity, String> {
    List<CustomerInstallationJobEntity> findAllByOrderByCreatedAtDesc();
    List<CustomerInstallationJobEntity> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);
}
