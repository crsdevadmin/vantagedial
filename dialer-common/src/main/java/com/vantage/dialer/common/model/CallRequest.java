package com.vantage.dialer.common.model;

public class CallRequest {

    private String phoneNumber;
    private String customerNumber;
    private String campaignId;
    private String agentId;
    private String agentChannel;
    private String provider;
    private String ivrFlowId;
    private String callMode;

    public CallRequest() {
    }

    public CallRequest(String phoneNumber, String campaignId, String agentId) {
        this.phoneNumber = phoneNumber;
        this.customerNumber = phoneNumber;
        this.campaignId = campaignId;
        this.agentId = agentId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        if (this.customerNumber == null) {
            this.customerNumber = phoneNumber;
        }
    }

    public String getCustomerNumber() {
        return customerNumber != null ? customerNumber : phoneNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
        if (this.phoneNumber == null) {
            this.phoneNumber = customerNumber;
        }
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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
}
