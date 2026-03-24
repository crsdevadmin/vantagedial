package com.vantage.dialer.api.controller;

import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CostEstimateResponse;
import com.vantage.dialer.api.dto.CostConfigurationRequest;
import com.vantage.dialer.api.dto.CostConfigurationResponse;
import com.vantage.dialer.api.service.PricingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pricing")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/estimate")
    public CostEstimateResponse estimate(@RequestBody CostEstimateRequest request) {
        return pricingService.estimate(request);
    }

    @PostMapping("/config")
    public CostConfigurationResponse upsertCustomerConfiguration(@RequestBody CostConfigurationRequest request) {
        return pricingService.upsertCustomerConfiguration(request);
    }

    @GetMapping("/config/default")
    public CostConfigurationResponse getDefaultConfiguration() {
        return pricingService.getDefaultConfiguration();
    }

    @GetMapping("/config/{customerId}")
    public CostConfigurationResponse getCustomerConfiguration(@PathVariable String customerId) {
        return pricingService.getCustomerConfiguration(customerId);
    }
}
