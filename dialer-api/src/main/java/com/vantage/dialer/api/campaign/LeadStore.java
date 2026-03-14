package com.vantage.dialer.api.campaign;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class LeadStore {

    private final Map<String, List<Lead>> leadsByCampaign = new ConcurrentHashMap<>();

    public void addLead(Lead lead) {
        leadsByCampaign
                .computeIfAbsent(lead.getCampaignId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(lead);
    }

    public List<Lead> getNextNewLeads(String campaignId, int limit) {
        List<Lead> leads = leadsByCampaign.getOrDefault(campaignId, List.of());

        return leads.stream()
                .filter(l -> l.getStatus() == LeadStatus.NEW)
                .filter(l -> l.getAttempts() < 3)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void updateStatus(String campaignId, String leadId, LeadStatus status) {
        List<Lead> leads = leadsByCampaign.getOrDefault(campaignId, List.of());
        for (Lead lead : leads) {
            if (lead.getLeadId().equals(leadId)) {
                lead.setStatus(status);
                return;
            }
        }
    }

    public void incrementAttempts(String campaignId, String leadId) {
        List<Lead> leads = leadsByCampaign.getOrDefault(campaignId, List.of());
        for (Lead lead : leads) {
            if (lead.getLeadId().equals(leadId)) {
                lead.incrementAttempts();
                return;
            }
        }
    }

    public long countActive(String campaignId) {
        List<Lead> leads = leadsByCampaign.getOrDefault(campaignId, List.of());
        return leads.stream()
                .filter(l -> l.getStatus() == LeadStatus.QUEUED || l.getStatus() == LeadStatus.IN_PROGRESS)
                .count();
    }
    public java.util.List<Lead> getLeads(String campaignId) {
        return new java.util.ArrayList<>(leadsByCampaign.getOrDefault(campaignId, java.util.List.of()));
    }

    public CampaignStats getStats(String campaignId) {
        java.util.List<Lead> leads = leadsByCampaign.getOrDefault(campaignId, java.util.List.of());

        CampaignStats stats = new CampaignStats();
        stats.setTotal(leads.size());
        stats.setNewCount(leads.stream().filter(l -> l.getStatus() == LeadStatus.NEW).count());
        stats.setQueued(leads.stream().filter(l -> l.getStatus() == LeadStatus.QUEUED).count());
        stats.setInProgress(leads.stream().filter(l -> l.getStatus() == LeadStatus.IN_PROGRESS).count());
        stats.setCompleted(leads.stream().filter(l -> l.getStatus() == LeadStatus.COMPLETED).count());
        stats.setFailed(leads.stream().filter(l -> l.getStatus() == LeadStatus.FAILED).count());

        return stats;
    }
}