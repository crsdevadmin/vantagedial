package com.vantage.dialer.api.dto;

import java.time.Instant;
import java.util.List;

public record CustomerPortfolioResponse(
        Instant generatedAt,
        int totalCustomers,
        int healthyCustomers,
        int customersWithDeliveryPackage,
        int customersWithReport,
        int customersWithArtifactCatalog,
        boolean healthy,
        String statusMessage,
        List<CustomerPortfolioEntryResponse> customers) {
}
