package com.vantage.dialer.api.agent;

public class Agent {

    private final String agentId;
    private final String agentName;
    private final String channel;
    private final String extensionNumber;
    private final String sipUsername;
    private final String sipPassword;
    private AgentStatus status;

    public Agent(String agentId,
                 String agentName,
                 String channel,
                 String extensionNumber,
                 String sipUsername,
                 String sipPassword,
                 AgentStatus status) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.channel = channel;
        this.extensionNumber = extensionNumber;
        this.sipUsername = sipUsername;
        this.sipPassword = sipPassword;
        this.status = status;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getChannel() {
        return channel;
    }

    public String getExtensionNumber() {
        return extensionNumber;
    }

    public String getSipUsername() {
        return sipUsername;
    }

    public String getSipPassword() {
        return sipPassword;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }
}
