package com.vantage.dialer.api.dto;

public record AsteriskAgentConfigResponse(
        String agentId,
        String extensionNumber,
        String clientType,
        String endpointSnippet,
        String softphoneUsername,
        String softphonePassword,
        String transportName,
        String webSocketPath,
        String includeHint) {
}
