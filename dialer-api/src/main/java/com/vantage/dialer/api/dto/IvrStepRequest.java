package com.vantage.dialer.api.dto;

import java.util.Map;

public class IvrStepRequest {
    private int stepOrder;
    private String stepType;
    private String promptSourceType;
    private String promptValue;
    private Map<String, String> dtmfMappings;
    private String targetAgentChannel;
    private String fallbackAction;

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getPromptSourceType() {
        return promptSourceType;
    }

    public void setPromptSourceType(String promptSourceType) {
        this.promptSourceType = promptSourceType;
    }

    public String getPromptValue() {
        return promptValue;
    }

    public void setPromptValue(String promptValue) {
        this.promptValue = promptValue;
    }

    public Map<String, String> getDtmfMappings() {
        return dtmfMappings;
    }

    public void setDtmfMappings(Map<String, String> dtmfMappings) {
        this.dtmfMappings = dtmfMappings;
    }

    public String getTargetAgentChannel() {
        return targetAgentChannel;
    }

    public void setTargetAgentChannel(String targetAgentChannel) {
        this.targetAgentChannel = targetAgentChannel;
    }

    public String getFallbackAction() {
        return fallbackAction;
    }

    public void setFallbackAction(String fallbackAction) {
        this.fallbackAction = fallbackAction;
    }
}
