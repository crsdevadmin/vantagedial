package com.vantage.dialer.api.persistence.repository;

import com.vantage.dialer.api.persistence.model.IvrStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IvrStepRepository extends JpaRepository<IvrStepEntity, String> {
    List<IvrStepEntity> findByIvrFlowIdOrderByStepOrderAsc(String ivrFlowId);
}
