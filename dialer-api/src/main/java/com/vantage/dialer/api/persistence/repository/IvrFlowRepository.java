package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.IvrFlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IvrFlowRepository extends JpaRepository<IvrFlowEntity, String> {
}
