package com.vantage.dialer.api.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "customer_installation_jobs")
public class CustomerInstallationJobEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String installationJobId;

    @Column(nullable = false)
    private String installationName;

    private String customerId;

    @Column(nullable = false)
    private String clientType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallationJobStatus status;

    @Column(nullable = false)
    private boolean dryRun;

    @Column(nullable = false)
    private boolean deployAfterProvision;

    @Column(nullable = false)
    private boolean performRemoteChecks;

    private int agentCount;
    private String packageId;
    private String deploymentJobId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String provisionedAgentsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String provisionedAgentIdsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String preflightJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String deploymentJson;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;
    private String message;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public String getInstallationJobId() {
        return installationJobId;
    }

    public void setInstallationJobId(String installationJobId) {
        this.installationJobId = installationJobId;
    }

    public String getInstallationName() {
        return installationName;
    }

    public void setInstallationName(String installationName) {
        this.installationName = installationName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public InstallationJobStatus getStatus() {
        return status;
    }

    public void setStatus(InstallationJobStatus status) {
        this.status = status;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDeployAfterProvision() {
        return deployAfterProvision;
    }

    public void setDeployAfterProvision(boolean deployAfterProvision) {
        this.deployAfterProvision = deployAfterProvision;
    }

    public boolean isPerformRemoteChecks() {
        return performRemoteChecks;
    }

    public void setPerformRemoteChecks(boolean performRemoteChecks) {
        this.performRemoteChecks = performRemoteChecks;
    }

    public int getAgentCount() {
        return agentCount;
    }

    public void setAgentCount(int agentCount) {
        this.agentCount = agentCount;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getDeploymentJobId() {
        return deploymentJobId;
    }

    public void setDeploymentJobId(String deploymentJobId) {
        this.deploymentJobId = deploymentJobId;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getProvisionedAgentsJson() {
        return provisionedAgentsJson;
    }

    public void setProvisionedAgentsJson(String provisionedAgentsJson) {
        this.provisionedAgentsJson = provisionedAgentsJson;
    }

    public String getProvisionedAgentIdsJson() {
        return provisionedAgentIdsJson;
    }

    public void setProvisionedAgentIdsJson(String provisionedAgentIdsJson) {
        this.provisionedAgentIdsJson = provisionedAgentIdsJson;
    }

    public String getPreflightJson() {
        return preflightJson;
    }

    public void setPreflightJson(String preflightJson) {
        this.preflightJson = preflightJson;
    }

    public String getDeploymentJson() {
        return deploymentJson;
    }

    public void setDeploymentJson(String deploymentJson) {
        this.deploymentJson = deploymentJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
