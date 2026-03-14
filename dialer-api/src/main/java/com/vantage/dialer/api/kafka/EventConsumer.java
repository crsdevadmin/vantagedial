package com.vantage.dialer.api.kafka;

import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.campaign.LeadStatus;
import com.vantage.dialer.api.campaign.LeadStore;
import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import com.vantage.dialer.common.kafka.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EventConsumer {

    private final LeadStore leadStore;
    private final AgentStore agentStore;

    public EventConsumer(LeadStore leadStore, AgentStore agentStore) {
        this.leadStore = leadStore;
        this.agentStore = agentStore;
    }

    @KafkaListener(topics = Topics.EVENTS, groupId = "dialer-api-events")
    public void onEvent(StandardEvent event) {

        Object campaignIdObj = event.getPayload().get("campaignId");
        Object leadIdObj = event.getPayload().get("leadId");

        if (campaignIdObj == null || leadIdObj == null) {
            return;
        }

        String campaignId = String.valueOf(campaignIdObj);
        String leadId = String.valueOf(leadIdObj);
        Object agentIdObj = event.getPayload().get("agentId");
        String agentId = agentIdObj == null ? null : String.valueOf(agentIdObj);

        if (event.getEventType() == EventType.CALL_CREATED) {
            leadStore.updateStatus(campaignId, leadId, LeadStatus.IN_PROGRESS);

        } else if (event.getEventType() == EventType.CALL_COMPLETED) {
            leadStore.updateStatus(campaignId, leadId, LeadStatus.COMPLETED);
            releaseAgent(agentId);

        } else if (event.getEventType() == EventType.CALL_FAILED) {
            var leads = leadStore.getLeads(campaignId);
            for (var lead : leads) {
                if (lead.getLeadId().equals(leadId)) {
                    if (lead.getAttempts() >= 3) {
                        leadStore.updateStatus(campaignId, leadId, LeadStatus.FAILED);
                    } else {
                        leadStore.updateStatus(campaignId, leadId, LeadStatus.NEW);
                    }
                    break;
                }
            }
            releaseAgent(agentId);
        }

        System.out.println("[API] event=" + event.getEventType()
                + " campaignId=" + campaignId
                + " leadId=" + leadId
                + " session=" + event.getCallSessionId());
    }

    private void releaseAgent(String agentId) {
        if (agentId != null && !agentId.isBlank()) {
            agentStore.releaseAgent(agentId);
        }
    }
}
