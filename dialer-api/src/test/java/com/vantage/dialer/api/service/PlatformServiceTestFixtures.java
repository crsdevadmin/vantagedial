package com.vantage.dialer.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.CustomerCommandCenterResponse;
import com.vantage.dialer.api.dto.CustomerPortfolioResponse;
import com.vantage.dialer.api.dto.TelephonyDeploymentAuditResponse;

import java.time.Instant;
import java.util.List;

final class PlatformServiceTestFixtures {

    private PlatformServiceTestFixtures() {
    }

    static ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    static CustomerCommandCenterResponse customerCommandCenterResponse(int totalCustomers, int healthyCustomers) {
        return new CustomerCommandCenterResponse(
                Instant.parse("2026-03-22T12:00:00Z"),
                null,
                totalCustomers,
                healthyCustomers,
                totalCustomers,
                totalCustomers,
                totalCustomers,
                totalCustomers,
                totalCustomers,
                healthyCustomers == totalCustomers,
                healthyCustomers == totalCustomers ? "Healthy" : "Attention",
                List.of()
        );
    }

    static CustomerPortfolioResponse customerPortfolioResponse(int totalCustomers, int healthyCustomers) {
        return new CustomerPortfolioResponse(
                Instant.parse("2026-03-22T12:05:00Z"),
                totalCustomers,
                healthyCustomers,
                totalCustomers,
                totalCustomers,
                totalCustomers,
                healthyCustomers == totalCustomers,
                healthyCustomers == totalCustomers ? "Healthy" : "Attention",
                List.of()
        );
    }

    static List<TelephonyDeploymentAuditResponse> sampleDeployments() {
        TelephonyDeploymentAuditResponse latestDeployment = new TelephonyDeploymentAuditResponse(
                "ASTERISK",
                "job-2",
                "pkg-2",
                "FULL",
                "SOFTPHONE",
                "DEPLOYED",
                false,
                true,
                "pbx-2",
                5060,
                "/srv/vantage",
                "/srv/vantage/package",
                "/opt/vantage",
                List.of("systemctl restart vantage"),
                List.of("extensions.conf", "queues.conf"),
                List.of("agent-1", "agent-2"),
                Instant.parse("2026-03-22T11:00:00Z"),
                Instant.parse("2026-03-22T11:05:00Z"),
                Instant.parse("2026-03-22T10:55:00Z"),
                "Deployment completed",
                null
        );
        TelephonyDeploymentAuditResponse previousDeployment = new TelephonyDeploymentAuditResponse(
                "CISCO",
                "job-1",
                "pkg-1",
                "PATCH",
                "HARDPHONE",
                "FAILED",
                true,
                false,
                "cisco-1",
                8443,
                "/var/cisco",
                "/var/cisco/package",
                "/opt/cisco",
                List.of("copy running-config startup-config"),
                List.of("gateway.cfg"),
                List.of("agent-9"),
                Instant.parse("2026-03-21T09:00:00Z"),
                null,
                Instant.parse("2026-03-21T08:45:00Z"),
                "",
                "gateway push failed"
        );
        return List.of(latestDeployment, previousDeployment);
    }
}
