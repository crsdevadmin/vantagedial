package com.vantage.dialer.api.service;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.kafka.CommandProducer;
import com.vantage.dialer.common.commands.CommandMessage;
import com.vantage.dialer.common.commands.CommandType;
import com.vantage.dialer.common.model.CallMode;
import com.vantage.dialer.common.model.CallRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;

class DirectOutboundServiceTest {

    @Test
    void queuesCallUsingRequestedAvailableAgent() {
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer producer = mock(CommandProducer.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer, callSessionService);

        Agent agent = new Agent("A1", "Agent 1", "PJSIP/1001", "1001", "1001", "StrongPassword1001", AgentStatus.BUSY);
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
        CallSessionService callSessionService = mock(CallSessionService.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer, callSessionService);

        Agent agent = new Agent("A2", "Agent 2", "PJSIP/1002", "1002", "1002", "StrongPassword1002", AgentStatus.BUSY);
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
        CallSessionService callSessionService = mock(CallSessionService.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer, callSessionService);

        when(agentStore.acquireAvailableAgent()).thenReturn(Optional.empty());

        CallRequest request = new CallRequest();
        request.setCustomerNumber("919876543212");

        assertThrows(ResponseStatusException.class, () -> service.queueCall(request));
    }

    @Test
    void queuesOutboundIvrWithoutReservingAgent() {
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer producer = mock(CommandProducer.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer, callSessionService);

        CallRequest request = new CallRequest();
        request.setCustomerNumber("919876543213");
        request.setProvider("ASTERISK");
        request.setCallMode("OUTBOUND_IVR");
        request.setIvrFlowId("ivr-1");

        Map<String, String> response = service.queueCall(request);

        ArgumentCaptor<CommandMessage> captor = ArgumentCaptor.forClass(CommandMessage.class);
        verify(producer).sendCommand(captor.capture());
        verify(agentStore, never()).acquireAvailableAgent();

        CommandMessage command = captor.getValue();
        assertEquals(CommandType.START_IVR_CALL, command.getCommandType());
        assertEquals("OUTBOUND_IVR", response.get("callMode"));
        assertEquals("ivr-1", command.getPayload().get("ivrFlowId"));
    }

    @Test
    void rejectsBlankCustomerNumber() {
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer producer = mock(CommandProducer.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer, callSessionService);

        CallRequest request = new CallRequest();
        request.setCustomerNumber("   ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.queueCall(request));

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("customerNumber is required"));
        verify(producer, never()).sendCommand(org.mockito.ArgumentMatchers.any());
        verify(callSessionService, never()).createQueuedSession(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(CallMode.class),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void rejectsRequestedAgentWhenItIsNotAvailable() {
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer producer = mock(CommandProducer.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer, callSessionService);

        when(agentStore.acquireAgent("A9")).thenReturn(Optional.empty());

        CallRequest request = new CallRequest();
        request.setCustomerNumber("919876543219");
        request.setAgentId("A9");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.queueCall(request));

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Requested agent is not available: A9"));
        verify(producer, never()).sendCommand(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usesProvidedAgentChannelWithoutReservingAgentAndPersistsQueuedSession() {
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer producer = mock(CommandProducer.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer, callSessionService);

        CallRequest request = new CallRequest();
        request.setCustomerNumber("919876543214");
        request.setCampaignId("manual-campaign");
        request.setProvider("ASTERISK");
        request.setAgentId("A7");
        request.setAgentChannel("PJSIP/1007");

        Map<String, String> response = service.queueCall(request);

        verify(agentStore, never()).acquireAvailableAgent();
        verify(agentStore, never()).acquireAgent("A7");
        verify(callSessionService).createQueuedSession(
                eq(response.get("callSessionId")),
                eq("manual-campaign"),
                eq(response.get("callSessionId")),
                eq("ASTERISK"),
                eq("919876543214"),
                eq("A7"),
                eq("PJSIP/1007"),
                eq(CallMode.AGENT_ASSISTED),
                org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void defaultsProviderAndCampaignForIvrCallsAndPersistsQueuedSession() {
        AgentStore agentStore = mock(AgentStore.class);
        CommandProducer producer = mock(CommandProducer.class);
        CallSessionService callSessionService = mock(CallSessionService.class);
        DirectOutboundService service = new DirectOutboundService(agentStore, producer, callSessionService);

        CallRequest request = new CallRequest();
        request.setCustomerNumber(" 919876543215 ");
        request.setProvider("   ");
        request.setCampaignId("   ");
        request.setCallMode("outbound_ivr");
        request.setIvrFlowId("ivr-9");

        Map<String, String> response = service.queueCall(request);

        assertEquals("queued", response.get("status"));
        assertEquals("ASTERISK", response.get("provider"));
        assertEquals("direct-outbound", response.get("campaignId"));
        assertEquals("OUTBOUND_IVR", response.get("callMode"));
        verify(callSessionService).createQueuedSession(
                eq(response.get("callSessionId")),
                eq("direct-outbound"),
                eq(response.get("callSessionId")),
                eq("ASTERISK"),
                eq("919876543215"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                eq(CallMode.OUTBOUND_IVR),
                eq("ivr-9")
        );
    }
}
