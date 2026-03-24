package com.vantage.dialer.api.dto;

public class CostEstimateRequest {
    private String customerId;
    private Long monthlyCallMinutes;
    private Long monthlyTtsUnits;
    private Long monthlySttMinutes;
    private Double monthlyRecordingGb;
    private Integer agentCount;
    private Integer concurrentChannels;
    private Double desiredMarginPercent;
    private Boolean useCustomerPresetDefaults = true;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Long getMonthlyCallMinutes() {
        return monthlyCallMinutes;
    }

    public void setMonthlyCallMinutes(Long monthlyCallMinutes) {
        this.monthlyCallMinutes = monthlyCallMinutes;
    }

    public Long getMonthlyTtsUnits() {
        return monthlyTtsUnits;
    }

    public void setMonthlyTtsUnits(Long monthlyTtsUnits) {
        this.monthlyTtsUnits = monthlyTtsUnits;
    }

    public Long getMonthlySttMinutes() {
        return monthlySttMinutes;
    }

    public void setMonthlySttMinutes(Long monthlySttMinutes) {
        this.monthlySttMinutes = monthlySttMinutes;
    }

    public Double getMonthlyRecordingGb() {
        return monthlyRecordingGb;
    }

    public void setMonthlyRecordingGb(Double monthlyRecordingGb) {
        this.monthlyRecordingGb = monthlyRecordingGb;
    }

    public Integer getAgentCount() {
        return agentCount;
    }

    public void setAgentCount(Integer agentCount) {
        this.agentCount = agentCount;
    }

    public Integer getConcurrentChannels() {
        return concurrentChannels;
    }

    public void setConcurrentChannels(Integer concurrentChannels) {
        this.concurrentChannels = concurrentChannels;
    }

    public Double getDesiredMarginPercent() {
        return desiredMarginPercent;
    }

    public void setDesiredMarginPercent(Double desiredMarginPercent) {
        this.desiredMarginPercent = desiredMarginPercent;
    }

    public Boolean getUseCustomerPresetDefaults() {
        return useCustomerPresetDefaults;
    }

    public void setUseCustomerPresetDefaults(Boolean useCustomerPresetDefaults) {
        this.useCustomerPresetDefaults = useCustomerPresetDefaults;
    }
}
