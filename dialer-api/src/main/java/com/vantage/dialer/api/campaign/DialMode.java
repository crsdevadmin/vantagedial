package com.vantage.dialer.api.campaign;

public enum DialMode {
    PROGRESSIVE,
    PREDICTIVE;

    public static DialMode from(String value) {
        if (value == null || value.isBlank()) {
            return PROGRESSIVE;
        }
        return DialMode.valueOf(value.trim().toUpperCase());
    }
}
