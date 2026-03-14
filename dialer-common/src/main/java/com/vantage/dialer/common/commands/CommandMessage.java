package com.vantage.dialer.common.commands;

import java.time.Instant;
import java.util.Map;

public class CommandMessage {

    private String commandId;
    private CommandType commandType;
    private String callSessionId;
    private String provider;
    private Instant timestamp;
    private Map<String, String> payload;

    public CommandMessage() {
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public String getCallSessionId() {
        return callSessionId;
    }

    public void setCallSessionId(String callSessionId) {
        this.callSessionId = callSessionId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, String> payload) {
        this.payload = payload;
    }
}