package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.campaign.DialMode;
import com.vantage.dialer.api.campaign.CampaignEngine;
import com.vantage.dialer.api.campaign.Lead;
import com.vantage.dialer.api.campaign.LeadStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private final LeadStore leadStore;
    private final CampaignEngine engine;

    public CampaignController(LeadStore leadStore, CampaignEngine engine) {
        this.leadStore = leadStore;
        this.engine = engine;
    }

    @PostMapping
    public Map<String, String> createCampaign() {
        // in MVP we just generate an id
        String campaignId = UUID.randomUUID().toString();
        return Map.of("campaignId", campaignId);
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

        return "campaign started: " + campaignId;
    }

    @PostMapping("/{campaignId}/stop")
    public String stop(@PathVariable String campaignId) {
        engine.stopCampaign(campaignId);
        return "campaign stopped: " + campaignId;
    }
}
