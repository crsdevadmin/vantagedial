package com.vantage.dialer.api.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "call_events", indexes = {
        @Index(name = "idx_call_events_session_time", columnList = "callSessionId,eventTimestamp"),
        @Index(name = "idx_call_events_campaign_time", columnList = "campaignId,eventTimestamp"),
        @Index(name = "idx_call_events_agent_time", columnList = "agentId,eventTimestamp")
})
public class CallEventEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String eventId;

    @Column(nullable = false)
    private String callSessionId;

    private String callLegId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant eventTimestamp;

    @Column(nullable = false)
    private String provider;

    private String providerCallId;
    private String legType;
    private String campaignId;
    private String leadId;
    private String agentId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getCallSessionId() {
        return callSessionId;
    }

    public void setCallSessionId(String callSessionId) {
        this.callSessionId = callSessionId;
    }

    public String getCallLegId() {
        return callLegId;
    }

    public void setCallLegId(String callLegId) {
        this.callLegId = callLegId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderCallId() {
        return providerCallId;
    }

    public void setProviderCallId(String providerCallId) {
        this.providerCallId = providerCallId;
    }

    public String getLegType() {
        return legType;
    }

    public void setLegType(String legType) {
        this.legType = legType;
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

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
