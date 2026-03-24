package com.vantage.dialer.api.campaign;

import com.vantage.dialer.api.persistence.model.LeadEntity;
import com.vantage.dialer.api.persistence.repository.LeadRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Component
public class LeadStore {

    private final LeadRepository leadRepository;

    public LeadStore(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Transactional
    public void addLead(Lead lead) {
        leadRepository.save(toEntity(lead));
    }

    @Transactional(readOnly = true)
    public List<Lead> getNextNewLeads(String campaignId, int limit) {
        return leadRepository.findByCampaignIdAndStatusOrderByCreatedAtAsc(campaignId, LeadStatus.NEW).stream()
                .filter(lead -> lead.getAttempts() < 3)
                .limit(limit)
                .map(this::toDomain)
                .toList();
    }

    @Transactional
    public void updateStatus(String campaignId, String leadId, LeadStatus status) {
        leadRepository.findById(leadId)
                .filter(lead -> campaignId.equals(lead.getCampaignId()))
                .ifPresent(lead -> {
                    lead.setStatus(status);
                    leadRepository.save(lead);
                });
    }

    @Transactional
    public void incrementAttempts(String campaignId, String leadId) {
        leadRepository.findById(leadId)
                .filter(lead -> campaignId.equals(lead.getCampaignId()))
                .ifPresent(lead -> {
                    lead.setAttempts(lead.getAttempts() + 1);
                    leadRepository.save(lead);
                });
    }

    @Transactional(readOnly = true)
    public long countActive(String campaignId) {
        return leadRepository.countByCampaignIdAndStatusIn(
                campaignId,
                EnumSet.of(LeadStatus.QUEUED, LeadStatus.IN_PROGRESS)
        );
    }

    @Transactional(readOnly = true)
    public List<Lead> getLeads(String campaignId) {
        return leadRepository.findByCampaignIdOrderByCreatedAtAsc(campaignId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Lead> getLead(String leadId) {
        return leadRepository.findById(leadId).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public CampaignStats getStats(String campaignId) {
        List<LeadEntity> leads = leadRepository.findByCampaignIdOrderByCreatedAtAsc(campaignId);

        CampaignStats stats = new CampaignStats();
        stats.setTotal(leads.size());
        stats.setNewCount(leads.stream().filter(l -> l.getStatus() == LeadStatus.NEW).count());
        stats.setQueued(leads.stream().filter(l -> l.getStatus() == LeadStatus.QUEUED).count());
        stats.setInProgress(leads.stream().filter(l -> l.getStatus() == LeadStatus.IN_PROGRESS).count());
        stats.setCompleted(leads.stream().filter(l -> l.getStatus() == LeadStatus.COMPLETED).count());
        stats.setFailed(leads.stream().filter(l -> l.getStatus() == LeadStatus.FAILED).count());
        return stats;
    }

    private LeadEntity toEntity(Lead lead) {
        LeadEntity entity = new LeadEntity();
        entity.setLeadId(lead.getLeadId());
        entity.setCampaignId(lead.getCampaignId());
        entity.setCustomerNumber(lead.getCustomerNumber());
        entity.setStatus(lead.getStatus());
        entity.setAttempts(lead.getAttempts());
        return entity;
    }

    private Lead toDomain(LeadEntity entity) {
        Lead lead = new Lead(entity.getLeadId(), entity.getCampaignId(), entity.getCustomerNumber());
        lead.setStatus(entity.getStatus());
        lead.setAttempts(entity.getAttempts());
        return lead;
    }
}
