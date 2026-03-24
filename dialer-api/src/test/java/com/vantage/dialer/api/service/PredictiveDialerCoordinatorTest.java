package com.vantage.dialer.api.service;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.campaign.CampaignEngine;
import com.vantage.dialer.api.campaign.CampaignExecution;
import com.vantage.dialer.api.campaign.DialMode;
import com.vantage.dialer.api.kafka.CommandProducer;
import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.events.EventType;
import com.vantage.dialer.common.events.StandardEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PredictiveDialerCoordinatorTest {

    @Test
    void queuesDialAgentCommandWhenPredictiveCampaignCustomerAnswers() {
        CampaignEngine campaignEngine = mock(CampaignEngine.class);
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer commandProducer = mock(CommandProducer.class);
        PredictiveDialerCoordinator coordinator = new PredictiveDialerCoordinator(campaignEngine, agentStore, commandProducer);

        when(campaignEngine.getExecution("campaign-1")).thenReturn(Optional.of(
                new CampaignExecution("campaign-1", 5, 2, "ASTERISK", DialMode.PREDICTIVE, 1.5)
        ));
        when(agentStore.acquireAvailableAgent()).thenReturn(Optional.of(
                new Agent("A1", "Agent 1", "PJSIP/1001", "1001", "1001", "StrongPassword1001", AgentStatus.BUSY)
        ));

        StandardEvent event = new StandardEvent();
        event.setCallSessionId("session-1");
        event.setEventType(EventType.CUSTOMER_ANSWERED);
        event.setProvider("ASTERISK");
        event.setTimestamp(Instant.now());
        event.setPayload(Map.of("campaignId", "campaign-1", "leadId", "lead-1"));

        coordinator.queueAnsweredCall(event);

        ArgumentCaptor<CommandMessage> captor = ArgumentCaptor.forClass(CommandMessage.class);
        verify(commandProducer).sendCommand(captor.capture());

        CommandMessage command = captor.getValue();
        assertEquals(CommandType.DIAL_AGENT, command.getCommandType());
        assertEquals("session-1", command.getCallSessionId());
        assertEquals("A1", command.getPayload().get("agentId"));
        assertEquals("PJSIP/1001", command.getPayload().get("agentChannel"));
    }

    @Test
    void ignoresAnsweredCallWhenCampaignIsNotPredictive() {
        CampaignEngine campaignEngine = mock(CampaignEngine.class);
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer commandProducer = mock(CommandProducer.class);
        PredictiveDialerCoordinator coordinator = new PredictiveDialerCoordinator(campaignEngine, agentStore, commandProducer);

        when(campaignEngine.getExecution("campaign-2")).thenReturn(Optional.of(
                new CampaignExecution("campaign-2", 5, 2, "ASTERISK", DialMode.PROGRESSIVE, 1.0)
        ));

        coordinator.queueAnsweredCall(answeredEvent("session-2", "campaign-2", "lead-2"));

        verify(commandProducer, never()).sendCommand(org.mockito.ArgumentMatchers.any());
        verify(agentStore, never()).acquireAvailableAgent();
    }

    @Test
    void keepsAnsweredCallQueuedUntilAnAgentBecomesAvailable() {
        CampaignEngine campaignEngine = mock(CampaignEngine.class);
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer commandProducer = mock(CommandProducer.class);
        PredictiveDialerCoordinator coordinator = new PredictiveDialerCoordinator(campaignEngine, agentStore, commandProducer);
        Agent agent = new Agent("A2", "Agent 2", "PJSIP/1002", "1002", "1002", "StrongPassword1002", AgentStatus.BUSY);

        when(campaignEngine.getExecution("campaign-3")).thenReturn(Optional.of(
                new CampaignExecution("campaign-3", 5, 2, "ASTERISK", DialMode.PREDICTIVE, 1.5)
        ));
        when(agentStore.acquireAvailableAgent()).thenReturn(Optional.empty(), Optional.of(agent), Optional.empty());

        coordinator.queueAnsweredCall(answeredEvent("session-3", "campaign-3", "lead-3"));
        verify(commandProducer, never()).sendCommand(org.mockito.ArgumentMatchers.any());

        coordinator.dispatchWaitingCalls("campaign-3");

        ArgumentCaptor<CommandMessage> captor = ArgumentCaptor.forClass(CommandMessage.class);
        verify(commandProducer).sendCommand(captor.capture());
        CommandMessage command = captor.getValue();
        assertEquals("session-3", command.getCallSessionId());
        assertEquals("lead-3", command.getPayload().get("leadId"));
        assertEquals("A2", command.getPayload().get("agentId"));
    }

    @Test
    void dispatchesWaitingCallsInOrderUntilAgentsRunOut() {
        CampaignEngine campaignEngine = mock(CampaignEngine.class);
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer commandProducer = mock(CommandProducer.class);
        PredictiveDialerCoordinator coordinator = new PredictiveDialerCoordinator(campaignEngine, agentStore, commandProducer);
        Agent firstAgent = new Agent("A1", "Agent 1", "PJSIP/1001", "1001", "1001", "StrongPassword1001", AgentStatus.BUSY);
        Agent secondAgent = new Agent("A2", "Agent 2", "PJSIP/1002", "1002", "1002", "StrongPassword1002", AgentStatus.BUSY);

        when(campaignEngine.getExecution("campaign-4")).thenReturn(Optional.of(
                new CampaignExecution("campaign-4", 5, 2, "ASTERISK", DialMode.PREDICTIVE, 1.8)
        ));
        when(agentStore.acquireAvailableAgent()).thenReturn(
                Optional.empty(),
                Optional.empty(),
                Optional.of(firstAgent),
                Optional.of(secondAgent),
                Optional.empty()
        );

        coordinator.queueAnsweredCall(answeredEvent("session-4a", "campaign-4", "lead-4a"));
        coordinator.queueAnsweredCall(answeredEvent("session-4b", "campaign-4", "lead-4b"));

        coordinator.dispatchWaitingCalls("campaign-4");

        ArgumentCaptor<CommandMessage> captor = ArgumentCaptor.forClass(CommandMessage.class);
        verify(commandProducer, times(2)).sendCommand(captor.capture());
        assertEquals(
                java.util.List.of("session-4a", "session-4b"),
                captor.getAllValues().stream().map(CommandMessage::getCallSessionId).toList()
        );
        assertEquals(
                java.util.List.of("A1", "A2"),
                captor.getAllValues().stream().map(command -> command.getPayload().get("agentId")).toList()
        );
        assertEquals(
                java.util.List.of("lead-4a", "lead-4b"),
                captor.getAllValues().stream().map(command -> command.getPayload().get("leadId")).toList()
        );
    }

    private StandardEvent answeredEvent(String callSessionId, String campaignId, String leadId) {
        StandardEvent event = new StandardEvent();
        event.setCallSessionId(callSessionId);
        event.setEventType(EventType.CUSTOMER_ANSWERED);
        event.setProvider("ASTERISK");
        event.setTimestamp(Instant.now());
        event.setPayload(Map.of("campaignId", campaignId, "leadId", leadId));
        return event;
    }
}
