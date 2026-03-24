package com.vantage.dialer.api.dto;

public enum AsteriskClientType {
    SOFTPHONE,
    WEBRTC;

    public static AsteriskClientType from(String value) {
        if (value == null || value.isBlank()) {
            return SOFTPHONE;
        }
        return AsteriskClientType.valueOf(value.trim().toUpperCase());
    }
}
