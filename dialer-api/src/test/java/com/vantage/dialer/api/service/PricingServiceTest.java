package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.CostEstimateRequest;
import com.vantage.dialer.api.dto.CostEstimateResponse;
import com.vantage.dialer.api.persistence.model.CostConfigurationEntity;
import com.vantage.dialer.api.persistence.repository.CostConfigurationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PricingServiceTest {

    @Test
    void estimatesMonthlyCostAndSellPrice() {
        CostConfigurationRepository repository = mock(CostConfigurationRepository.class);
        CustomerConfigurationService customerConfigurationService = mock(CustomerConfigurationService.class);
        CostConfigurationEntity config = new CostConfigurationEntity();
        config.setConfigurationId("default");
        config.setAsteriskServerMonthlyCost(15.18);
        config.setAppServerMonthlyCost(30.37);
        config.setEbsMonthlyCost(4.00);
        config.setSnapshotMonthlyCost(1.50);
        config.setVoiceMinuteCost(0.015);
        config.setTtsUnitCost(0.00002);
        config.setSttMinuteCost(0.012);
        config.setRecordingGbCost(0.10);
        when(repository.findById("default")).thenReturn(Optional.of(config));

        PricingService service = new PricingService(repository, customerConfigurationService);
        CostEstimateRequest request = new CostEstimateRequest();
        request.setMonthlyCallMinutes(1000L);
        request.setMonthlyTtsUnits(20000L);
        request.setMonthlySttMinutes(100L);
        request.setMonthlyRecordingGb(15.0);
        request.setDesiredMarginPercent(30.0);

        CostEstimateResponse response = service.estimate(request);

        assertTrue(response.fixedInfrastructureCost() > 0);
        assertTrue(response.variableUsageCost() > 0);
        assertTrue(response.suggestedSellPrice() > response.totalEstimatedCost());
    }
}
