package com.vantage.dialer.api.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "cost_configuration")
public class CostConfigurationEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String configurationId;

    private String customerId;

    @Column(nullable = false)
    private double asteriskServerMonthlyCost;

    @Column(nullable = false)
    private double appServerMonthlyCost;

    @Column(nullable = false)
    private double ebsMonthlyCost;

    @Column(nullable = false)
    private double snapshotMonthlyCost;

    @Column(nullable = false)
    private double voiceMinuteCost;

    @Column(nullable = false)
    private double ttsUnitCost;

    @Column(nullable = false)
    private double sttMinuteCost;

    @Column(nullable = false)
    private double recordingGbCost;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public double getAsteriskServerMonthlyCost() {
        return asteriskServerMonthlyCost;
    }

    public void setAsteriskServerMonthlyCost(double asteriskServerMonthlyCost) {
        this.asteriskServerMonthlyCost = asteriskServerMonthlyCost;
    }

    public double getAppServerMonthlyCost() {
        return appServerMonthlyCost;
    }

    public void setAppServerMonthlyCost(double appServerMonthlyCost) {
        this.appServerMonthlyCost = appServerMonthlyCost;
    }

    public double getEbsMonthlyCost() {
        return ebsMonthlyCost;
    }

    public void setEbsMonthlyCost(double ebsMonthlyCost) {
        this.ebsMonthlyCost = ebsMonthlyCost;
    }

    public double getSnapshotMonthlyCost() {
        return snapshotMonthlyCost;
    }

    public void setSnapshotMonthlyCost(double snapshotMonthlyCost) {
        this.snapshotMonthlyCost = snapshotMonthlyCost;
    }

    public double getVoiceMinuteCost() {
        return voiceMinuteCost;
    }

    public void setVoiceMinuteCost(double voiceMinuteCost) {
        this.voiceMinuteCost = voiceMinuteCost;
    }

    public double getTtsUnitCost() {
        return ttsUnitCost;
    }

    public void setTtsUnitCost(double ttsUnitCost) {
        this.ttsUnitCost = ttsUnitCost;
    }

    public double getSttMinuteCost() {
        return sttMinuteCost;
    }

    public void setSttMinuteCost(double sttMinuteCost) {
        this.sttMinuteCost = sttMinuteCost;
    }

    public double getRecordingGbCost() {
        return recordingGbCost;
    }

    public void setRecordingGbCost(double recordingGbCost) {
        this.recordingGbCost = recordingGbCost;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
