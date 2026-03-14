package com.vantage.dialer.worker.core;

import com.vantage.dialer.common.events.EventType;

import java.util.Map;

public interface CallEventPublisher {

    void publish(CallSession session, EventType eventType);

    void publishFailure(CallSession session, String result, Object error);

    void publish(CallSession session, EventType eventType, Map<String, Object> extraPayload);
}
