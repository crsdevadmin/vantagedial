package com.vantage.dialer.worker.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CallSession {

    private final String callSessionId;
    private final String provider;
    private final String campaignId;
    private final String leadId;
    private final String customerNumber;
    private final String callMode;
    private final String ivrFlowId;
    private volatile String agentId;
    private volatile String agentChannel;
    private final Map<String, String> actionOwners = new ConcurrentHashMap<>();

    private volatile String customerChannel;
    private volatile String agentLiveChannel;
    private volatile boolean customerAnswered;
    private volatile boolean agentDialRequested;
    private volatile boolean agentAnswered;
    private volatile boolean bridged;
    private volatile boolean terminated;

    public CallSession(String callSessionId,
                       String provider,
                       String campaignId,
                       String leadId,
                       String customerNumber,
                       String callMode,
                       String ivrFlowId,
                       String agentId,
                       String agentChannel) {
        this.callSessionId = callSessionId;
        this.provider = provider;
        this.campaignId = campaignId;
        this.leadId = leadId;
        this.customerNumber = customerNumber;
        this.callMode = callMode;
        this.ivrFlowId = ivrFlowId;
        this.agentId = agentId;
        this.agentChannel = agentChannel;
    }

    public String getCallSessionId() {
        return callSessionId;
    }

    public String getProvider() {
        return provider;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getLeadId() {
        return leadId;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public String getCallMode() {
        return callMode;
    }

    public String getIvrFlowId() {
        return ivrFlowId;
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

    public Map<String, String> getActionOwners() {
        return actionOwners;
    }

    public String getCustomerChannel() {
        return customerChannel;
    }

    public void setCustomerChannel(String customerChannel) {
        this.customerChannel = customerChannel;
    }

    public String getAgentLiveChannel() {
        return agentLiveChannel;
    }

    public void setAgentLiveChannel(String agentLiveChannel) {
        this.agentLiveChannel = agentLiveChannel;
    }

    public boolean isCustomerAnswered() {
        return customerAnswered;
    }

    public void setCustomerAnswered(boolean customerAnswered) {
        this.customerAnswered = customerAnswered;
    }

    public boolean isAgentDialRequested() {
        return agentDialRequested;
    }

    public void setAgentDialRequested(boolean agentDialRequested) {
        this.agentDialRequested = agentDialRequested;
    }

    public boolean isAgentAnswered() {
        return agentAnswered;
    }

    public void setAgentAnswered(boolean agentAnswered) {
        this.agentAnswered = agentAnswered;
    }

    public boolean isBridged() {
        return bridged;
    }

    public void setBridged(boolean bridged) {
        this.bridged = bridged;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }
}
