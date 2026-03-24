package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.service.DirectOutboundService;
import com.vantage.dialer.common.model.CallRequest;
import com.vantage.dialer.common.model.CallMode;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/outbound")
public class OutboundController {

    private final DirectOutboundService directOutboundService;

    public OutboundController(DirectOutboundService directOutboundService) {
        this.directOutboundService = directOutboundService;
    }

    @PostMapping("/start")
    public Map<String, String> startCall(@RequestBody CallRequest request) {
        return directOutboundService.queueCall(request);
    }

    @PostMapping("/test-call")
    public Map<String, String> triggerTestCall(@RequestBody Map<String, String> request) {
        CallRequest callRequest = new CallRequest();
        callRequest.setCustomerNumber(request.get("customerNumber"));
        callRequest.setCampaignId("test-call");
        callRequest.setProvider("ASTERISK");
        return directOutboundService.queueCall(callRequest);
    }

    @PostMapping("/ivr/start")
    public Map<String, String> startIvr(@RequestBody CallRequest request) {
        request.setCallMode(CallMode.OUTBOUND_IVR.name());
        return directOutboundService.queueCall(request);
    }
}
