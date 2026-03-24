package com.vantage.dialer.api.dto;

import java.util.ArrayList;
import java.util.List;

public class CustomerInstallationRequest {
    private String customerId;
    private String installationName;
    private String clientType;
    private Boolean dryRun;
    private Boolean performRemoteChecks;
    private Boolean deployAfterProvision;
    private Boolean useCustomerPresetDefaults = true;
    private List<AgentProvisionRequest> agents = new ArrayList<>();

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getInstallationName() {
        return installationName;
    }

    public void setInstallationName(String installationName) {
        this.installationName = installationName;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Boolean getPerformRemoteChecks() {
        return performRemoteChecks;
    }

    public void setPerformRemoteChecks(Boolean performRemoteChecks) {
        this.performRemoteChecks = performRemoteChecks;
    }

    public Boolean getDeployAfterProvision() {
        return deployAfterProvision;
    }

    public void setDeployAfterProvision(Boolean deployAfterProvision) {
        this.deployAfterProvision = deployAfterProvision;
    }

    public Boolean getUseCustomerPresetDefaults() {
        return useCustomerPresetDefaults;
    }

    public void setUseCustomerPresetDefaults(Boolean useCustomerPresetDefaults) {
        this.useCustomerPresetDefaults = useCustomerPresetDefaults;
    }

    public List<AgentProvisionRequest> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentProvisionRequest> agents) {
        this.agents = agents;
    }
}
