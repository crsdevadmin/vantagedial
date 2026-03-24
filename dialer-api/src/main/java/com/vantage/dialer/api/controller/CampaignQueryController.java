package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.campaign.CampaignStats;
import com.vantage.dialer.api.campaign.Lead;
import com.vantage.dialer.api.campaign.LeadStore;
import com.vantage.dialer.api.dto.CallSessionResponse;
import com.vantage.dialer.api.dto.CampaignResponse;
import com.vantage.dialer.api.service.CallSessionService;
import com.vantage.dialer.api.service.CampaignCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/campaigns")
public class CampaignQueryController {

    private final LeadStore leadStore;
    private final CampaignCatalogService campaignCatalogService;
    private final CallSessionService callSessionService;

    public CampaignQueryController(LeadStore leadStore,
                                   CampaignCatalogService campaignCatalogService,
                                   CallSessionService callSessionService) {
        this.leadStore = leadStore;
        this.campaignCatalogService = campaignCatalogService;
        this.callSessionService = callSessionService;
    }

    @GetMapping("/{campaignId}")
    public CampaignResponse getCampaign(@PathVariable String campaignId) {
        return campaignCatalogService.getCampaign(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown campaign: " + campaignId));
    }

    @GetMapping("/{campaignId}/leads")
    public List<Lead> getLeads(@PathVariable String campaignId) {
        return leadStore.getLeads(campaignId);
    }

    @GetMapping("/{campaignId}/stats")
    public CampaignStats getStats(@PathVariable String campaignId) {
        return leadStore.getStats(campaignId);
    }

    @GetMapping("/{campaignId}/sessions")
    public List<CallSessionResponse> getSessions(@PathVariable String campaignId) {
        return callSessionService.getCampaignSessions(campaignId);
    }
}
