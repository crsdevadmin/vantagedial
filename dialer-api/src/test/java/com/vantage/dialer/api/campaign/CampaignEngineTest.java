package com.vantage.dialer.api.campaign;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.kafka.Topics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignEngineTest {

    @Test
    void tickQueuesProgressiveCallWithReservedAgentContext() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, CommandMessage> kafkaTemplate = mock(KafkaTemplate.class);
        CampaignEngine engine = new CampaignEngine(leadStore, agentStore, kafkaTemplate);

        Lead lead = new Lead("lead-1", "campaign-1", "+15551234567");
        when(leadStore.countActive("campaign-1")).thenReturn(0L);
        when(agentStore.countAvailable()).thenReturn(2L);
        when(leadStore.getNextNewLeads("campaign-1", 1)).thenReturn(List.of(lead));
        when(agentStore.acquireAvailableAgent()).thenReturn(Optional.of(
                new Agent("agent-1", "Agent 1", "PJSIP/1001", "1001", "1001", "pw", AgentStatus.BUSY)
        ));

        invokeTick(engine, new CampaignExecution("campaign-1", 1, 1, "ASTERISK", DialMode.PROGRESSIVE, 1.0));

        verify(leadStore).incrementAttempts("campaign-1", "lead-1");
        verify(leadStore).updateStatus("campaign-1", "lead-1", LeadStatus.QUEUED);

        ArgumentCaptor<CommandMessage> commandCaptor = ArgumentCaptor.forClass(CommandMessage.class);
        verify(kafkaTemplate).send(eq(Topics.COMMANDS), anyString(), commandCaptor.capture());
        CommandMessage command = commandCaptor.getValue();

        assertEquals(CommandType.START_CUSTOMER_CALL, command.getCommandType());
        assertEquals("ASTERISK", command.getProvider());
        assertEquals("campaign-1", command.getPayload().get("campaignId"));
        assertEquals("lead-1", command.getPayload().get("leadId"));
        assertEquals("+15551234567", command.getPayload().get("customerNumber"));
        assertEquals("PROGRESSIVE", command.getPayload().get("dialMode"));
        assertEquals("agent-1", command.getPayload().get("agentId"));
        assertEquals("PJSIP/1001", command.getPayload().get("agentChannel"));
    }

    @Test
    void tickSkipsPredictiveQueueingWhenNoAgentsAreAvailable() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, CommandMessage> kafkaTemplate = mock(KafkaTemplate.class);
        CampaignEngine engine = new CampaignEngine(leadStore, agentStore, kafkaTemplate);

        when(leadStore.countActive("campaign-2")).thenReturn(0L);
        when(agentStore.countAvailable()).thenReturn(0L);

        invokeTick(engine, new CampaignExecution("campaign-2", 3, 3, "ASTERISK", DialMode.PREDICTIVE, 2.0));

        verify(leadStore, never()).getNextNewLeads(anyString(), org.mockito.ArgumentMatchers.anyInt());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), org.mockito.ArgumentMatchers.any(CommandMessage.class));
    }

    @Test
    void tickQueuesPredictiveCallWithoutReservingSpecificAgent() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, CommandMessage> kafkaTemplate = mock(KafkaTemplate.class);
        CampaignEngine engine = new CampaignEngine(leadStore, agentStore, kafkaTemplate);

        Lead lead = new Lead("lead-2", "campaign-3", "+15557654321");
        when(leadStore.countActive("campaign-3")).thenReturn(0L);
        when(agentStore.countAvailable()).thenReturn(2L);
        when(leadStore.getNextNewLeads("campaign-3", 3)).thenReturn(List.of(lead));

        invokeTick(engine, new CampaignExecution("campaign-3", 3, 3, "ASTERISK", DialMode.PREDICTIVE, 1.5));

        ArgumentCaptor<CommandMessage> commandCaptor = ArgumentCaptor.forClass(CommandMessage.class);
        verify(kafkaTemplate).send(eq(Topics.COMMANDS), anyString(), commandCaptor.capture());
        verify(agentStore, never()).acquireAvailableAgent();

        CommandMessage command = commandCaptor.getValue();
        assertEquals("PREDICTIVE", command.getPayload().get("dialMode"));
        assertFalse(command.getPayload().containsKey("agentId"));
        assertFalse(command.getPayload().containsKey("agentChannel"));
    }

    @Test
    void tickSkipsWhenActiveCallsAlreadyFillCampaignCapacity() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, CommandMessage> kafkaTemplate = mock(KafkaTemplate.class);
        CampaignEngine engine = new CampaignEngine(leadStore, agentStore, kafkaTemplate);

        when(leadStore.countActive("campaign-4")).thenReturn(2L);

        invokeTick(engine, new CampaignExecution("campaign-4", 2, 5, "ASTERISK", DialMode.PROGRESSIVE, 1.0));

        verify(agentStore, never()).countAvailable();
        verify(leadStore, never()).getNextNewLeads(anyString(), org.mockito.ArgumentMatchers.anyInt());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), org.mockito.ArgumentMatchers.any(CommandMessage.class));
    }

    @Test
    void tickSkipsWhenCallsPerSecondBudgetIsAlreadyConsumedForCurrentSecond() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, CommandMessage> kafkaTemplate = mock(KafkaTemplate.class);
        CampaignEngine engine = new CampaignEngine(leadStore, agentStore, kafkaTemplate);

        when(leadStore.countActive("campaign-5")).thenReturn(0L);
        seedRateLimitState(engine, "campaign-5", System.currentTimeMillis() / 1000, 1);

        invokeTick(engine, new CampaignExecution("campaign-5", 3, 1, "ASTERISK", DialMode.PREDICTIVE, 2.0));

        verify(agentStore, never()).countAvailable();
        verify(leadStore, never()).getNextNewLeads(anyString(), org.mockito.ArgumentMatchers.anyInt());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), org.mockito.ArgumentMatchers.any(CommandMessage.class));
    }

    @Test
    void progressiveTickStopsQueueingWhenASecondAgentCannotBeReserved() throws Exception {
        LeadStore leadStore = mock(LeadStore.class);
        AgentStore agentStore = mock(AgentStore.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, CommandMessage> kafkaTemplate = mock(KafkaTemplate.class);
        CampaignEngine engine = new CampaignEngine(leadStore, agentStore, kafkaTemplate);

        Lead firstLead = new Lead("lead-6a", "campaign-6", "+155500001");
        Lead secondLead = new Lead("lead-6b", "campaign-6", "+155500002");
        when(leadStore.countActive("campaign-6")).thenReturn(0L);
        when(agentStore.countAvailable()).thenReturn(2L);
        when(leadStore.getNextNewLeads("campaign-6", 2)).thenReturn(List.of(firstLead, secondLead));
        when(agentStore.acquireAvailableAgent()).thenReturn(
                Optional.of(new Agent("agent-6a", "Agent 6A", "PJSIP/1061", "1061", "1061", "pw", AgentStatus.BUSY)),
                Optional.empty()
        );

        invokeTick(engine, new CampaignExecution("campaign-6", 2, 2, "ASTERISK", DialMode.PROGRESSIVE, 1.0));

        verify(leadStore).incrementAttempts("campaign-6", "lead-6a");
        verify(leadStore).updateStatus("campaign-6", "lead-6a", LeadStatus.QUEUED);
        verify(leadStore, never()).incrementAttempts("campaign-6", "lead-6b");
        verify(leadStore, never()).updateStatus("campaign-6", "lead-6b", LeadStatus.QUEUED);
        verify(kafkaTemplate).send(eq(Topics.COMMANDS), anyString(), org.mockito.ArgumentMatchers.any(CommandMessage.class));
    }

    private void invokeTick(CampaignEngine engine, CampaignExecution execution) throws Exception {
        Method tick = CampaignEngine.class.getDeclaredMethod("tick", CampaignExecution.class);
        tick.setAccessible(true);
        tick.invoke(engine, execution);
    }

    @SuppressWarnings("unchecked")
    private void seedRateLimitState(CampaignEngine engine, String campaignId, long second, int callsThisSecond) throws Exception {
        Field lastSecondField = CampaignEngine.class.getDeclaredField("lastSecond");
        lastSecondField.setAccessible(true);
        ((Map<String, Long>) lastSecondField.get(engine)).put(campaignId, second);

        Field callsThisSecondField = CampaignEngine.class.getDeclaredField("callsThisSecond");
        callsThisSecondField.setAccessible(true);
        ((Map<String, Integer>) callsThisSecondField.get(engine)).put(campaignId, callsThisSecond);
    }
}
