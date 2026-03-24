package com.vantage.dialer.api.dto;

public class CostConfigurationRequest {
    private String customerId;
    private double asteriskServerMonthlyCost;
    private double appServerMonthlyCost;
    private double ebsMonthlyCost;
    private double snapshotMonthlyCost;
    private double voiceMinuteCost;
    private double ttsUnitCost;
    private double sttMinuteCost;
    private double recordingGbCost;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public double getAsteriskServerMonthlyCost() { return asteriskServerMonthlyCost; }
    public void setAsteriskServerMonthlyCost(double asteriskServerMonthlyCost) { this.asteriskServerMonthlyCost = asteriskServerMonthlyCost; }
    public double getAppServerMonthlyCost() { return appServerMonthlyCost; }
    public void setAppServerMonthlyCost(double appServerMonthlyCost) { this.appServerMonthlyCost = appServerMonthlyCost; }
    public double getEbsMonthlyCost() { return ebsMonthlyCost; }
    public void setEbsMonthlyCost(double ebsMonthlyCost) { this.ebsMonthlyCost = ebsMonthlyCost; }
    public double getSnapshotMonthlyCost() { return snapshotMonthlyCost; }
    public void setSnapshotMonthlyCost(double snapshotMonthlyCost) { this.snapshotMonthlyCost = snapshotMonthlyCost; }
    public double getVoiceMinuteCost() { return voiceMinuteCost; }
    public void setVoiceMinuteCost(double voiceMinuteCost) { this.voiceMinuteCost = voiceMinuteCost; }
    public double getTtsUnitCost() { return ttsUnitCost; }
    public void setTtsUnitCost(double ttsUnitCost) { this.ttsUnitCost = ttsUnitCost; }
    public double getSttMinuteCost() { return sttMinuteCost; }
    public void setSttMinuteCost(double sttMinuteCost) { this.sttMinuteCost = sttMinuteCost; }
    public double getRecordingGbCost() { return recordingGbCost; }
    public void setRecordingGbCost(double recordingGbCost) { this.recordingGbCost = recordingGbCost; }
}
