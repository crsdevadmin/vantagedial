package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.campaign.CampaignStats;
import com.vantage.dialer.api.campaign.Lead;
import com.vantage.dialer.api.campaign.LeadStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/campaigns")
public class CampaignQueryController {

    private final LeadStore leadStore;

    public CampaignQueryController(LeadStore leadStore) {
        this.leadStore = leadStore;
    }

    @GetMapping("/{campaignId}/leads")
    public List<Lead> getLeads(@PathVariable String campaignId) {
        return leadStore.getLeads(campaignId);
    }

    @GetMapping("/{campaignId}/stats")
    public CampaignStats getStats(@PathVariable String campaignId) {
        return leadStore.getStats(campaignId);
    }
}