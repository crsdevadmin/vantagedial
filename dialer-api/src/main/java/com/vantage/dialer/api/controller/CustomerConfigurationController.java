package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.CustomerConfigurationRequest;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import com.vantage.dialer.api.service.CustomerConfigurationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerConfigurationController {

    private final CustomerConfigurationService service;

    public CustomerConfigurationController(CustomerConfigurationService service) {
        this.service = service;
    }

    @PostMapping
    public CustomerConfigurationResponse create(@RequestBody CustomerConfigurationRequest request) {
        return service.createOrUpdate(request);
    }

    @PutMapping("/{customerId}")
    public CustomerConfigurationResponse update(@PathVariable String customerId, @RequestBody CustomerConfigurationRequest request) {
        request.setCustomerId(customerId);
        return service.createOrUpdate(request);
    }

    @GetMapping
    public List<CustomerConfigurationResponse> list() {
        return service.list();
    }

    @GetMapping("/{customerId}")
    public CustomerConfigurationResponse get(@PathVariable String customerId) {
        return service.find(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown customer: " + customerId));
    }

    @GetMapping("/proposal-presets")
    public List<ProposalPresetResponse> proposalPresets() {
        return service.listProposalPresets();
    }
}
