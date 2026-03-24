package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.AsteriskDeploymentJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsteriskDeploymentJobRepository extends JpaRepository<AsteriskDeploymentJobEntity, String> {
    List<AsteriskDeploymentJobEntity> findAllByOrderByCreatedAtDesc();
    List<AsteriskDeploymentJobEntity> findByPackageIdOrderByCreatedAtDesc(String packageId);
}
