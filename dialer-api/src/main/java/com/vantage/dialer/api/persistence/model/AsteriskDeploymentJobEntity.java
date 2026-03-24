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
@Table(name = "asterisk_deployment_jobs")
public class AsteriskDeploymentJobEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String deploymentJobId;

    @Column(nullable = false)
    private String packageId;

    @Column(nullable = false)
    private String packageType;

    @Column(nullable = false)
    private String clientType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentExecutionStatus status;

    @Column(nullable = false)
    private boolean dryRun;

    private boolean deployed;
    private String host;
    private int port;
    private String remoteBaseDirectory;
    private String remotePackageDirectory;
    private String targetDirectory;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String commandsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bundledFilesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String agentIdsJson;

    private Instant generatedAt;
    private Instant executedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private String message;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public String getDeploymentJobId() {
        return deploymentJobId;
    }

    public void setDeploymentJobId(String deploymentJobId) {
        this.deploymentJobId = deploymentJobId;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public DeploymentExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(DeploymentExecutionStatus status) {
        this.status = status;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRemoteBaseDirectory() {
        return remoteBaseDirectory;
    }

    public void setRemoteBaseDirectory(String remoteBaseDirectory) {
        this.remoteBaseDirectory = remoteBaseDirectory;
    }

    public String getRemotePackageDirectory() {
        return remotePackageDirectory;
    }

    public void setRemotePackageDirectory(String remotePackageDirectory) {
        this.remotePackageDirectory = remotePackageDirectory;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public String getCommandsJson() {
        return commandsJson;
    }

    public void setCommandsJson(String commandsJson) {
        this.commandsJson = commandsJson;
    }

    public String getBundledFilesJson() {
        return bundledFilesJson;
    }

    public void setBundledFilesJson(String bundledFilesJson) {
        this.bundledFilesJson = bundledFilesJson;
    }

    public String getAgentIdsJson() {
        return agentIdsJson;
    }

    public void setAgentIdsJson(String agentIdsJson) {
        this.agentIdsJson = agentIdsJson;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
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
