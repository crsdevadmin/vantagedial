package com.vantage.dialer.worker.provider.asterisk.service;

import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.worker.core.CallEventPublisher;
import com.vantage.dialer.worker.core.CallSession;
import com.vantage.dialer.worker.core.CallSessionRegistry;
import com.vantage.dialer.worker.core.TelephonyProviderRouter;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.event.BridgeEvent;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.NewStateEvent;
import org.asteriskjava.manager.event.OriginateResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class AsteriskEventListener implements ManagerEventListener {

    private static final Logger log = LoggerFactory.getLogger(AsteriskEventListener.class);

    private final CallSessionRegistry sessionRegistry;
    private final TelephonyProviderRouter providerRouter;
    private final CallEventPublisher eventPublisher;

    public AsteriskEventListener(CallSessionRegistry sessionRegistry,
                                 TelephonyProviderRouter providerRouter,
                                 CallEventPublisher eventPublisher,
                                 AsteriskService asteriskService) {
        this.sessionRegistry = sessionRegistry;
        this.providerRouter = providerRouter;
        this.eventPublisher = eventPublisher;
        asteriskService.addEventListener(this);
    }

    @Override
    public void onManagerEvent(ManagerEvent event) {
        if (event instanceof OriginateResponseEvent responseEvent) {
            handleOriginateResponse(responseEvent);
            return;
        }
        if (event instanceof NewStateEvent stateEvent) {
            handleNewState(stateEvent);
            return;
        }
        if (event instanceof BridgeEvent bridgeEvent) {
            handleBridge(bridgeEvent);
            return;
        }
        if (event instanceof HangupEvent hangupEvent) {
            handleHangup(hangupEvent);
        }
    }

    private void handleOriginateResponse(OriginateResponseEvent event) {
        sessionRegistry.findByAction(event.getActionId()).ifPresent(session -> {
            String owner = session.getActionOwners().getOrDefault(event.getActionId(), "");
            if ("Failure".equalsIgnoreCase(event.getResponse())) {
                eventPublisher.publishFailure(session, "AMI_ORIGINATE_FAILED", event.getReason());
                sessionRegistry.remove(session.getCallSessionId());
                return;
            }

            if ("customer".equals(owner)) {
                session.setCustomerChannel(event.getChannel());
                sessionRegistry.mapChannel(event.getChannel(), session.getCallSessionId());
                eventPublisher.publish(session, EventType.CUSTOMER_RINGING);
            } else if ("agent".equals(owner)) {
                session.setAgentLiveChannel(event.getChannel());
                sessionRegistry.mapChannel(event.getChannel(), session.getCallSessionId());
                eventPublisher.publish(session, EventType.AGENT_RINGING);
            }
        });
    }

    private void handleNewState(NewStateEvent event) {
        if (!"Up".equalsIgnoreCase(String.valueOf(event.getChannelStateDesc()))) {
            return;
        }

        sessionRegistry.findByChannel(event.getChannel()).ifPresent(session -> {
            if (isSameChannel(event.getChannel(), session.getCustomerChannel()) && !session.isCustomerAnswered()) {
                session.setCustomerAnswered(true);
                eventPublisher.publish(session, EventType.CUSTOMER_ANSWERED);

                if (!session.isAgentDialRequested()
                        && session.getAgentChannel() != null
                        && !session.getAgentChannel().isBlank()) {
                    eventPublisher.publish(session, EventType.AGENT_DIALING);
                    providerRouter.resolve(session.getProvider()).startAgentLeg(session);
                }
                return;
            }

            if (isSameChannel(event.getChannel(), session.getAgentLiveChannel()) && !session.isAgentAnswered()) {
                session.setAgentAnswered(true);
                eventPublisher.publish(session, EventType.AGENT_ANSWERED);

                if (!session.isBridged() && session.isCustomerAnswered()) {
                    providerRouter.resolve(session.getProvider()).bridge(session);
                }
            }
        });
    }

    private void handleBridge(BridgeEvent event) {
        findSessionByEitherChannel(event.getChannel1(), event.getChannel2()).ifPresent(session -> {
            if (!session.isBridged()
                    && matchesSession(session, event.getChannel1())
                    && matchesSession(session, event.getChannel2())) {
                session.setBridged(true);
                eventPublisher.publish(session, EventType.CALL_BRIDGED);
            }
        });
    }

    private void handleHangup(HangupEvent event) {
        sessionRegistry.findByChannel(event.getChannel()).ifPresent(session -> {
            if (session.isTerminated()) {
                return;
            }

            session.setTerminated(true);
            if (session.isBridged() || (session.isCustomerAnswered() && session.isAgentAnswered())) {
                eventPublisher.publish(session, EventType.CALL_COMPLETED);
            } else {
                eventPublisher.publishFailure(session, hangupResult(session, event), buildHangupPayload(event));
            }
            sessionRegistry.remove(session.getCallSessionId());
        });
    }

    private boolean matchesSession(CallSession session, String channel) {
        return isSameChannel(channel, session.getCustomerChannel())
                || isSameChannel(channel, session.getAgentLiveChannel());
    }

    private Optional<CallSession> findSessionByEitherChannel(String channel1, String channel2) {
        Optional<CallSession> session = sessionRegistry.findByChannel(channel1);
        return session.isPresent() ? session : sessionRegistry.findByChannel(channel2);
    }

    private boolean isSameChannel(String left, String right) {
        String normalizedLeft = normalizeChannel(left);
        String normalizedRight = normalizeChannel(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
    }

    private String normalizeChannel(String channel) {
        if (channel == null) {
            return null;
        }

        String normalized = channel.trim();
        if (normalized.isBlank()) {
            return null;
        }

        int semicolonIndex = normalized.indexOf(';');
        if (semicolonIndex >= 0) {
            normalized = normalized.substring(0, semicolonIndex);
        }

        return normalized;
    }

    private String hangupResult(CallSession session, HangupEvent event) {
        if (isSameChannel(event.getChannel(), session.getCustomerChannel()) && !session.isCustomerAnswered()) {
            return "CUSTOMER_HANGUP_BEFORE_ANSWER";
        }
        if (isSameChannel(event.getChannel(), session.getAgentLiveChannel()) && !session.isAgentAnswered()) {
            return "AGENT_HANGUP_BEFORE_ANSWER";
        }
        return "CALL_TERMINATED_BEFORE_BRIDGE";
    }

    private Map<String, Object> buildHangupPayload(HangupEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", event.getChannel());
        payload.put("cause", event.getCause());
        payload.put("causeTxt", event.getCauseTxt());
        return payload;
    }
}
