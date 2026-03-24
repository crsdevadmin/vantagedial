package com.vantage.dialer.api.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "customer_configuration")
public class CustomerConfigurationEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String customerId;

    @Column(nullable = false)
    private String customerName;

    private String serverAHost;
    private String serverAPrivateIp;
    private String serverBHost;
    private String asteriskDeployUser;
    private String asteriskDeployPrivateKeyPath;
    private String asteriskDeployTargetDirectory;
    private String amiUsername;
    private String amiEndpoint;
    private String dialPrefix;
    private String sipDomain;
    private String webSocketUrl;
    private String apiBaseUrl;
    private String defaultAgentUiMode;
    private String defaultSupervisorUiMode;
    private String brandDisplayName;
    private String brandLogoUrl;
    private String brandPrimaryColor;
    private String brandAccentColor;
    private String proposalPreset;
    private String proposalTemplate;
    private String proposalTitle;
    private String proposalSubtitle;
    private Boolean proposalIncludeAgentOutbound;
    private Boolean proposalIncludeIvr;
    private Boolean proposalIncludeReporting;
    private Boolean proposalIncludeWebRtc;
    private Boolean proposalIncludeProvisioning;
    private Boolean proposalIncludePricingBreakdown;
    private Long defaultMonthlyCallMinutes;
    private Long defaultMonthlyTtsUnits;
    private Long defaultMonthlySttMinutes;
    private Double defaultMonthlyRecordingGb;
    private Integer defaultAgentCount;
    private Integer defaultConcurrentChannels;
    private Double defaultMarginPercent;

    @Column(columnDefinition = "TEXT")
    private String proposalTerms;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getServerAHost() { return serverAHost; }
    public void setServerAHost(String serverAHost) { this.serverAHost = serverAHost; }
    public String getServerAPrivateIp() { return serverAPrivateIp; }
    public void setServerAPrivateIp(String serverAPrivateIp) { this.serverAPrivateIp = serverAPrivateIp; }
    public String getServerBHost() { return serverBHost; }
    public void setServerBHost(String serverBHost) { this.serverBHost = serverBHost; }
    public String getAsteriskDeployUser() { return asteriskDeployUser; }
    public void setAsteriskDeployUser(String asteriskDeployUser) { this.asteriskDeployUser = asteriskDeployUser; }
    public String getAsteriskDeployPrivateKeyPath() { return asteriskDeployPrivateKeyPath; }
    public void setAsteriskDeployPrivateKeyPath(String asteriskDeployPrivateKeyPath) { this.asteriskDeployPrivateKeyPath = asteriskDeployPrivateKeyPath; }
    public String getAsteriskDeployTargetDirectory() { return asteriskDeployTargetDirectory; }
    public void setAsteriskDeployTargetDirectory(String asteriskDeployTargetDirectory) { this.asteriskDeployTargetDirectory = asteriskDeployTargetDirectory; }
    public String getAmiUsername() { return amiUsername; }
    public void setAmiUsername(String amiUsername) { this.amiUsername = amiUsername; }
    public String getAmiEndpoint() { return amiEndpoint; }
    public void setAmiEndpoint(String amiEndpoint) { this.amiEndpoint = amiEndpoint; }
    public String getDialPrefix() { return dialPrefix; }
    public void setDialPrefix(String dialPrefix) { this.dialPrefix = dialPrefix; }
    public String getSipDomain() { return sipDomain; }
    public void setSipDomain(String sipDomain) { this.sipDomain = sipDomain; }
    public String getWebSocketUrl() { return webSocketUrl; }
    public void setWebSocketUrl(String webSocketUrl) { this.webSocketUrl = webSocketUrl; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getDefaultAgentUiMode() { return defaultAgentUiMode; }
    public void setDefaultAgentUiMode(String defaultAgentUiMode) { this.defaultAgentUiMode = defaultAgentUiMode; }
    public String getDefaultSupervisorUiMode() { return defaultSupervisorUiMode; }
    public void setDefaultSupervisorUiMode(String defaultSupervisorUiMode) { this.defaultSupervisorUiMode = defaultSupervisorUiMode; }
    public String getBrandDisplayName() { return brandDisplayName; }
    public void setBrandDisplayName(String brandDisplayName) { this.brandDisplayName = brandDisplayName; }
    public String getBrandLogoUrl() { return brandLogoUrl; }
    public void setBrandLogoUrl(String brandLogoUrl) { this.brandLogoUrl = brandLogoUrl; }
    public String getBrandPrimaryColor() { return brandPrimaryColor; }
    public void setBrandPrimaryColor(String brandPrimaryColor) { this.brandPrimaryColor = brandPrimaryColor; }
    public String getBrandAccentColor() { return brandAccentColor; }
    public void setBrandAccentColor(String brandAccentColor) { this.brandAccentColor = brandAccentColor; }
    public String getProposalPreset() { return proposalPreset; }
    public void setProposalPreset(String proposalPreset) { this.proposalPreset = proposalPreset; }
    public String getProposalTemplate() { return proposalTemplate; }
    public void setProposalTemplate(String proposalTemplate) { this.proposalTemplate = proposalTemplate; }
    public String getProposalTitle() { return proposalTitle; }
    public void setProposalTitle(String proposalTitle) { this.proposalTitle = proposalTitle; }
    public String getProposalSubtitle() { return proposalSubtitle; }
    public void setProposalSubtitle(String proposalSubtitle) { this.proposalSubtitle = proposalSubtitle; }
    public Boolean getProposalIncludeAgentOutbound() { return proposalIncludeAgentOutbound; }
    public void setProposalIncludeAgentOutbound(Boolean proposalIncludeAgentOutbound) { this.proposalIncludeAgentOutbound = proposalIncludeAgentOutbound; }
    public Boolean getProposalIncludeIvr() { return proposalIncludeIvr; }
    public void setProposalIncludeIvr(Boolean proposalIncludeIvr) { this.proposalIncludeIvr = proposalIncludeIvr; }
    public Boolean getProposalIncludeReporting() { return proposalIncludeReporting; }
    public void setProposalIncludeReporting(Boolean proposalIncludeReporting) { this.proposalIncludeReporting = proposalIncludeReporting; }
    public Boolean getProposalIncludeWebRtc() { return proposalIncludeWebRtc; }
    public void setProposalIncludeWebRtc(Boolean proposalIncludeWebRtc) { this.proposalIncludeWebRtc = proposalIncludeWebRtc; }
    public Boolean getProposalIncludeProvisioning() { return proposalIncludeProvisioning; }
    public void setProposalIncludeProvisioning(Boolean proposalIncludeProvisioning) { this.proposalIncludeProvisioning = proposalIncludeProvisioning; }
    public Boolean getProposalIncludePricingBreakdown() { return proposalIncludePricingBreakdown; }
    public void setProposalIncludePricingBreakdown(Boolean proposalIncludePricingBreakdown) { this.proposalIncludePricingBreakdown = proposalIncludePricingBreakdown; }
    public Long getDefaultMonthlyCallMinutes() { return defaultMonthlyCallMinutes; }
    public void setDefaultMonthlyCallMinutes(Long defaultMonthlyCallMinutes) { this.defaultMonthlyCallMinutes = defaultMonthlyCallMinutes; }
    public Long getDefaultMonthlyTtsUnits() { return defaultMonthlyTtsUnits; }
    public void setDefaultMonthlyTtsUnits(Long defaultMonthlyTtsUnits) { this.defaultMonthlyTtsUnits = defaultMonthlyTtsUnits; }
    public Long getDefaultMonthlySttMinutes() { return defaultMonthlySttMinutes; }
    public void setDefaultMonthlySttMinutes(Long defaultMonthlySttMinutes) { this.defaultMonthlySttMinutes = defaultMonthlySttMinutes; }
    public Double getDefaultMonthlyRecordingGb() { return defaultMonthlyRecordingGb; }
    public void setDefaultMonthlyRecordingGb(Double defaultMonthlyRecordingGb) { this.defaultMonthlyRecordingGb = defaultMonthlyRecordingGb; }
    public Integer getDefaultAgentCount() { return defaultAgentCount; }
    public void setDefaultAgentCount(Integer defaultAgentCount) { this.defaultAgentCount = defaultAgentCount; }
    public Integer getDefaultConcurrentChannels() { return defaultConcurrentChannels; }
    public void setDefaultConcurrentChannels(Integer defaultConcurrentChannels) { this.defaultConcurrentChannels = defaultConcurrentChannels; }
    public Double getDefaultMarginPercent() { return defaultMarginPercent; }
    public void setDefaultMarginPercent(Double defaultMarginPercent) { this.defaultMarginPercent = defaultMarginPercent; }
    public String getProposalTerms() { return proposalTerms; }
    public void setProposalTerms(String proposalTerms) { this.proposalTerms = proposalTerms; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
