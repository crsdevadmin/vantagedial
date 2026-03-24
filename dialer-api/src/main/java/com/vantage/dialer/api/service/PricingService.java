package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CostEstimateResponse;
import com.vantage.dialer.api.dto.CostConfigurationRequest;
import com.vantage.dialer.api.dto.CostConfigurationResponse;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import com.vantage.dialer.api.persistence.model.CostConfigurationEntity;
import com.vantage.dialer.api.persistence.repository.CostConfigurationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {

    private static final String DEFAULT_ID = "default";

    private final CostConfigurationRepository costConfigurationRepository;
    private final CustomerConfigurationService customerConfigurationService;

    public PricingService(CostConfigurationRepository costConfigurationRepository,
                          CustomerConfigurationService customerConfigurationService) {
        this.costConfigurationRepository = costConfigurationRepository;
        this.customerConfigurationService = customerConfigurationService;
    }

    @PostConstruct
    public void seedDefaultConfiguration() {
        if (costConfigurationRepository.existsById(DEFAULT_ID)) {
            return;
        }

        CostConfigurationEntity config = new CostConfigurationEntity();
        config.setConfigurationId(DEFAULT_ID);
        config.setCustomerId(null);
        config.setAsteriskServerMonthlyCost(15.18);
        config.setAppServerMonthlyCost(30.37);
        config.setEbsMonthlyCost(4.00);
        config.setSnapshotMonthlyCost(1.50);
        config.setVoiceMinuteCost(0.015);
        config.setTtsUnitCost(0.00002);
        config.setSttMinuteCost(0.012);
        config.setRecordingGbCost(0.10);
        costConfigurationRepository.save(config);
    }

    @Transactional(readOnly = true)
    public CostEstimateResponse estimate(CostEstimateRequest request) {
        CostConfigurationEntity config = resolveConfiguration(request.getCustomerId());
        CustomerConfigurationResponse customerConfiguration = resolveCustomerConfiguration(request);
        ProposalPresetResponse preset = resolvePreset(request);

        long monthlyCallMinutes = longOrDefault(request.getMonthlyCallMinutes(),
                customerConfiguration != null && customerConfiguration.defaultMonthlyCallMinutes() != null
                        ? customerConfiguration.defaultMonthlyCallMinutes()
                        : preset == null ? 0 : preset.defaultMonthlyCallMinutes());
        long monthlyTtsUnits = longOrDefault(request.getMonthlyTtsUnits(),
                customerConfiguration != null && customerConfiguration.defaultMonthlyTtsUnits() != null
                        ? customerConfiguration.defaultMonthlyTtsUnits()
                        : preset == null ? 0 : preset.defaultMonthlyTtsUnits());
        long monthlySttMinutes = longOrDefault(request.getMonthlySttMinutes(),
                customerConfiguration != null && customerConfiguration.defaultMonthlySttMinutes() != null
                        ? customerConfiguration.defaultMonthlySttMinutes()
                        : preset == null ? 0 : preset.defaultMonthlySttMinutes());
        double monthlyRecordingGb = doubleOrDefault(request.getMonthlyRecordingGb(),
                customerConfiguration != null && customerConfiguration.defaultMonthlyRecordingGb() != null
                        ? customerConfiguration.defaultMonthlyRecordingGb()
                        : preset == null ? 0 : preset.defaultMonthlyRecordingGb());
        double desiredMarginPercent = doubleOrDefault(request.getDesiredMarginPercent(),
                customerConfiguration != null && customerConfiguration.defaultMarginPercent() != null
                        ? customerConfiguration.defaultMarginPercent()
                        : preset == null ? 30 : preset.defaultMarginPercent());

        double fixed = config.getAsteriskServerMonthlyCost()
                + config.getAppServerMonthlyCost()
                + config.getEbsMonthlyCost()
                + config.getSnapshotMonthlyCost();

        double variable = monthlyCallMinutes * config.getVoiceMinuteCost()
                + monthlyTtsUnits * config.getTtsUnitCost()
                + monthlySttMinutes * config.getSttMinuteCost()
                + monthlyRecordingGb * config.getRecordingGbCost();

        double total = fixed + variable;
        double suggestedSellPrice = total * (1 + (desiredMarginPercent / 100.0));

        return new CostEstimateResponse(
                request.getCustomerId(),
                config.getConfigurationId(),
                round(fixed),
                round(variable),
                round(total),
                round(suggestedSellPrice),
                desiredMarginPercent
        );
    }

    @Transactional
    public CostConfigurationResponse upsertCustomerConfiguration(CostConfigurationRequest request) {
        String customerId = normalize(request.getCustomerId());
        if (customerId == null) {
            throw new IllegalArgumentException("customerId is required");
        }
        String configurationId = "customer-" + customerId;
        CostConfigurationEntity config = costConfigurationRepository.findById(configurationId)
                .orElseGet(CostConfigurationEntity::new);
        config.setConfigurationId(configurationId);
        config.setCustomerId(customerId);
        config.setAsteriskServerMonthlyCost(request.getAsteriskServerMonthlyCost());
        config.setAppServerMonthlyCost(request.getAppServerMonthlyCost());
        config.setEbsMonthlyCost(request.getEbsMonthlyCost());
        config.setSnapshotMonthlyCost(request.getSnapshotMonthlyCost());
        config.setVoiceMinuteCost(request.getVoiceMinuteCost());
        config.setTtsUnitCost(request.getTtsUnitCost());
        config.setSttMinuteCost(request.getSttMinuteCost());
        config.setRecordingGbCost(request.getRecordingGbCost());
        return toResponse(costConfigurationRepository.save(config));
    }

    @Transactional(readOnly = true)
    public CostConfigurationResponse getCustomerConfiguration(String customerId) {
        return costConfigurationRepository.findByCustomerId(customerId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Unknown customer pricing configuration: " + customerId));
    }

    @Transactional(readOnly = true)
    public CostConfigurationResponse getDefaultConfiguration() {
        return toResponse(getDefaultEntity());
    }

    private CostConfigurationEntity resolveConfiguration(String customerId) {
        if (customerId != null && !customerId.isBlank()) {
            return costConfigurationRepository.findByCustomerId(customerId)
                    .orElseGet(this::getDefaultEntity);
        }
        return getDefaultEntity();
    }

    private CostConfigurationEntity getDefaultEntity() {
        return costConfigurationRepository.findById(DEFAULT_ID)
                .orElseThrow(() -> new IllegalStateException("Missing default cost configuration"));
    }

    private CostConfigurationResponse toResponse(CostConfigurationEntity config) {
        return new CostConfigurationResponse(
                config.getConfigurationId(),
                config.getCustomerId(),
                config.getAsteriskServerMonthlyCost(),
                config.getAppServerMonthlyCost(),
                config.getEbsMonthlyCost(),
                config.getSnapshotMonthlyCost(),
                config.getVoiceMinuteCost(),
                config.getTtsUnitCost(),
                config.getSttMinuteCost(),
                config.getRecordingGbCost()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private ProposalPresetResponse resolvePreset(CostEstimateRequest request) {
        if (!useCustomerPresetDefaults(request)) {
            return null;
        }
        CustomerConfigurationResponse configuration = resolveCustomerConfiguration(request);
        if (configuration == null) {
            return null;
        }
        return customerConfigurationService.findProposalPreset(configuration.proposalPreset()).orElse(null);
    }

    private CustomerConfigurationResponse resolveCustomerConfiguration(CostEstimateRequest request) {
        if (!useCustomerPresetDefaults(request)) {
            return null;
        }
        String customerId = normalize(request.getCustomerId());
        if (customerId == null) {
            return null;
        }
        return customerConfigurationService.find(customerId).orElse(null);
    }

    private boolean useCustomerPresetDefaults(CostEstimateRequest request) {
        return request.getUseCustomerPresetDefaults() == null || request.getUseCustomerPresetDefaults();
    }

    private long longOrDefault(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private double doubleOrDefault(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}
