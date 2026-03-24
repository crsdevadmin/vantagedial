package com.vantage.dialer.api.dto;

public record AsteriskPreflightCheckResponse(
        String checkName,
        boolean passed,
        String detail) {
}
