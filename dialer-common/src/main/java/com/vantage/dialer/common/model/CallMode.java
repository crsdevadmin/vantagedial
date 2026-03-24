package com.vantage.dialer.common.model;

public enum CallMode {
    AGENT_ASSISTED,
    OUTBOUND_IVR;

    public static CallMode from(String value) {
        if (value == null || value.isBlank()) {
            return AGENT_ASSISTED;
        }
        return CallMode.valueOf(value.trim().toUpperCase());
    }
}
