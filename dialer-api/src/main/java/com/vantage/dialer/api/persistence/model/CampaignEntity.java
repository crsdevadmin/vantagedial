package com.vantage.dialer.api.persistence.model;

import com.vantage.dialer.api.campaign.DialMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "campaigns")
public class CampaignEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String campaignId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DialMode dialMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @Column(nullable = false)
    private int maxConcurrentCalls;

    @Column(nullable = false)
    private int callsPerSecond;

    @Column(nullable = false)
    private double predictiveRatio;

    private String ivrFlowId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public DialMode getDialMode() {
        return dialMode;
    }

    public void setDialMode(DialMode dialMode) {
        this.dialMode = dialMode;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    public void setMaxConcurrentCalls(int maxConcurrentCalls) {
        this.maxConcurrentCalls = maxConcurrentCalls;
    }

    public int getCallsPerSecond() {
        return callsPerSecond;
    }

    public void setCallsPerSecond(int callsPerSecond) {
        this.callsPerSecond = callsPerSecond;
    }

    public double getPredictiveRatio() {
        return predictiveRatio;
    }

    public void setPredictiveRatio(double predictiveRatio) {
        this.predictiveRatio = predictiveRatio;
    }

    public String getIvrFlowId() {
        return ivrFlowId;
    }

    public void setIvrFlowId(String ivrFlowId) {
        this.ivrFlowId = ivrFlowId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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
