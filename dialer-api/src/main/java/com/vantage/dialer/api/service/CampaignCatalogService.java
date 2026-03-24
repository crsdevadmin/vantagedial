package com.vantage.dialer.api.service;

import com.vantage.dialer.api.campaign.DialMode;
import com.vantage.dialer.api.dto.CampaignRequest;
import com.vantage.dialer.api.dto.CampaignResponse;
import com.vantage.dialer.api.persistence.model.CampaignEntity;
import com.vantage.dialer.api.persistence.model.CampaignStatus;
import com.vantage.dialer.api.persistence.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CampaignCatalogService {

    private final CampaignRepository campaignRepository;

    public CampaignCatalogService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request) {
        CampaignEntity campaign = new CampaignEntity();
        campaign.setCampaignId(UUID.randomUUID().toString());
        campaign.setName(request.getName() == null || request.getName().isBlank()
                ? "Campaign " + System.currentTimeMillis()
                : request.getName());
        campaign.setProvider(defaultString(request.getProvider(), "ASTERISK"));
        campaign.setDialMode(DialMode.from(request.getDialMode()));
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setMaxConcurrentCalls(defaultInt(request.getMaxConcurrentCalls(), 5));
        campaign.setCallsPerSecond(defaultInt(request.getCallsPerSecond(), 2));
        campaign.setPredictiveRatio(defaultDouble(request.getPredictiveRatio(), 1.5));
        campaign.setIvrFlowId(blankToNull(request.getIvrFlowId()));
        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public Optional<CampaignResponse> getCampaign(String campaignId) {
        return campaignRepository.findById(campaignId).map(this::toResponse);
    }

    @Transactional
    public void markRunning(String campaignId, DialMode dialMode, String provider, int maxConcurrentCalls, int callsPerSecond, double predictiveRatio) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseGet(() -> {
                    CampaignEntity created = new CampaignEntity();
                    created.setCampaignId(campaignId);
                    created.setName("Campaign " + campaignId);
                    created.setIvrFlowId(null);
                    return created;
                });
        campaign.setProvider(provider);
        campaign.setDialMode(dialMode);
        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setMaxConcurrentCalls(maxConcurrentCalls);
        campaign.setCallsPerSecond(callsPerSecond);
        campaign.setPredictiveRatio(predictiveRatio);
        if (campaign.getName() == null) {
            campaign.setName("Campaign " + campaignId);
        }
        campaignRepository.save(campaign);
    }

    @Transactional
    public void markStopped(String campaignId) {
        campaignRepository.findById(campaignId).ifPresent(campaign -> {
            campaign.setStatus(CampaignStatus.STOPPED);
            campaignRepository.save(campaign);
        });
    }

    private CampaignResponse toResponse(CampaignEntity entity) {
        return new CampaignResponse(
                entity.getCampaignId(),
                entity.getName(),
                entity.getProvider(),
                entity.getDialMode().name(),
                entity.getStatus().name(),
                entity.getIvrFlowId(),
                entity.getMaxConcurrentCalls(),
                entity.getCallsPerSecond(),
                entity.getPredictiveRatio()
        );
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
