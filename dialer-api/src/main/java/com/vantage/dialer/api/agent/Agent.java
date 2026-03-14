package com.vantage.dialer.api.agent;

public class Agent {

    private final String agentId;
    private final String agentName;
    private final String channel;
    private AgentStatus status;

    public Agent(String agentId, String agentName, String channel, AgentStatus status) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.channel = channel;
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

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }
}
