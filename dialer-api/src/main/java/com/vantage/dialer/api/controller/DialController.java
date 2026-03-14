package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.service.DirectOutboundService;
import com.vantage.dialer.common.model.CallRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dialer")
public class DialController {

    private final DirectOutboundService directOutboundService;

    public DialController(DirectOutboundService directOutboundService) {
        this.directOutboundService = directOutboundService;
    }

    @PostMapping("/call")
    public Map<String, String> createCall(@RequestBody CallRequest request) {
        return directOutboundService.queueCall(request);
    }
}
