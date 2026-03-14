package com.vantage.dialer.worker.provider.asterisk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asterisk.ami")
public class AsteriskProperties {

    private String host = "localhost";
    private int port = 5038;
    private String username = "admin";
    private String password;
    private String endpoint = "vivphone-endpoint";
    private String callerId = "Vantage Dialer";
    private String dialPrefix = "91";
    private String originateContext = "from-internal";
    private String originateExtension = "s";
    private int originatePriority = 1;
    private long originateTimeoutMs = 30000L;
    private long reconnectDelayMs = 10000L;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getCallerId() { return callerId; }
    public void setCallerId(String callerId) { this.callerId = callerId; }
    public String getDialPrefix() { return dialPrefix; }
    public void setDialPrefix(String dialPrefix) { this.dialPrefix = dialPrefix; }
    public String getOriginateContext() { return originateContext; }
    public void setOriginateContext(String originateContext) { this.originateContext = originateContext; }
    public String getOriginateExtension() { return originateExtension; }
    public void setOriginateExtension(String originateExtension) { this.originateExtension = originateExtension; }
    public int getOriginatePriority() { return originatePriority; }
    public void setOriginatePriority(int originatePriority) { this.originatePriority = originatePriority; }
    public long getOriginateTimeoutMs() { return originateTimeoutMs; }
    public void setOriginateTimeoutMs(long originateTimeoutMs) { this.originateTimeoutMs = originateTimeoutMs; }
    public long getReconnectDelayMs() { return reconnectDelayMs; }
    public void setReconnectDelayMs(long reconnectDelayMs) { this.reconnectDelayMs = reconnectDelayMs; }
}
