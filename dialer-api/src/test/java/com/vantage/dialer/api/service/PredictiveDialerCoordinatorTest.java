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
import static org.mockito.Mockito.mock;
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
                new Agent("A1", "Agent 1", "PJSIP/1001", AgentStatus.BUSY)
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
}
