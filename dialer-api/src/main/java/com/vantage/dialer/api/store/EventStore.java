package com.vantage.dialer.api.store;

import com.vantage.dialer.common.events.StandardEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventStore {

    private final ConcurrentHashMap<String, StandardEvent> latestBySession = new ConcurrentHashMap<>();

    public void upsert(StandardEvent event) {
        if (event.getCallSessionId() != null) {
            latestBySession.put(event.getCallSessionId(), event);
        }
    }

    public Optional<StandardEvent> getLatest(String callSessionId) {
        return Optional.ofNullable(latestBySession.get(callSessionId));
    }
}