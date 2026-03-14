package com.vantage.dialer.worker.provider.asterisk.service;

import com.vantage.dialer.worker.provider.asterisk.config.AsteriskProperties;
import jakarta.annotation.PreDestroy;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.BridgeAction;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.response.ManagerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AsteriskService {

    private static final Logger log = LoggerFactory.getLogger(AsteriskService.class);

    private final AsteriskProperties properties;
    private final AmiConnectionFactory connectionFactory;
    private final List<ManagerEventListener> eventListeners = new CopyOnWriteArrayList<>();

    private volatile boolean shutdown;
    private ManagerConnection managerConnection;

    public AsteriskService(AsteriskProperties properties) {
        this(properties, configuredProperties -> {
            ManagerConnectionFactory factory = new ManagerConnectionFactory(
                    configuredProperties.getHost(),
                    configuredProperties.getPort(),
                    configuredProperties.getUsername(),
                    configuredProperties.getPassword()
            );
            return factory.createManagerConnection();
        });
    }

    AsteriskService(AsteriskProperties properties, AmiConnectionFactory connectionFactory) {
        this.properties = properties;
        this.connectionFactory = connectionFactory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeOnStartup() {
        connectInternal("startup", false);
    }

    @Scheduled(fixedDelayString = "${asterisk.ami.reconnect-delay-ms:10000}")
    public void reconnectIfDisconnected() {
        if (shutdown || isConnected()) {
            return;
        }

        log.info("Attempting Asterisk AMI reconnect to {}:{}", properties.getHost(), properties.getPort());
        connectInternal("reconnect", false);
    }

    public synchronized void connect() {
        connectInternal("manual", true);
    }

    private synchronized void connectInternal(String reason, boolean failFast) {
        if (shutdown || isConnected()) {
            return;
        }

        closeStaleConnection();

        if (isConnected()) {
            return;
        }

        try {
            managerConnection = connectionFactory.create(properties);
            managerConnection.login();
            registerEventListeners(managerConnection);

            log.info("Connected to Asterisk AMI at {}:{} ({})",
                    properties.getHost(), properties.getPort(), reason);
        } catch (IOException | AuthenticationFailedException | TimeoutException e) {
            managerConnection = null;
            log.error("Failed to connect to Asterisk AMI at {}:{} ({})",
                    properties.getHost(), properties.getPort(), reason, e);
            if (failFast) {
                throw new IllegalStateException(
                        "Failed to connect to Asterisk AMI at "
                                + properties.getHost() + ":" + properties.getPort(), e);
            }
        }
    }

    private void registerEventListeners(ManagerConnection connection) {
        for (ManagerEventListener listener : eventListeners) {
            connection.addEventListener(listener);
        }
    }

    private void closeStaleConnection() {
        if (managerConnection == null) {
            return;
        }

        if (isConnected()) {
            return;
        }

        try {
            managerConnection.logoff();
        } catch (Exception e) {
            log.debug("Ignoring stale AMI connection cleanup failure", e);
        } finally {
            managerConnection = null;
        }
    }

    @PreDestroy
    public synchronized void disconnect() {
        shutdown = true;
        if (managerConnection == null) {
            return;
        }

        try {
            managerConnection.logoff();
            log.info("Disconnected from Asterisk AMI");
        } catch (Exception e) {
            log.warn("Failed to disconnect cleanly from Asterisk AMI", e);
        } finally {
            managerConnection = null;
        }
    }

    public synchronized ManagerResponse originateCall(String phoneNumber, String actionId) {
        ensureConnected();

        OriginateAction action = new OriginateAction();
        action.setActionId(actionId);
        action.setAsync(true);
        action.setChannel(buildChannel(phoneNumber));
        action.setContext(properties.getOriginateContext());
        action.setExten(properties.getOriginateExtension());
        action.setPriority(properties.getOriginatePriority());
        action.setTimeout((int) properties.getOriginateTimeoutMs());
        if (properties.getCallerId() != null && !properties.getCallerId().isBlank()) {
            action.setCallerId(properties.getCallerId());
        }

        try {
            ManagerResponse response = managerConnection.sendAction(action, properties.getOriginateTimeoutMs());
            log.info("AMI originate submitted for channel={} response={}",
                    action.getChannel(), response.getResponse());
            return response;
        } catch (IOException | TimeoutException e) {
            throw new IllegalStateException(
                    "Failed to originate call for " + phoneNumber + " via Asterisk AMI", e);
        }
    }

    public synchronized ManagerResponse originateChannel(String channel, String actionId) {
        ensureConnected();

        OriginateAction action = new OriginateAction();
        action.setActionId(actionId);
        action.setAsync(true);
        action.setChannel(channel);
        action.setContext(properties.getOriginateContext());
        action.setExten(properties.getOriginateExtension());
        action.setPriority(properties.getOriginatePriority());
        action.setTimeout((int) properties.getOriginateTimeoutMs());
        if (properties.getCallerId() != null && !properties.getCallerId().isBlank()) {
            action.setCallerId(properties.getCallerId());
        }

        try {
            ManagerResponse response = managerConnection.sendAction(action, properties.getOriginateTimeoutMs());
            log.info("AMI originate submitted for channel={} response={}",
                    channel, response.getResponse());
            return response;
        } catch (IOException | TimeoutException e) {
            throw new IllegalStateException(
                    "Failed to originate channel " + channel + " via Asterisk AMI", e);
        }
    }

    public synchronized ManagerResponse bridgeChannels(String channel1, String channel2) {
        ensureConnected();

        BridgeAction action = new BridgeAction();
        action.setChannel1(channel1);
        action.setChannel2(channel2);

        try {
            ManagerResponse response = managerConnection.sendAction(action, properties.getOriginateTimeoutMs());
            log.info("AMI bridge submitted channel1={} channel2={} response={}",
                    channel1, channel2, response.getResponse());
            return response;
        } catch (IOException | TimeoutException e) {
            throw new IllegalStateException(
                    "Failed to bridge channels " + channel1 + " and " + channel2 + " via Asterisk AMI", e);
        }
    }

    public synchronized void addEventListener(ManagerEventListener listener) {
        eventListeners.add(listener);
        if (isConnected()) {
            managerConnection.addEventListener(listener);
        }
    }

    public String buildChannel(String phoneNumber) {
        return "PJSIP/" + properties.getDialPrefix() + normalizeNumber(phoneNumber) + "@" + properties.getEndpoint();
    }

    private void ensureConnected() {
        if (!isConnected()) {
            log.info("Asterisk AMI connection is down, attempting reconnect before request");
            connectInternal("on-demand reconnect", true);
        }
    }

    private boolean isConnected() {
        return managerConnection != null
                && managerConnection.getState() != null
                && "CONNECTED".equals(managerConnection.getState().name());
    }

    private String normalizeNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException("phoneNumber is required");
        }

        String normalized = phoneNumber.replaceAll("\\D", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("phoneNumber must contain digits");
        }

        return normalized;
    }

    @FunctionalInterface
    interface AmiConnectionFactory {
        ManagerConnection create(AsteriskProperties properties)
                throws IOException, AuthenticationFailedException, TimeoutException;
    }
}
