package com.vantage.dialer.api.service;

import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStatus;
import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.dto.AgentProvisionRequest;
import com.vantage.dialer.api.dto.AsteriskDeploymentExecutionResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPreflightResponse;
import com.vantage.dialer.api.dto.AsteriskPreflightCheckResponse;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.CustomerInstallationRequest;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import com.vantage.dialer.api.persistence.model.CustomerInstallationJobEntity;
import com.vantage.dialer.api.persistence.model.InstallationJobStatus;
import com.vantage.dialer.api.persistence.repository.CustomerInstallationJobRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerInstallationServiceTest {

    @Test
    void installUsesPresetDefaultsAndCompletesDeployment() {
        CustomerInstallationJobRepository repository = mock(CustomerInstallationJobRepository.class);
        AgentStore agentStore = mock(AgentStore.class);
        AsteriskProvisioningService provisioningService = mock(AsteriskProvisioningService.class);
        AsteriskDeploymentRunnerService deploymentRunnerService = mock(AsteriskDeploymentRunnerService.class);
        CustomerConfigurationService customerConfigurationService = mock(CustomerConfigurationService.class);
        CustomerInstallationService service = service(
                repository,
                agentStore,
                provisioningService,
                deploymentRunnerService,
                customerConfigurationService
        );

        CustomerInstallationRequest request = installRequest("customer-1", "Install Alpha", false);
        Agent agent = new Agent("agent-1", "Agent One", "PJSIP/1001", "1001", "user1", "pass1", AgentStatus.AVAILABLE);
        CustomerConfigurationResponse configuration = new CustomerConfigurationResponse(
                "customer-1",
                "Acme",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "preset-1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1000L,
                2000L,
                100L,
                5.0,
                2,
                10,
                30.0,
                null
        );
        ProposalPresetResponse preset = new ProposalPresetResponse(
                "preset-1", "default", true, true, true, true, true, true,
                "WEBRTC", "agent", "supervisor", true, false,
                1000L, 2000L, 100L, 5.0, 2, 10, 30.0
        );
        AsteriskDeploymentPreflightResponse preflight = new AsteriskDeploymentPreflightResponse(
                true, false, true, "pbx-1", 22, "deploy", "/opt/vantage", "/srv/vantage",
                List.of(new AsteriskPreflightCheckResponse("ssh", true, "ok")),
                List.of("check ssh"),
                Instant.parse("2026-03-22T12:00:00Z")
        );
        AsteriskDeploymentPackageResponse deploymentPackage = new AsteriskDeploymentPackageResponse(
                "pkg-1", "FULL", "WEBRTC", "/tmp/pkg", "pjsip.conf", "reload.sh", "manifest.json",
                Instant.parse("2026-03-22T12:01:00Z"), List.of("agent-1"), List.of("pjsip.conf")
        );
        AsteriskDeploymentExecutionResponse deployment = new AsteriskDeploymentExecutionResponse(
                "deploy-1", "pkg-1", "FULL", "WEBRTC", false, true, "pbx-1", 22,
                "/opt/vantage", "/opt/vantage/pkg", "/srv/vantage", List.of("scp pkg"),
                List.of("pjsip.conf"), Instant.parse("2026-03-22T12:01:00Z"),
                Instant.parse("2026-03-22T12:02:00Z"), "Deployment completed"
        );

        when(customerConfigurationService.find("customer-1")).thenReturn(Optional.of(configuration));
        when(customerConfigurationService.findProposalPreset("preset-1")).thenReturn(Optional.of(preset));
        when(repository.save(any(CustomerInstallationJobEntity.class))).thenAnswer(invocation -> {
            CustomerInstallationJobEntity entity = invocation.getArgument(0);
            if (entity.getInstallationJobId() != null) {
                setField(entity, "createdAt", Instant.parse("2026-03-22T12:00:00Z"));
            }
            return entity;
        });
        when(agentStore.createOrUpdate(any(AgentProvisionRequest.class))).thenReturn(agent);
        when(deploymentRunnerService.preflight(eq(com.vantage.dialer.api.dto.AsteriskClientType.WEBRTC), eq(false))).thenReturn(preflight);
        when(provisioningService.generateAllAgentsPackage(List.of(agent), com.vantage.dialer.api.dto.AsteriskClientType.WEBRTC))
                .thenReturn(deploymentPackage);
        when(deploymentRunnerService.deploy(deploymentPackage, false)).thenReturn(deployment);

        CustomerInstallationResponse response = service.install(request);

        assertEquals("WEBRTC", response.clientType());
        assertEquals("COMPLETED", response.status());
        assertEquals("pkg-1", response.packageId());
        assertEquals("deploy-1", response.deploymentJobId());
        assertEquals(1, response.agentCount());
        assertNotNull(response.preflight());
        assertNotNull(response.deployment());
        assertEquals("Installation workflow completed.", response.message());
    }

    @Test
    void installStopsWhenPreflightFails() {
        CustomerInstallationJobRepository repository = mock(CustomerInstallationJobRepository.class);
        AgentStore agentStore = mock(AgentStore.class);
        AsteriskProvisioningService provisioningService = mock(AsteriskProvisioningService.class);
        AsteriskDeploymentRunnerService deploymentRunnerService = mock(AsteriskDeploymentRunnerService.class);
        CustomerConfigurationService customerConfigurationService = mock(CustomerConfigurationService.class);
        CustomerInstallationService service = service(
                repository,
                agentStore,
                provisioningService,
                deploymentRunnerService,
                customerConfigurationService
        );

        CustomerInstallationRequest request = installRequest(null, "Install Beta", false);
        Agent agent = new Agent("agent-1", "Agent One", "PJSIP/1001", "1001", "user1", "pass1", AgentStatus.AVAILABLE);
        AsteriskDeploymentPreflightResponse preflight = new AsteriskDeploymentPreflightResponse(
                false, true, true, "pbx-1", 22, "deploy", "/opt/vantage", "/srv/vantage",
                List.of(new AsteriskPreflightCheckResponse("ssh", false, "unreachable")),
                List.of("check ssh"),
                Instant.parse("2026-03-22T12:00:00Z")
        );

        when(repository.save(any(CustomerInstallationJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentStore.createOrUpdate(any(AgentProvisionRequest.class))).thenReturn(agent);
        when(deploymentRunnerService.preflight(eq(com.vantage.dialer.api.dto.AsteriskClientType.SOFTPHONE), eq(true))).thenReturn(preflight);

        CustomerInstallationResponse response = service.install(request);

        assertEquals("FAILED", response.status());
        assertNull(response.deployment());
        assertEquals("Preflight did not pass. Review checks before continuing.", response.message());
        verify(provisioningService, never()).generateAllAgentsPackage(any(), any());
    }

    @Test
    void listAndGetRehydrateStoredAgentsAndDeploymentPayloads() throws Exception {
        CustomerInstallationJobRepository repository = mock(CustomerInstallationJobRepository.class);
        AgentStore agentStore = mock(AgentStore.class);
        AsteriskProvisioningService provisioningService = mock(AsteriskProvisioningService.class);
        AsteriskDeploymentRunnerService deploymentRunnerService = mock(AsteriskDeploymentRunnerService.class);
        CustomerConfigurationService customerConfigurationService = mock(CustomerConfigurationService.class);
        CustomerInstallationService service = service(
                repository,
                agentStore,
                provisioningService,
                deploymentRunnerService,
                customerConfigurationService
        );

        Agent agent = new Agent("agent-1", "Agent One", "PJSIP/1001", "1001", "user1", "pass1", AgentStatus.AVAILABLE);
        CustomerInstallationJobEntity entity = new CustomerInstallationJobEntity();
        entity.setInstallationJobId("install-1");
        entity.setCustomerId("customer-1");
        entity.setInstallationName("Install Gamma");
        entity.setClientType("SOFTPHONE");
        entity.setStatus(InstallationJobStatus.COMPLETED);
        entity.setDryRun(false);
        entity.setDeployAfterProvision(true);
        entity.setPerformRemoteChecks(true);
        entity.setAgentCount(1);
        entity.setPackageId("pkg-1");
        entity.setDeploymentJobId("deploy-1");
        entity.setProvisionedAgentIdsJson(CustomerServiceTestFixtures.objectMapper().writeValueAsString(List.of("agent-1")));
        entity.setPreflightJson(CustomerServiceTestFixtures.objectMapper().writeValueAsString(
                new AsteriskDeploymentPreflightResponse(true, true, true, "pbx", 22, "deploy", "/opt", "/srv",
                        List.of(), List.of(), Instant.parse("2026-03-22T12:00:00Z"))));
        entity.setDeploymentJson(CustomerServiceTestFixtures.objectMapper().writeValueAsString(
                new AsteriskDeploymentExecutionResponse("deploy-1", "pkg-1", "FULL", "SOFTPHONE", false, true,
                        "pbx", 22, "/opt", "/opt/pkg", "/srv", List.of("scp"), List.of("pjsip.conf"),
                        Instant.parse("2026-03-22T12:01:00Z"), Instant.parse("2026-03-22T12:02:00Z"), "done")));
        setField(entity, "createdAt", Instant.parse("2026-03-22T12:00:00Z"));

        when(repository.findAllByCustomerIdOrderByCreatedAtDesc("customer-1")).thenReturn(List.of(entity));
        when(repository.findById("install-1")).thenReturn(Optional.of(entity));
        when(agentStore.findAgent("agent-1")).thenReturn(Optional.of(agent));

        CustomerInstallationResponse listed = service.list("customer-1").get(0);
        CustomerInstallationResponse fetched = service.get("install-1");

        assertEquals("Install Gamma", listed.installationName());
        assertEquals(1, listed.provisionedAgents().size());
        assertEquals("agent-1", listed.provisionedAgents().get(0).getAgentId());
        assertEquals("deploy-1", listed.deployment().deploymentJobId());
        assertEquals("pkg-1", fetched.packageId());
        assertTrue(fetched.preflight().ready());
    }

    private CustomerInstallationService service(CustomerInstallationJobRepository repository,
                                                AgentStore agentStore,
                                                AsteriskProvisioningService provisioningService,
                                                AsteriskDeploymentRunnerService deploymentRunnerService,
                                                CustomerConfigurationService customerConfigurationService) {
        return new CustomerInstallationService(
                repository,
                agentStore,
                provisioningService,
                deploymentRunnerService,
                customerConfigurationService,
                CustomerServiceTestFixtures.objectMapper()
        );
    }

    private CustomerInstallationRequest installRequest(String customerId, String installationName, boolean dryRun) {
        CustomerInstallationRequest request = new CustomerInstallationRequest();
        request.setCustomerId(customerId);
        request.setInstallationName(installationName);
        request.setDryRun(dryRun);
        AgentProvisionRequest agent = new AgentProvisionRequest();
        agent.setAgentId("agent-1");
        agent.setAgentName("Agent One");
        agent.setExtensionNumber("1001");
        agent.setSipUsername("user1");
        agent.setSipPassword("pass1");
        agent.setChannel("PJSIP/1001");
        request.setAgents(List.of(agent));
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
