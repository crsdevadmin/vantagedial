package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.IvrFlowRequest;
import com.vantage.dialer.api.dto.IvrFlowResponse;
import com.vantage.dialer.api.service.IvrFlowService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ivr/flows")
public class IvrController {

    private final IvrFlowService ivrFlowService;

    public IvrController(IvrFlowService ivrFlowService) {
        this.ivrFlowService = ivrFlowService;
    }

    @PostMapping
    public IvrFlowResponse createFlow(@RequestBody IvrFlowRequest request) {
        return ivrFlowService.createFlow(request);
    }

    @GetMapping("/{ivrFlowId}")
    public IvrFlowResponse getFlow(@PathVariable String ivrFlowId) {
        return ivrFlowService.getFlow(ivrFlowId);
    }
}
