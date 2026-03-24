package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.ReportExportJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportExportJobRepository extends JpaRepository<ReportExportJobEntity, String> {
}
