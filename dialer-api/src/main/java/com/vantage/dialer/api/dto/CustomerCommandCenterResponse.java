package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerCommandCenterResponse(
        Instant generatedAt,
        CustomerPortfolioResponse portfolio,
        int totalCustomers,
        int healthyCustomers,
        int customersWithInstallations,
        int customersWithQuotes,
        int customersWithDeliveryPackage,
        int customersWithReport,
        int customersWithArtifactCatalog,
        boolean healthy,
        String statusMessage,
        List<CustomerCommandCenterEntryResponse> customers) {
}
