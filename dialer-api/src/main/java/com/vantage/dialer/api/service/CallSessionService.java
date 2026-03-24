package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.CallSessionResponse;
import com.vantage.dialer.api.persistence.model.CallSessionEntity;
import com.vantage.dialer.api.persistence.repository.CallSessionRepository;
import com.vantage.dialer.common.model.CallMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CallSessionService {

    private final CallSessionRepository callSessionRepository;

    public CallSessionService(CallSessionRepository callSessionRepository) {
        this.callSessionRepository = callSessionRepository;
    }

    @Transactional
    public void createQueuedSession(String callSessionId,
                                    String campaignId,
                                    String leadId,
                                    String provider,
                                    String customerNumber,
                                    String agentId,
                                    String agentChannel,
                                    CallMode callMode,
                                    String ivrFlowId) {
        CallSessionEntity session = new CallSessionEntity();
        session.setCallSessionId(callSessionId);
        session.setCampaignId(campaignId);
        session.setLeadId(leadId);
        session.setProvider(provider);
        session.setCustomerNumber(customerNumber);
        session.setAgentId(agentId);
        session.setAgentChannel(agentChannel);
        session.setCallMode(callMode.name());
        session.setIvrFlowId(ivrFlowId);
        session.setStatus("QUEUED");
        session.setLastEventType("QUEUED");
        session.setLastEventAt(Instant.now());
        callSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<CallSessionResponse> getCallSession(String callSessionId) {
        return callSessionRepository.findById(callSessionId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CallSessionResponse> getCampaignSessions(String campaignId) {
        return callSessionRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CallSessionResponse toResponse(CallSessionEntity entity) {
        return new CallSessionResponse(
                entity.getCallSessionId(),
                entity.getCampaignId(),
                entity.getLeadId(),
                entity.getProvider(),
                entity.getCustomerNumber(),
                entity.getAgentId(),
                entity.getAgentChannel(),
                entity.getCallMode(),
                entity.getIvrFlowId(),
                entity.getStatus(),
                entity.getLastEventType(),
                entity.getLastEventAt(),
                entity.getCreatedAt()
        );
    }
}
