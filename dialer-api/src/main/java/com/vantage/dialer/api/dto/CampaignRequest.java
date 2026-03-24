package com.vantage.dialer.api.dto;

public class CampaignRequest {
    private String name;
    private String provider;
    private String dialMode;
    private String ivrFlowId;
    private Integer maxConcurrentCalls;
    private Integer callsPerSecond;
    private Double predictiveRatio;

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

    public String getDialMode() {
        return dialMode;
    }

    public void setDialMode(String dialMode) {
        this.dialMode = dialMode;
    }

    public String getIvrFlowId() {
        return ivrFlowId;
    }

    public void setIvrFlowId(String ivrFlowId) {
        this.ivrFlowId = ivrFlowId;
    }

    public Integer getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    public void setMaxConcurrentCalls(Integer maxConcurrentCalls) {
        this.maxConcurrentCalls = maxConcurrentCalls;
    }

    public Integer getCallsPerSecond() {
        return callsPerSecond;
    }

    public void setCallsPerSecond(Integer callsPerSecond) {
        this.callsPerSecond = callsPerSecond;
    }

    public Double getPredictiveRatio() {
        return predictiveRatio;
    }

    public void setPredictiveRatio(Double predictiveRatio) {
        this.predictiveRatio = predictiveRatio;
    }
}
