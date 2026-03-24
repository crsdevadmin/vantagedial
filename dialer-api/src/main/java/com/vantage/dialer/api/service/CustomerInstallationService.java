package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.agent.Agent;
import com.vantage.dialer.api.agent.AgentStore;
import com.vantage.dialer.api.dto.AsteriskClientType;
import com.vantage.dialer.api.dto.AsteriskDeploymentExecutionResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPreflightResponse;
import com.vantage.dialer.api.dto.AgentProvisionRequest;
import com.vantage.dialer.api.dto.CustomerInstallationRequest;
import com.vantage.dialer.api.dto.CustomerInstallationResponse;
import com.vantage.dialer.api.dto.CustomerConfigurationResponse;
import com.vantage.dialer.api.dto.ProposalPresetResponse;
import com.vantage.dialer.api.persistence.model.CustomerInstallationJobEntity;
import com.vantage.dialer.api.persistence.model.InstallationJobStatus;
import com.vantage.dialer.api.persistence.repository.CustomerInstallationJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerInstallationService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() { };

    private final CustomerInstallationJobRepository repository;
    private final AgentStore agentStore;
    private final AsteriskProvisioningService provisioningService;
    private final AsteriskDeploymentRunnerService deploymentRunnerService;
    private final CustomerConfigurationService customerConfigurationService;
    private final ObjectMapper objectMapper;

    public CustomerInstallationService(CustomerInstallationJobRepository repository,
                                       AgentStore agentStore,
                                       AsteriskProvisioningService provisioningService,
                                       AsteriskDeploymentRunnerService deploymentRunnerService,
                                       CustomerConfigurationService customerConfigurationService,
                                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.agentStore = agentStore;
        this.provisioningService = provisioningService;
        this.deploymentRunnerService = deploymentRunnerService;
        this.customerConfigurationService = customerConfigurationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CustomerInstallationResponse install(CustomerInstallationRequest request) {
        String customerId = normalize(request.getCustomerId(), null);
        CustomerConfigurationResponse customerConfiguration = customerId == null
                ? null
                : customerConfigurationService.find(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown customer: " + customerId));
        ProposalPresetResponse preset = customerConfiguration == null
                ? null
                : customerConfigurationService.findProposalPreset(customerConfiguration.proposalPreset()).orElse(null);

        String resolvedClientType = normalize(request.getClientType(),
                useCustomerPresetDefaults(request) && preset != null ? preset.recommendedClientType() : "SOFTPHONE");
        boolean resolvedDryRun = booleanFallback(request.getDryRun(), true);
        boolean resolvedPerformRemoteChecks = booleanFallback(
                request.getPerformRemoteChecks(),
                useCustomerPresetDefaults(request) && preset != null ? preset.recommendedPerformRemoteChecks() : true);
        boolean resolvedDeployAfterProvision = booleanFallback(
                request.getDeployAfterProvision(),
                useCustomerPresetDefaults(request) && preset != null ? preset.recommendedDeployAfterProvision() : true);

        AsteriskClientType clientType = AsteriskClientType.from(resolvedClientType);
        if (customerId != null && customerConfiguration == null) {
            throw new IllegalArgumentException("Unknown customer: " + customerId);
        }
        CustomerInstallationJobEntity job = new CustomerInstallationJobEntity();
        job.setInstallationJobId(UUID.randomUUID().toString());
        job.setCustomerId(customerId);
        job.setInstallationName(normalize(request.getInstallationName(), "installation-" + UUID.randomUUID()));
        job.setClientType(clientType.name());
        job.setStatus(InstallationJobStatus.PENDING);
        job.setDryRun(resolvedDryRun);
        job.setDeployAfterProvision(resolvedDeployAfterProvision);
        job.setPerformRemoteChecks(resolvedPerformRemoteChecks);
        job.setRequestJson(writeJson(request));
        repository.save(job);

        try {
            List<Agent> provisionedAgents = provisionAgents(request.getAgents());
            job.setAgentCount(provisionedAgents.size());
            job.setProvisionedAgentsJson(writeJson(provisionedAgents));
            job.setProvisionedAgentIdsJson(writeJson(provisionedAgents.stream().map(Agent::getAgentId).toList()));

            AsteriskDeploymentPreflightResponse preflight =
                    deploymentRunnerService.preflight(clientType, resolvedPerformRemoteChecks);
            job.setPreflightJson(writeJson(preflight));

            if (!preflight.ready()) {
                job.setStatus(resolvedDryRun ? InstallationJobStatus.DRY_RUN : InstallationJobStatus.FAILED);
                job.setCompletedAt(Instant.now());
                job.setMessage("Preflight did not pass. Review checks before continuing.");
                repository.save(job);
                return toResponse(job, provisionedAgents, preflight, null);
            }

            AsteriskDeploymentExecutionResponse deployment = null;
            if (resolvedDeployAfterProvision) {
                AsteriskDeploymentPackageResponse deploymentPackage =
                        provisioningService.generateAllAgentsPackage(provisionedAgents, clientType);
                job.setPackageId(deploymentPackage.packageId());
                deployment = deploymentRunnerService.deploy(deploymentPackage, resolvedDryRun);
                job.setDeploymentJobId(deployment.deploymentJobId());
                job.setDeploymentJson(writeJson(deployment));
            }

            job.setStatus(resolvedDryRun ? InstallationJobStatus.DRY_RUN : InstallationJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setMessage(resolvedDeployAfterProvision
                    ? "Installation workflow completed."
                    : "Agents provisioned and preflight completed. Deployment skipped by request.");
            repository.save(job);
            return toResponse(job, provisionedAgents, preflight, deployment);
        } catch (RuntimeException e) {
            job.setStatus(InstallationJobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(e.getMessage());
            job.setMessage("Installation workflow failed.");
            repository.save(job);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<CustomerInstallationResponse> list() {
        return list(null);
    }

    @Transactional(readOnly = true)
    public List<CustomerInstallationResponse> list(String customerId) {
        return (customerId == null || customerId.isBlank()
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerInstallationResponse get(String installationJobId) {
        return repository.findById(installationJobId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Unknown installation job: " + installationJobId));
    }

    private List<Agent> provisionAgents(List<AgentProvisionRequest> requests) {
        List<Agent> provisioned = new ArrayList<>();
        for (AgentProvisionRequest request : requests) {
            provisioned.add(agentStore.createOrUpdate(request));
        }
        return provisioned;
    }

    private CustomerInstallationResponse toResponse(CustomerInstallationJobEntity entity) {
        return toResponse(
                entity,
                resolveProvisionedAgents(entity.getProvisionedAgentIdsJson()),
                readJson(entity.getPreflightJson(), AsteriskDeploymentPreflightResponse.class),
                readJson(entity.getDeploymentJson(), AsteriskDeploymentExecutionResponse.class)
        );
    }

    private CustomerInstallationResponse toResponse(CustomerInstallationJobEntity entity,
                                                    List<Agent> provisionedAgents,
                                                    AsteriskDeploymentPreflightResponse preflight,
                                                    AsteriskDeploymentExecutionResponse deployment) {
        return new CustomerInstallationResponse(
                entity.getInstallationJobId(),
                entity.getCustomerId(),
                entity.getInstallationName(),
                entity.getClientType(),
                entity.getStatus().name(),
                entity.isDryRun(),
                entity.isDeployAfterProvision(),
                entity.isPerformRemoteChecks(),
                entity.getAgentCount(),
                entity.getPackageId(),
                entity.getDeploymentJobId(),
                provisionedAgents,
                preflight,
                deployment,
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getMessage(),
                entity.getErrorMessage()
        );
    }

    private String writeJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize installation payload", e);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize installation payload", e);
        }
    }

    private List<Agent> resolveProvisionedAgents(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE).stream()
                    .map(agentStore::findAgent)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize provisioned agents payload", e);
        }
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private boolean booleanFallback(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private boolean useCustomerPresetDefaults(CustomerInstallationRequest request) {
        return request.getUseCustomerPresetDefaults() == null || request.getUseCustomerPresetDefaults();
    }
}
