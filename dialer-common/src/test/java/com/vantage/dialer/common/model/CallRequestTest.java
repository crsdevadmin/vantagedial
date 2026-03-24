package com.vantage.dialer.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CallRequestTest {

    @Test
    void constructorSeedsPhoneCustomerCampaignAndAgent() {
        CallRequest request = new CallRequest("+15550001", "campaign-1", "agent-1");

        assertEquals("+15550001", request.getPhoneNumber());
        assertEquals("+15550001", request.getCustomerNumber());
        assertEquals("campaign-1", request.getCampaignId());
        assertEquals("agent-1", request.getAgentId());
    }

    @Test
    void setPhoneNumberBackfillsCustomerNumberOnlyWhenMissing() {
        CallRequest request = new CallRequest();

        request.setPhoneNumber("+15550001");
        assertEquals("+15550001", request.getPhoneNumber());
        assertEquals("+15550001", request.getCustomerNumber());

        request.setCustomerNumber("+15550099");
        request.setPhoneNumber("+15550002");
        assertEquals("+15550002", request.getPhoneNumber());
        assertEquals("+15550099", request.getCustomerNumber());
    }

    @Test
    void setCustomerNumberBackfillsPhoneNumberOnlyWhenMissing() {
        CallRequest request = new CallRequest();

        request.setCustomerNumber("+15550001");
        assertEquals("+15550001", request.getCustomerNumber());
        assertEquals("+15550001", request.getPhoneNumber());

        request.setPhoneNumber("+15550002");
        request.setCustomerNumber("+15550003");
        assertEquals("+15550002", request.getPhoneNumber());
        assertEquals("+15550003", request.getCustomerNumber());
    }

    @Test
    void customerNumberFallsBackToPhoneNumberAndOtherFieldsRoundTrip() {
        CallRequest request = new CallRequest();

        request.setPhoneNumber("+15550001");
        request.setCampaignId("campaign-1");
        request.setAgentId("agent-1");
        request.setAgentChannel("PJSIP/1001");
        request.setProvider("ASTERISK");
        request.setIvrFlowId("ivr-1");
        request.setCallMode("OUTBOUND_IVR");

        assertEquals("+15550001", request.getCustomerNumber());
        assertEquals("campaign-1", request.getCampaignId());
        assertEquals("agent-1", request.getAgentId());
        assertEquals("PJSIP/1001", request.getAgentChannel());
        assertEquals("ASTERISK", request.getProvider());
        assertEquals("ivr-1", request.getIvrFlowId());
        assertEquals("OUTBOUND_IVR", request.getCallMode());
    }
}
