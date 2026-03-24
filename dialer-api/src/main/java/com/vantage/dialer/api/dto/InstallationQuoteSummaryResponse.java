package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record InstallationQuoteSummaryResponse(
        String installationJobId,
        String customerId,
        String installationName,
        String customerName,
        String clientType,
        String installationStatus,
        int provisionedAgentCount,
        List<String> provisionedExtensions,
        CustomerConfigurationResponse customerConfiguration,
        CommercialAssumptionsResponse commercialAssumptions,
        CostEstimateResponse estimate,
        Instant generatedAt) {
}
