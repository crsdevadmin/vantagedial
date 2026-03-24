package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.QuoteSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuoteSnapshotRepository extends JpaRepository<QuoteSnapshotEntity, String> {
    List<QuoteSnapshotEntity> findByInstallationJobIdOrderByCreatedAtDesc(String installationJobId);
    List<QuoteSnapshotEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<QuoteSnapshotEntity> findByInstallationJobIdAndCustomerIdOrderByCreatedAtDesc(String installationJobId, String customerId);
    List<QuoteSnapshotEntity> findAllByOrderByCreatedAtDesc();
    Optional<QuoteSnapshotEntity> findFirstByInstallationJobIdAndCreatedAtBeforeOrderByCreatedAtDesc(String installationJobId, Instant createdAt);
    Optional<QuoteSnapshotEntity> findFirstByInstallationJobIdAndCustomerIdAndCreatedAtBeforeOrderByCreatedAtDesc(String installationJobId, String customerId, Instant createdAt);
}
