package com.vantage.dialer.api.service;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.dto.CommercialAssumptionsResponse;
import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CostEstimateResponse;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerQuoteServiceTest {

    @Test
    void quoteUsesInstallationCustomerAndCustomerProfileDefaults() {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        CustomerConfigurationService customerConfigurationService = mock(CustomerConfigurationService.class);
        PricingService pricingService = mock(PricingService.class);
        CustomerQuoteService service = new CustomerQuoteService(
                installationService,
                customerConfigurationService,
                pricingService
        );

        CostEstimateRequest request = new CostEstimateRequest();
        CustomerInstallationResponse installation = installation("install-1", "customer-1");
        CustomerConfigurationResponse configuration = configuration("customer-1");
        ProposalPresetResponse preset = preset("FULL_SUITE");
        CostEstimateResponse estimate = new CostEstimateResponse("customer-1", "default", 40.0, 72.0, 112.0, 149.5, 35.0);

        when(installationService.get("install-1")).thenReturn(installation);
        when(customerConfigurationService.find("customer-1")).thenReturn(Optional.of(configuration));
        when(customerConfigurationService.findProposalPreset("FULL_SUITE")).thenReturn(Optional.of(preset));
        when(pricingService.estimate(request)).thenReturn(estimate);

        InstallationQuoteSummaryResponse summary = service.quote("install-1", request);
        CommercialAssumptionsResponse assumptions = summary.commercialAssumptions();

        assertEquals("customer-1", request.getCustomerId());
        assertEquals("customer-1", summary.customerId());
        assertEquals("Acme Corp", summary.customerName());
        assertEquals(List.of("1001", "1002"), summary.provisionedExtensions());
        assertEquals("CUSTOMER_PROFILE", assumptions.source());
        assertEquals(25000L, assumptions.monthlyCallMinutes());
        assertEquals(120000L, assumptions.monthlyTtsUnits());
        assertEquals(5000L, assumptions.monthlySttMinutes());
        assertEquals(35.0, assumptions.monthlyRecordingGb());
        assertEquals(2, assumptions.agentCount());
        assertEquals(10, assumptions.concurrentChannels());
        assertEquals(35.0, assumptions.desiredMarginPercent());
        assertEquals(149.5, summary.estimate().suggestedSellPrice());
    }

    @Test
    void quoteUsesRequestCustomerAndRequestOverridesWhenNoProfileExists() {
        CustomerInstallationService installationService = mock(CustomerInstallationService.class);
        CustomerConfigurationService customerConfigurationService = mock(CustomerConfigurationService.class);
        PricingService pricingService = mock(PricingService.class);
        CustomerQuoteService service = new CustomerQuoteService(
                installationService,
                customerConfigurationService,
                pricingService
        );

        CostEstimateRequest request = new CostEstimateRequest();
        request.setCustomerId("customer-2");
        request.setMonthlyCallMinutes(9000L);
        request.setMonthlyTtsUnits(100L);
        request.setMonthlySttMinutes(50L);
        request.setMonthlyRecordingGb(2.5);
        request.setAgentCount(5);
        request.setConcurrentChannels(8);
        request.setDesiredMarginPercent(45.0);
        request.setUseCustomerPresetDefaults(false);

        CustomerInstallationResponse installation = installation("install-2", null);
        CostEstimateResponse estimate = new CostEstimateResponse("customer-2", "default", 30.0, 40.0, 70.0, 105.0, 45.0);

        when(installationService.get("install-2")).thenReturn(installation);
        when(customerConfigurationService.find("customer-2")).thenReturn(Optional.empty());
        when(pricingService.estimate(request)).thenReturn(estimate);

        InstallationQuoteSummaryResponse summary = service.quote("install-2", request);
        CommercialAssumptionsResponse assumptions = summary.commercialAssumptions();

        assertEquals("customer-2", summary.customerId());
        assertNull(summary.customerName());
        assertNull(summary.customerConfiguration());
        assertEquals("REQUEST", assumptions.source());
        assertEquals(9000L, assumptions.monthlyCallMinutes());
        assertEquals(100L, assumptions.monthlyTtsUnits());
        assertEquals(50L, assumptions.monthlySttMinutes());
        assertEquals(2.5, assumptions.monthlyRecordingGb());
        assertEquals(5, assumptions.agentCount());
        assertEquals(8, assumptions.concurrentChannels());
        assertEquals(45.0, assumptions.desiredMarginPercent());

        verify(customerConfigurationService, never()).findProposalPreset(anyString());
    }

    private CustomerInstallationResponse installation(String installationJobId, String customerId) {
        Agent agentOne = new Agent("agent-1", "Agent One", "PJSIP/1001", "1001", "user1", "pass1", AgentStatus.AVAILABLE);
        Agent agentTwo = new Agent("agent-2", "Agent Two", "PJSIP/1002", "1002", "user2", "pass2", AgentStatus.AVAILABLE);
        return new CustomerInstallationResponse(
                installationJobId,
                customerId,
                "Acme Softphone",
                "WEBRTC",
                "COMPLETED",
                false,
                true,
                true,
                2,
                "pkg-1",
                "deploy-1",
                List.of(agentOne, agentTwo),
                null,
                null,
                Instant.parse("2026-03-22T11:00:00Z"),
                Instant.parse("2026-03-22T12:00:00Z"),
                "Installation completed",
                null
        );
    }

    private CustomerConfigurationResponse configuration(String customerId) {
        return new CustomerConfigurationResponse(
                customerId,
                "Acme Corp",
                "pbx.acme.test",
                "10.0.0.10",
                "api.acme.test",
                "ubuntu",
                "/keys/pbx.pem",
                "/etc/asterisk/generated",
                "admin",
                "vivphone-endpoint",
                "91",
                "pbx.acme.test",
                "wss://pbx.acme.test/ws",
                "http://api.acme.test:8081",
                "jssip",
                "MONITOR_ONLY",
                "Acme Corp",
                null,
                "#1f2a2a",
                "#1c7c54",
                "FULL_SUITE",
                "STANDARD",
                "Vantage Dialer Proposal",
                "Outbound rollout",
                true,
                true,
                true,
                true,
                true,
                true,
                25000L,
                120000L,
                5000L,
                35.0,
                2,
                10,
                35.0,
                "Net 30"
        );
    }

    private ProposalPresetResponse preset(String presetId) {
        return new ProposalPresetResponse(
                presetId,
                "Full rollout",
                true,
                true,
                true,
                true,
                true,
                true,
                "WEBRTC",
                "jssip",
                "MONITOR_ONLY",
                true,
                true,
                25000L,
                120000L,
                5000L,
                35.0,
                2,
                10,
                35.0
        );
    }
}
