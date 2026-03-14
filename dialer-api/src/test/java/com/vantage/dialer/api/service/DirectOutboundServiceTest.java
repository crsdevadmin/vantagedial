package com.vantage.dialer.api.service;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.kafka.CommandProducer;
import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.model.CallRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DirectOutboundServiceTest {

    @Test
    void queuesCallUsingRequestedAvailableAgent() {
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer producer = mock(CommandProducer.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer);

        Agent agent = new Agent("A1", "Agent 1", "PJSIP/1001", AgentStatus.BUSY);
        when(agentStore.acquireAgent("A1")).thenReturn(Optional.of(agent));

        CallRequest request = new CallRequest();
        request.setCustomerNumber("919876543210");
        request.setAgentId("A1");
        request.setProvider("ASTERISK");

        Map<String, String> response = service.queueCall(request);

        ArgumentCaptor<CommandMessage> captor = ArgumentCaptor.forClass(CommandMessage.class);
        verify(producer).sendCommand(captor.capture());

        CommandMessage command = captor.getValue();
        assertEquals("ASTERISK", command.getProvider());
        assertEquals("919876543210", command.getPayload().get("customerNumber"));
        assertEquals("A1", command.getPayload().get("agentId"));
        assertEquals("PJSIP/1001", command.getPayload().get("agentChannel"));
        assertEquals(response.get("callSessionId"), command.getCallSessionId());
    }

    @Test
    void queuesCallUsingFirstAvailableAgentWhenRequestDoesNotSpecifyOne() {
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer producer = mock(CommandProducer.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer);

        Agent agent = new Agent("A2", "Agent 2", "PJSIP/1002", AgentStatus.BUSY);
        when(agentStore.acquireAvailableAgent()).thenReturn(Optional.of(agent));

        CallRequest request = new CallRequest();
        request.setPhoneNumber("919876543211");

        Map<String, String> response = service.queueCall(request);

        assertEquals("A2", response.get("agentId"));
        assertEquals("PJSIP/1002", response.get("agentChannel"));
        assertEquals("ASTERISK", response.get("provider"));
        assertEquals("direct-outbound", response.get("campaignId"));
    }

    @Test
    void rejectsRequestWhenNoAgentIsAvailable() {
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer producer = mock(CommandProducer.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer);

        when(agentStore.acquireAvailableAgent()).thenReturn(Optional.empty());

        CallRequest request = new CallRequest();
        request.setCustomerNumber("919876543212");

        assertThrows(ResponseStatusException.class, () -> service.queueCall(request));
    }
}
