package com.vantage.dialer.api.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "call_sessions", indexes = {
        @Index(name = "idx_call_sessions_campaign_id", columnList = "campaignId"),
        @Index(name = "idx_call_sessions_agent_id", columnList = "agentId")
})
public class CallSessionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String callSessionId;

    private String campaignId;
    private String leadId;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String customerNumber;

    private String agentId;
    private String agentChannel;
    private String ivrFlowId;

    @Column(nullable = false)
    private String callMode;

    @Column(nullable = false)
    private String status;

    private String lastEventType;
    private Instant lastEventAt;
    private String callDirection;
    private String uiCallStatus;
    private String operatorDisposition;
    private String operatorPriority;
    private Instant followUpAt;

    @Column(length = 2000)
    private String operatorNotes;

    private Instant wrapUpUpdatedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getCallSessionId() {
        return callSessionId;
    }

    public void setCallSessionId(String callSessionId) {
        this.callSessionId = callSessionId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentChannel() {
        return agentChannel;
    }

    public void setAgentChannel(String agentChannel) {
        this.agentChannel = agentChannel;
    }

    public String getIvrFlowId() {
        return ivrFlowId;
    }

    public void setIvrFlowId(String ivrFlowId) {
        this.ivrFlowId = ivrFlowId;
    }

    public String getCallMode() {
        return callMode;
    }

    public void setCallMode(String callMode) {
        this.callMode = callMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastEventType() {
        return lastEventType;
    }

    public void setLastEventType(String lastEventType) {
        this.lastEventType = lastEventType;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(Instant lastEventAt) {
        this.lastEventAt = lastEventAt;
    }

    public String getCallDirection() {
        return callDirection;
    }

    public void setCallDirection(String callDirection) {
        this.callDirection = callDirection;
    }

    public String getUiCallStatus() {
        return uiCallStatus;
    }

    public void setUiCallStatus(String uiCallStatus) {
        this.uiCallStatus = uiCallStatus;
    }

    public String getOperatorDisposition() {
        return operatorDisposition;
    }

    public void setOperatorDisposition(String operatorDisposition) {
        this.operatorDisposition = operatorDisposition;
    }

    public String getOperatorPriority() {
        return operatorPriority;
    }

    public void setOperatorPriority(String operatorPriority) {
        this.operatorPriority = operatorPriority;
    }

    public Instant getFollowUpAt() {
        return followUpAt;
    }

    public void setFollowUpAt(Instant followUpAt) {
        this.followUpAt = followUpAt;
    }

    public String getOperatorNotes() {
        return operatorNotes;
    }

    public void setOperatorNotes(String operatorNotes) {
        this.operatorNotes = operatorNotes;
    }

    public Instant getWrapUpUpdatedAt() {
        return wrapUpUpdatedAt;
    }

    public void setWrapUpUpdatedAt(Instant wrapUpUpdatedAt) {
        this.wrapUpUpdatedAt = wrapUpUpdatedAt;
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
