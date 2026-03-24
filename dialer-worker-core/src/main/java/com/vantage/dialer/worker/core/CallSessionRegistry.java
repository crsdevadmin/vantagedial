package com.vantage.dialer.worker.core;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CallSessionRegistry {

    private final Map<String, CallSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdByActionId = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdByChannel = new ConcurrentHashMap<>();

    public void register(CallSession session) {
        sessionsById.put(session.getCallSessionId(), session);
    }

    public Optional<CallSession> get(String callSessionId) {
        return Optional.ofNullable(sessionsById.get(callSessionId));
    }

    public void mapAction(String actionId, String callSessionId) {
        sessionIdByActionId.put(actionId, callSessionId);
    }

    public Optional<CallSession> findByAction(String actionId) {
        String sessionId = sessionIdByActionId.get(actionId);
        return sessionId == null ? Optional.empty() : get(sessionId);
    }

    public void mapChannel(String channel, String callSessionId) {
        String normalizedChannel = normalizeChannel(channel);
        if (normalizedChannel != null) {
            sessionIdByChannel.put(normalizedChannel, callSessionId);
        }
    }

    public Optional<CallSession> findByChannel(String channel) {
        String normalizedChannel = normalizeChannel(channel);
        if (normalizedChannel == null) {
            return Optional.empty();
        }

        String sessionId = sessionIdByChannel.get(normalizedChannel);
        return sessionId == null ? Optional.empty() : get(sessionId);
    }

    public void remove(String callSessionId) {
        CallSession session = sessionsById.remove(callSessionId);
        if (session == null) {
            return;
        }

        sessionIdByActionId.entrySet().removeIf(entry -> callSessionId.equals(entry.getValue()));
        sessionIdByChannel.entrySet().removeIf(entry -> callSessionId.equals(entry.getValue()));
    }

    private String normalizeChannel(String channel) {
        if (channel == null) {
            return null;
        }

        String normalized = channel.trim();
        if (normalized.isBlank()) {
            return null;
        }

        int semicolonIndex = normalized.indexOf(';');
        if (semicolonIndex >= 0) {
            normalized = normalized.substring(0, semicolonIndex);
        }

        return normalized;
    }
}
