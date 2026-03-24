package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.CustomerConfigurationRequest;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import com.vantage.dialer.api.persistence.model.CustomerConfigurationEntity;
import com.vantage.dialer.api.persistence.repository.CustomerConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerConfigurationServiceTest {

    @Test
    void createOrUpdateAppliesDefaultsAndPresetDerivedFlags() {
        CustomerConfigurationRepository repository = mock(CustomerConfigurationRepository.class);
        CustomerConfigurationService service = new CustomerConfigurationService(repository);

        CustomerConfigurationRequest request = new CustomerConfigurationRequest();
        request.setCustomerId(" customer-1 ");
        request.setCustomerName(" Acme Corp ");
        request.setServerAHost(" pbx.acme.test ");
        request.setProposalPreset("COMMERCIAL_MINIMAL");

        when(repository.findById("customer-1")).thenReturn(Optional.empty());
        when(repository.save(any(CustomerConfigurationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerConfigurationResponse response = service.createOrUpdate(request);

        assertEquals("customer-1", response.customerId());
        assertEquals("Acme Corp", response.customerName());
        assertEquals("pbx.acme.test", response.serverAHost());
        assertEquals("ubuntu", response.asteriskDeployUser());
        assertEquals("/etc/asterisk/generated", response.asteriskDeployTargetDirectory());
        assertEquals("admin", response.amiUsername());
        assertEquals("vivphone-endpoint", response.amiEndpoint());
        assertEquals("91", response.dialPrefix());
        assertEquals("jssip", response.defaultAgentUiMode());
        assertEquals("MONITOR_ONLY", response.defaultSupervisorUiMode());
        assertEquals("Acme Corp", response.brandDisplayName());
        assertEquals("#1f2a2a", response.brandPrimaryColor());
        assertEquals("#1c7c54", response.brandAccentColor());
        assertEquals("COMMERCIAL_MINIMAL", response.proposalPreset());
        assertTrue(response.proposalIncludeAgentOutbound());
        assertFalse(response.proposalIncludeIvr());
        assertFalse(response.proposalIncludeReporting());
        assertFalse(response.proposalIncludeWebRtc());
        assertTrue(response.proposalIncludeProvisioning());
        assertFalse(response.proposalIncludePricingBreakdown());

        ArgumentCaptor<CustomerConfigurationEntity> captor = ArgumentCaptor.forClass(CustomerConfigurationEntity.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        service.createOrUpdate(request);
        assertEquals("customer-1", captor.getValue().getCustomerId());
        assertEquals("Acme Corp", captor.getValue().getBrandDisplayName());
    }

    @Test
    void findListAndPresetLookupReturnMappedValues() {
        CustomerConfigurationRepository repository = mock(CustomerConfigurationRepository.class);
        CustomerConfigurationService service = new CustomerConfigurationService(repository);
        CustomerConfigurationEntity entity = configurationEntity("customer-2", "Bravo Corp");

        when(repository.findById("customer-2")).thenReturn(Optional.of(entity));
        when(repository.findAll()).thenReturn(List.of(entity));

        CustomerConfigurationResponse found = service.find("customer-2").orElseThrow();
        List<CustomerConfigurationResponse> listed = service.list();
        ProposalPresetResponse preset = service.findProposalPreset("webrtc_contact_center").orElseThrow();
        ProposalPresetResponse defaultPreset = service.findProposalPreset(null).orElseThrow();

        assertEquals("Bravo Corp", found.customerName());
        assertEquals("FULL_SUITE", found.proposalPreset());
        assertEquals(1, listed.size());
        assertEquals("WEBRTC_CONTACT_CENTER", preset.presetId());
        assertEquals("WEBRTC", preset.recommendedClientType());
        assertEquals("FULL_SUITE", defaultPreset.presetId());
    }

    private CustomerConfigurationEntity configurationEntity(String customerId, String customerName) {
        CustomerConfigurationEntity entity = new CustomerConfigurationEntity();
        entity.setCustomerId(customerId);
        entity.setCustomerName(customerName);
        entity.setServerAHost("pbx.example.test");
        entity.setServerAPrivateIp("10.0.0.10");
        entity.setServerBHost("api.example.test");
        entity.setAsteriskDeployUser("ubuntu");
        entity.setAsteriskDeployPrivateKeyPath("/keys/pbx.pem");
        entity.setAsteriskDeployTargetDirectory("/etc/asterisk/generated");
        entity.setAmiUsername("admin");
        entity.setAmiEndpoint("vivphone-endpoint");
        entity.setDialPrefix("91");
        entity.setSipDomain("pbx.example.test");
        entity.setWebSocketUrl("wss://pbx.example.test/ws");
        entity.setApiBaseUrl("http://api.example.test:8081");
        entity.setDefaultAgentUiMode("jssip");
        entity.setDefaultSupervisorUiMode("MONITOR_ONLY");
        entity.setBrandDisplayName(customerName);
        entity.setBrandPrimaryColor("#1f2a2a");
        entity.setBrandAccentColor("#1c7c54");
        entity.setProposalPreset("FULL_SUITE");
        entity.setProposalTemplate("STANDARD");
        entity.setProposalTitle("Vantage Dialer Proposal");
        entity.setProposalSubtitle("Outbound rollout");
        entity.setProposalIncludeAgentOutbound(true);
        entity.setProposalIncludeIvr(true);
        entity.setProposalIncludeReporting(true);
        entity.setProposalIncludeWebRtc(true);
        entity.setProposalIncludeProvisioning(true);
        entity.setProposalIncludePricingBreakdown(true);
        entity.setDefaultMonthlyCallMinutes(25000L);
        entity.setDefaultMonthlyTtsUnits(120000L);
        entity.setDefaultMonthlySttMinutes(5000L);
        entity.setDefaultMonthlyRecordingGb(35.0);
        entity.setDefaultAgentCount(2);
        entity.setDefaultConcurrentChannels(10);
        entity.setDefaultMarginPercent(35.0);
        entity.setProposalTerms("Net 30");
        return entity;
    }
}
