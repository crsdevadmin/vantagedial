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
@Table(name = "report_export_jobs")
public class ReportExportJobEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String exportJobId;

    @Column(nullable = false)
    private String exportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExportJobStatus status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestJson;

    private String filePath;
    private long rowCount;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;
    private String errorMessage;

    public String getExportJobId() {
        return exportJobId;
    }

    public void setExportJobId(String exportJobId) {
        this.exportJobId = exportJobId;
    }

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }

    public ExportJobStatus getStatus() {
        return status;
    }

    public void setStatus(ExportJobStatus status) {
        this.status = status;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long rowCount) {
        this.rowCount = rowCount;
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
