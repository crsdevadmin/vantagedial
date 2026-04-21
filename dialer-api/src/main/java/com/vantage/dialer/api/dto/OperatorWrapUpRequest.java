package com.vantage.dialer.api.dto;

import java.time.Instant;

public class OperatorWrapUpRequest {
    private String campaignId;
    private String provider;
    private String customerNumber;
    private String agentId;
    private String callMode;
    private String callDirection;
    private String callStatus;
    private String disposition;
    private String notes;
    private String priority;
    private Instant followUpAt;

    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getCallMode() { return callMode; }
    public void setCallMode(String callMode) { this.callMode = callMode; }
    public String getCallDirection() { return callDirection; }
    public void setCallDirection(String callDirection) { this.callDirection = callDirection; }
    public String getCallStatus() { return callStatus; }
    public void setCallStatus(String callStatus) { this.callStatus = callStatus; }
    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Instant getFollowUpAt() { return followUpAt; }
    public void setFollowUpAt(Instant followUpAt) { this.followUpAt = followUpAt; }
}
