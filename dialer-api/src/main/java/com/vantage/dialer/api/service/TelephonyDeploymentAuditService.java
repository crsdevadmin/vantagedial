package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.TelephonyDeploymentAuditResponse;

import java.util.List;

public interface TelephonyDeploymentAuditService {

    List<TelephonyDeploymentAuditResponse> listDeploymentAudits(String packageId);

    TelephonyDeploymentAuditResponse getDeploymentAudit(String deploymentJobId);
}
