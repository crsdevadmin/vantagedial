package com.vantage.dialer.api.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "ivr_steps", indexes = {
        @Index(name = "idx_ivr_steps_flow_order", columnList = "ivrFlowId,stepOrder")
})
public class IvrStepEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String ivrStepId;

    @Column(nullable = false)
    private String ivrFlowId;

    @Column(nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IvrStepType stepType;

    @Enumerated(EnumType.STRING)
    private PromptSourceType promptSourceType;

    private String promptValue;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String dtmfMappingsJson;

    private String targetAgentChannel;
    private String fallbackAction;

    public String getIvrStepId() {
        return ivrStepId;
    }

    public void setIvrStepId(String ivrStepId) {
        this.ivrStepId = ivrStepId;
    }

    public String getIvrFlowId() {
        return ivrFlowId;
    }

    public void setIvrFlowId(String ivrFlowId) {
        this.ivrFlowId = ivrFlowId;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public IvrStepType getStepType() {
        return stepType;
    }

    public void setStepType(IvrStepType stepType) {
        this.stepType = stepType;
    }

    public PromptSourceType getPromptSourceType() {
        return promptSourceType;
    }

    public void setPromptSourceType(PromptSourceType promptSourceType) {
        this.promptSourceType = promptSourceType;
    }

    public String getPromptValue() {
        return promptValue;
    }

    public void setPromptValue(String promptValue) {
        this.promptValue = promptValue;
    }

    public String getDtmfMappingsJson() {
        return dtmfMappingsJson;
    }

    public void setDtmfMappingsJson(String dtmfMappingsJson) {
        this.dtmfMappingsJson = dtmfMappingsJson;
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
