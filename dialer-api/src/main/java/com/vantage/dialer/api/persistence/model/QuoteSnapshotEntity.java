package com.vantage.dialer.api.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "quote_snapshots")
public class QuoteSnapshotEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String quoteSnapshotId;

    @Column(nullable = false)
    private String installationJobId;

    private String customerId;
    private String configurationId;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String requestJson;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String summaryJson;

    private String filePath;

    @Column(nullable = false)
    private Instant createdAt;

    public String getQuoteSnapshotId() {
        return quoteSnapshotId;
    }

    public void setQuoteSnapshotId(String quoteSnapshotId) {
        this.quoteSnapshotId = quoteSnapshotId;
    }

    public String getInstallationJobId() {
        return installationJobId;
    }

    public void setInstallationJobId(String installationJobId) {
        this.installationJobId = installationJobId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public void setSummaryJson(String summaryJson) {
        this.summaryJson = summaryJson;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
