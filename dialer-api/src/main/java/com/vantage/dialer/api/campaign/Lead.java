package com.vantage.dialer.api.campaign;

public class Lead {
    private final String leadId;
    private final String campaignId;
    private final String customerNumber;
    private LeadStatus status;
    private int attempts;

    public Lead(String leadId, String campaignId, String customerNumber) {
        this.leadId = leadId;
        this.campaignId = campaignId;
        this.customerNumber = customerNumber;
        this.status = LeadStatus.NEW;
        this.attempts = 0;
    }

    public String getLeadId() {
        return leadId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
