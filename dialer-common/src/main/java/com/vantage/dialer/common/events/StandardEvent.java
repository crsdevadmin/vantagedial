package com.vantage.dialer.common.events;

import java.time.Instant;
import java.util.Map;

public class StandardEvent {
  private String eventId;
  private String callSessionId;
  private String callLegId;
  private EventType eventType;
  private Instant timestamp;
  private String provider;
  private String providerCallId;
  private String legType;
  private Map<String, Object> payload;

  public StandardEvent() {}

  public String getEventId() { return eventId; }
  public void setEventId(String eventId) { this.eventId = eventId; }

  public String getCallSessionId() { return callSessionId; }
  public void setCallSessionId(String callSessionId) { this.callSessionId = callSessionId; }

  public String getCallLegId() { return callLegId; }
  public void setCallLegId(String callLegId) { this.callLegId = callLegId; }

  public EventType getEventType() { return eventType; }
  public void setEventType(EventType eventType) { this.eventType = eventType; }

  public Instant getTimestamp() { return timestamp; }
  public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

  public String getProvider() { return provider; }
  public void setProvider(String provider) { this.provider = provider; }

  public String getProviderCallId() { return providerCallId; }
  public void setProviderCallId(String providerCallId) { this.providerCallId = providerCallId; }

  public String getLegType() { return legType; }
  public void setLegType(String legType) { this.legType = legType; }

  public Map<String, Object> getPayload() { return payload; }
  public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}