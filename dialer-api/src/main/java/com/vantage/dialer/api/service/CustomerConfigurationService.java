package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.CustomerConfigurationRequest;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import com.vantage.dialer.api.persistence.model.CustomerConfigurationEntity;
import com.vantage.dialer.api.persistence.repository.CustomerConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerConfigurationService {

    private final CustomerConfigurationRepository repository;

    public CustomerConfigurationService(CustomerConfigurationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerConfigurationResponse createOrUpdate(CustomerConfigurationRequest request) {
        String customerId = normalize(request.getCustomerId());
        if (customerId == null) {
            throw new IllegalArgumentException("customerId is required");
        }
        CustomerConfigurationEntity entity = repository.findById(customerId).orElseGet(CustomerConfigurationEntity::new);
        entity.setCustomerId(customerId);
        entity.setCustomerName(fallback(request.getCustomerName(), customerId));
        entity.setServerAHost(normalize(request.getServerAHost()));
        entity.setServerAPrivateIp(normalize(request.getServerAPrivateIp()));
        entity.setServerBHost(normalize(request.getServerBHost()));
        entity.setAsteriskDeployUser(fallback(request.getAsteriskDeployUser(), "ubuntu"));
        entity.setAsteriskDeployPrivateKeyPath(normalize(request.getAsteriskDeployPrivateKeyPath()));
        entity.setAsteriskDeployTargetDirectory(fallback(request.getAsteriskDeployTargetDirectory(), "/etc/asterisk/generated"));
        entity.setAmiUsername(fallback(request.getAmiUsername(), "admin"));
        entity.setAmiEndpoint(fallback(request.getAmiEndpoint(), "vivphone-endpoint"));
        entity.setDialPrefix(fallback(request.getDialPrefix(), "91"));
        entity.setSipDomain(normalize(request.getSipDomain()));
        entity.setWebSocketUrl(normalize(request.getWebSocketUrl()));
        entity.setApiBaseUrl(normalize(request.getApiBaseUrl()));
        entity.setDefaultAgentUiMode(fallback(request.getDefaultAgentUiMode(), "jssip"));
        entity.setDefaultSupervisorUiMode(fallback(request.getDefaultSupervisorUiMode(), "MONITOR_ONLY"));
        entity.setBrandDisplayName(fallback(request.getBrandDisplayName(), entity.getCustomerName()));
        entity.setBrandLogoUrl(normalize(request.getBrandLogoUrl()));
        entity.setBrandPrimaryColor(fallback(request.getBrandPrimaryColor(), "#1f2a2a"));
        entity.setBrandAccentColor(fallback(request.getBrandAccentColor(), "#1c7c54"));
        String proposalPreset = fallback(request.getProposalPreset(), "FULL_SUITE");
        entity.setProposalPreset(proposalPreset);
        entity.setProposalTemplate(fallback(request.getProposalTemplate(), "STANDARD"));
        entity.setProposalTitle(fallback(request.getProposalTitle(), "Vantage Dialer Proposal"));
        entity.setProposalSubtitle(fallback(request.getProposalSubtitle(), "Single-tenant outbound dialer deployment proposal"));
        entity.setProposalIncludeAgentOutbound(resolvePresetFlag(proposalPreset, "agentOutbound", request.getProposalIncludeAgentOutbound(), true));
        entity.setProposalIncludeIvr(resolvePresetFlag(proposalPreset, "ivr", request.getProposalIncludeIvr(), true));
        entity.setProposalIncludeReporting(resolvePresetFlag(proposalPreset, "reporting", request.getProposalIncludeReporting(), true));
        entity.setProposalIncludeWebRtc(resolvePresetFlag(proposalPreset, "webRtc", request.getProposalIncludeWebRtc(), true));
        entity.setProposalIncludeProvisioning(resolvePresetFlag(proposalPreset, "provisioning", request.getProposalIncludeProvisioning(), true));
        entity.setProposalIncludePricingBreakdown(resolvePresetFlag(proposalPreset, "pricingBreakdown", request.getProposalIncludePricingBreakdown(), true));
        entity.setDefaultMonthlyCallMinutes(request.getDefaultMonthlyCallMinutes());
        entity.setDefaultMonthlyTtsUnits(request.getDefaultMonthlyTtsUnits());
        entity.setDefaultMonthlySttMinutes(request.getDefaultMonthlySttMinutes());
        entity.setDefaultMonthlyRecordingGb(request.getDefaultMonthlyRecordingGb());
        entity.setDefaultAgentCount(request.getDefaultAgentCount());
        entity.setDefaultConcurrentChannels(request.getDefaultConcurrentChannels());
        entity.setDefaultMarginPercent(request.getDefaultMarginPercent());
        entity.setProposalTerms(normalize(request.getProposalTerms()));
        return toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<CustomerConfigurationResponse> find(String customerId) {
        return repository.findById(customerId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CustomerConfigurationResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProposalPresetResponse> listProposalPresets() {
        return List.of(
                new ProposalPresetResponse("AGENT_ONLY", "Agent-assisted outbound only", true, false, true, false, true, true, "SOFTPHONE", "mock", "MONITOR_ONLY", true, true, 8000, 0, 0, 5, 10, 20, 28),
                new ProposalPresetResponse("AGENT_PLUS_IVR", "Agent outbound plus IVR and reporting", true, true, true, false, true, true, "SOFTPHONE", "mock", "MONITOR_ONLY", true, true, 12000, 50000, 0, 10, 15, 30, 30),
                new ProposalPresetResponse("WEBRTC_CONTACT_CENTER", "Browser softphone centric rollout", true, true, true, true, true, true, "WEBRTC", "jssip", "MONITOR_ONLY", true, true, 18000, 80000, 3000, 20, 25, 40, 32),
                new ProposalPresetResponse("COMMERCIAL_MINIMAL", "Lean sales proposal without pricing detail section", true, false, false, false, true, false, "SOFTPHONE", "mock", "MONITOR_ONLY", false, false, 4000, 0, 0, 0, 5, 10, 25),
                new ProposalPresetResponse("FULL_SUITE", "Full platform proposal", true, true, true, true, true, true, "WEBRTC", "jssip", "MONITOR_ONLY", true, true, 25000, 120000, 5000, 35, 30, 50, 35)
        );
    }

    @Transactional(readOnly = true)
    public Optional<ProposalPresetResponse> findProposalPreset(String presetId) {
        String normalized = normalizePreset(presetId);
        return listProposalPresets().stream()
                .filter(preset -> preset.presetId().equalsIgnoreCase(normalized))
                .findFirst();
    }

    private CustomerConfigurationResponse toResponse(CustomerConfigurationEntity entity) {
        return new CustomerConfigurationResponse(
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getServerAHost(),
                entity.getServerAPrivateIp(),
                entity.getServerBHost(),
                entity.getAsteriskDeployUser(),
                entity.getAsteriskDeployPrivateKeyPath(),
                entity.getAsteriskDeployTargetDirectory(),
                entity.getAmiUsername(),
                entity.getAmiEndpoint(),
                entity.getDialPrefix(),
                entity.getSipDomain(),
                entity.getWebSocketUrl(),
                entity.getApiBaseUrl(),
                entity.getDefaultAgentUiMode(),
                entity.getDefaultSupervisorUiMode(),
                entity.getBrandDisplayName(),
                entity.getBrandLogoUrl(),
                entity.getBrandPrimaryColor(),
                entity.getBrandAccentColor(),
                entity.getProposalPreset(),
                entity.getProposalTemplate(),
                entity.getProposalTitle(),
                entity.getProposalSubtitle(),
                entity.getProposalIncludeAgentOutbound(),
                entity.getProposalIncludeIvr(),
                entity.getProposalIncludeReporting(),
                entity.getProposalIncludeWebRtc(),
                entity.getProposalIncludeProvisioning(),
                entity.getProposalIncludePricingBreakdown(),
                entity.getDefaultMonthlyCallMinutes(),
                entity.getDefaultMonthlyTtsUnits(),
                entity.getDefaultMonthlySttMinutes(),
                entity.getDefaultMonthlyRecordingGb(),
                entity.getDefaultAgentCount(),
                entity.getDefaultConcurrentChannels(),
                entity.getDefaultMarginPercent(),
                entity.getProposalTerms()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String fallback(String value, String fallback) {
        String normalized = normalize(value);
        return normalized == null ? fallback : normalized;
    }

    private boolean booleanFallback(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private boolean resolvePresetFlag(String preset, String flagName, Boolean explicitValue, boolean fallback) {
        if (explicitValue != null) {
            return explicitValue;
        }
        return switch (normalizePreset(preset)) {
            case "AGENT_ONLY" -> switch (flagName) {
                case "agentOutbound", "reporting", "provisioning", "pricingBreakdown" -> true;
                default -> false;
            };
            case "AGENT_PLUS_IVR" -> switch (flagName) {
                case "agentOutbound", "ivr", "reporting", "provisioning", "pricingBreakdown" -> true;
                default -> false;
            };
            case "WEBRTC_CONTACT_CENTER", "FULL_SUITE" -> true;
            case "COMMERCIAL_MINIMAL" -> switch (flagName) {
                case "agentOutbound", "provisioning" -> true;
                default -> false;
            };
            default -> fallback;
        };
    }

    private String normalizePreset(String preset) {
        String normalized = normalize(preset);
        return normalized == null ? "FULL_SUITE" : normalized.toUpperCase();
    }
}
