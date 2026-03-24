package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.campaign.DialMode;
import com.vantage.dialer.api.campaign.CampaignEngine;
import com.vantage.dialer.api.campaign.Lead;
import com.vantage.dialer.api.campaign.LeadStore;
import com.vantage.dialer.api.dto.CampaignRequest;
import com.vantage.dialer.api.dto.CampaignResponse;
import com.vantage.dialer.api.service.CampaignCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private final LeadStore leadStore;
    private final CampaignEngine engine;
    private final CampaignCatalogService campaignCatalogService;

    public CampaignController(LeadStore leadStore,
                              CampaignEngine engine,
                              CampaignCatalogService campaignCatalogService) {
        this.leadStore = leadStore;
        this.engine = engine;
        this.campaignCatalogService = campaignCatalogService;
    }

    @PostMapping
    public CampaignResponse createCampaign(@RequestBody(required = false) CampaignRequest request) {
        return campaignCatalogService.createCampaign(request == null ? new CampaignRequest() : request);
    }

    @PostMapping("/{campaignId}/leads")
    public String addLead(@PathVariable String campaignId, @RequestBody Map<String, String> body) {
        String number = body.get("customerNumber");
        String leadId = UUID.randomUUID().toString();
        leadStore.addLead(new Lead(leadId, campaignId, number));
        return "leadId=" + leadId;
    }

    @PostMapping("/{campaignId}/start")
    public String start(@PathVariable String campaignId,
                        @RequestParam(defaultValue = "5") int maxConcurrentCalls,
                        @RequestParam(defaultValue = "2") int callsPerSecond,
                        @RequestParam(defaultValue = "EXOTEL") String provider,
                        @RequestParam(defaultValue = "PROGRESSIVE") String mode,
                        @RequestParam(defaultValue = "1.5") double predictiveRatio) {

        engine.startCampaign(
                campaignId,
                maxConcurrentCalls,
                callsPerSecond,
                provider,
                DialMode.from(mode),
                predictiveRatio
        );
        campaignCatalogService.markRunning(
                campaignId,
                DialMode.from(mode),
                provider,
                maxConcurrentCalls,
                callsPerSecond,
                predictiveRatio
        );

        return "campaign started: " + campaignId;
    }

    @PostMapping("/{campaignId}/stop")
    public String stop(@PathVariable String campaignId) {
        engine.stopCampaign(campaignId);
        campaignCatalogService.markStopped(campaignId);
        return "campaign stopped: " + campaignId;
    }
}
