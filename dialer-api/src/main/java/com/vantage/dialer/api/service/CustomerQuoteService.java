package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CostEstimateResponse;
import com.vantage.dialer.api.dto.CommercialAssumptionsResponse;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.InstallationQuoteSummaryResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CustomerQuoteService {

    private final CustomerInstallationService installationService;
    private final CustomerConfigurationService customerConfigurationService;
    private final PricingService pricingService;

    public CustomerQuoteService(CustomerInstallationService installationService,
                                CustomerConfigurationService customerConfigurationService,
                                PricingService pricingService) {
        this.installationService = installationService;
        this.customerConfigurationService = customerConfigurationService;
        this.pricingService = pricingService;
    }

    public InstallationQuoteSummaryResponse quote(String installationJobId, CostEstimateRequest request) {
        CustomerInstallationResponse installation = installationService.get(installationJobId);
        String customerId = installation.customerId() != null ? installation.customerId() : request.getCustomerId();
        if (customerId != null && (request.getCustomerId() == null || request.getCustomerId().isBlank())) {
            request.setCustomerId(customerId);
        }

        CustomerConfigurationResponse customerConfiguration = customerId == null
                ? null
                : customerConfigurationService.find(customerId).orElse(null);
        ProposalPresetResponse preset = customerConfiguration == null
                ? null
                : customerConfigurationService.findProposalPreset(customerConfiguration.proposalPreset()).orElse(null);
        CostEstimateResponse estimate = pricingService.estimate(request);

        return new InstallationQuoteSummaryResponse(
                installation.installationJobId(),
                customerId,
                installation.installationName(),
                customerConfiguration == null ? null : customerConfiguration.customerName(),
                installation.clientType(),
                installation.status(),
                installation.agentCount(),
                installation.provisionedAgents().stream().map(agent -> agent.getExtensionNumber()).toList(),
                customerConfiguration,
                resolveCommercialAssumptions(request, customerConfiguration, preset),
                estimate,
                Instant.now()
        );
    }

    private CommercialAssumptionsResponse resolveCommercialAssumptions(CostEstimateRequest request,
                                                                       CustomerConfigurationResponse customerConfiguration,
                                                                       ProposalPresetResponse preset) {
        boolean usePresetDefaults = request.getUseCustomerPresetDefaults() == null || request.getUseCustomerPresetDefaults();
        String source = usePresetDefaults
                ? customerConfiguration != null ? "CUSTOMER_PROFILE" : preset != null ? "PRESET" : "REQUEST"
                : "REQUEST";
        long monthlyCallMinutes = request.getMonthlyCallMinutes() != null
                ? request.getMonthlyCallMinutes()
                : customerConfiguration != null && customerConfiguration.defaultMonthlyCallMinutes() != null
                ? customerConfiguration.defaultMonthlyCallMinutes()
                : preset != null ? preset.defaultMonthlyCallMinutes() : 0L;
        long monthlyTtsUnits = request.getMonthlyTtsUnits() != null
                ? request.getMonthlyTtsUnits()
                : customerConfiguration != null && customerConfiguration.defaultMonthlyTtsUnits() != null
                ? customerConfiguration.defaultMonthlyTtsUnits()
                : preset != null ? preset.defaultMonthlyTtsUnits() : 0L;
        long monthlySttMinutes = request.getMonthlySttMinutes() != null
                ? request.getMonthlySttMinutes()
                : customerConfiguration != null && customerConfiguration.defaultMonthlySttMinutes() != null
                ? customerConfiguration.defaultMonthlySttMinutes()
                : preset != null ? preset.defaultMonthlySttMinutes() : 0L;
        double monthlyRecordingGb = request.getMonthlyRecordingGb() != null
                ? request.getMonthlyRecordingGb()
                : customerConfiguration != null && customerConfiguration.defaultMonthlyRecordingGb() != null
                ? customerConfiguration.defaultMonthlyRecordingGb()
                : preset != null ? preset.defaultMonthlyRecordingGb() : 0D;
        int agentCount = request.getAgentCount() != null
                ? request.getAgentCount()
                : customerConfiguration != null && customerConfiguration.defaultAgentCount() != null
                ? customerConfiguration.defaultAgentCount()
                : preset != null ? preset.defaultAgentCount() : 0;
        int concurrentChannels = request.getConcurrentChannels() != null
                ? request.getConcurrentChannels()
                : customerConfiguration != null && customerConfiguration.defaultConcurrentChannels() != null
                ? customerConfiguration.defaultConcurrentChannels()
                : preset != null ? preset.defaultConcurrentChannels() : 0;
        double desiredMarginPercent = request.getDesiredMarginPercent() != null
                ? request.getDesiredMarginPercent()
                : customerConfiguration != null && customerConfiguration.defaultMarginPercent() != null
                ? customerConfiguration.defaultMarginPercent()
                : preset != null ? preset.defaultMarginPercent() : 30D;

        return new CommercialAssumptionsResponse(
                source,
                monthlyCallMinutes,
                monthlyTtsUnits,
                monthlySttMinutes,
                monthlyRecordingGb,
                agentCount,
                concurrentChannels,
                desiredMarginPercent
        );
    }
}
