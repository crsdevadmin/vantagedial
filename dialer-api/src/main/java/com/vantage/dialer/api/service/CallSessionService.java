package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.CallSessionResponse;
import com.vantage.dialer.api.dto.OperatorWrapUpRequest;
import com.vantage.dialer.api.dto.OperatorWrapUpResponse;
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

    @Transactional
    public OperatorWrapUpResponse saveOperatorWrapUp(String callSessionId, OperatorWrapUpRequest request) {
        String normalizedCallSessionId = normalize(callSessionId);
        if (normalizedCallSessionId == null) {
            throw new IllegalArgumentException("callSessionId is required");
        }

        Instant now = Instant.now();
        CallSessionEntity session = callSessionRepository.findById(normalizedCallSessionId)
                .orElseGet(() -> createSoftphoneSession(normalizedCallSessionId, request, now));

        session.setCampaignId(fallback(request.getCampaignId(), session.getCampaignId()));
        session.setCustomerNumber(fallback(request.getCustomerNumber(), session.getCustomerNumber()));
        session.setAgentId(fallback(request.getAgentId(), session.getAgentId()));
        session.setCallDirection(normalize(request.getCallDirection()));
        session.setUiCallStatus(normalize(request.getCallStatus()));
        session.setOperatorDisposition(normalize(request.getDisposition()));
        session.setOperatorNotes(normalize(request.getNotes()));
        session.setOperatorPriority(normalize(request.getPriority()));
        session.setFollowUpAt(request.getFollowUpAt());
        session.setStatus(fallback(uppercase(request.getCallStatus()), session.getStatus()));
        session.setLastEventType("OPERATOR_WRAP_UP");
        session.setLastEventAt(now);
        session.setWrapUpUpdatedAt(now);

        return toWrapUpResponse(callSessionRepository.save(session));
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
                entity.getCreatedAt(),
                entity.getOperatorDisposition(),
                entity.getOperatorNotes(),
                entity.getOperatorPriority(),
                entity.getFollowUpAt(),
                entity.getWrapUpUpdatedAt()
        );
    }

    private CallSessionEntity createSoftphoneSession(String callSessionId, OperatorWrapUpRequest request, Instant now) {
        CallSessionEntity session = new CallSessionEntity();
        session.setCallSessionId(callSessionId);
        session.setCampaignId(normalize(request.getCampaignId()));
        session.setProvider(fallback(request.getProvider(), "SOFTPHONE"));
        session.setCustomerNumber(fallback(request.getCustomerNumber(), "UNKNOWN"));
        session.setAgentId(normalize(request.getAgentId()));
        session.setCallMode(fallback(uppercase(request.getCallMode()), "AGENT_SOFTPHONE"));
        session.setStatus(fallback(uppercase(request.getCallStatus()), "WRAP_UP"));
        session.setLastEventType("OPERATOR_WRAP_UP");
        session.setLastEventAt(now);
        return session;
    }

    private OperatorWrapUpResponse toWrapUpResponse(CallSessionEntity entity) {
        return new OperatorWrapUpResponse(
                entity.getCallSessionId(),
                entity.getCampaignId(),
                entity.getCustomerNumber(),
                entity.getAgentId(),
                entity.getOperatorDisposition(),
                entity.getOperatorNotes(),
                entity.getOperatorPriority(),
                entity.getFollowUpAt(),
                entity.getWrapUpUpdatedAt()
        );
    }

    private String fallback(String value, String fallback) {
        String normalized = normalize(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String uppercase(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
