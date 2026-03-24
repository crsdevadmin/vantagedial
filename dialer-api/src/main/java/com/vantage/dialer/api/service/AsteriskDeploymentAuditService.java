package com.vantage.dialer.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.dialer.api.dto.AsteriskDeploymentAuditResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentExecutionResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import com.vantage.dialer.api.dto.TelephonyDeploymentAuditResponse;
import com.vantage.dialer.api.persistence.model.AsteriskDeploymentJobEntity;
import com.vantage.dialer.api.persistence.model.DeploymentExecutionStatus;
import com.vantage.dialer.api.persistence.repository.AsteriskDeploymentJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AsteriskDeploymentAuditService implements TelephonyDeploymentAuditService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final AsteriskDeploymentJobRepository repository;
    private final ObjectMapper objectMapper;

    public AsteriskDeploymentAuditService(AsteriskDeploymentJobRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AsteriskDeploymentJobEntity createPending(AsteriskDeploymentPackageResponse deploymentPackage, boolean dryRun) {
        AsteriskDeploymentJobEntity entity = new AsteriskDeploymentJobEntity();
        entity.setDeploymentJobId(UUID.randomUUID().toString());
        entity.setPackageId(deploymentPackage.packageId());
        entity.setPackageType(deploymentPackage.packageType());
        entity.setClientType(deploymentPackage.clientType());
        entity.setStatus(DeploymentExecutionStatus.PENDING);
        entity.setDryRun(dryRun);
        entity.setDeployed(false);
        entity.setGeneratedAt(deploymentPackage.generatedAt());
        entity.setBundledFilesJson(writeJson(deploymentPackage.bundledFiles()));
        entity.setAgentIdsJson(writeJson(deploymentPackage.agentIds()));
        return repository.save(entity);
    }

    @Transactional
    public AsteriskDeploymentJobEntity complete(AsteriskDeploymentJobEntity entity, AsteriskDeploymentExecutionResponse response) {
        entity.setStatus(resolveStatus(response));
        entity.setDeployed(response.deployed());
        entity.setHost(response.host());
        entity.setPort(response.port());
        entity.setRemoteBaseDirectory(response.remoteBaseDirectory());
        entity.setRemotePackageDirectory(response.remotePackageDirectory());
        entity.setTargetDirectory(response.targetDirectory());
        entity.setCommandsJson(writeJson(response.commands()));
        entity.setBundledFilesJson(writeJson(response.bundledFiles()));
        entity.setExecutedAt(response.executedAt());
        entity.setMessage(response.message());
        entity.setErrorMessage(null);
        return repository.save(entity);
    }

    @Transactional
    public AsteriskDeploymentJobEntity fail(AsteriskDeploymentJobEntity entity,
                                            AsteriskDeploymentExecutionResponse baseResponse,
                                            String errorMessage) {
        entity.setStatus(DeploymentExecutionStatus.FAILED);
        entity.setDeployed(false);
        entity.setHost(baseResponse.host());
        entity.setPort(baseResponse.port());
        entity.setRemoteBaseDirectory(baseResponse.remoteBaseDirectory());
        entity.setRemotePackageDirectory(baseResponse.remotePackageDirectory());
        entity.setTargetDirectory(baseResponse.targetDirectory());
        entity.setCommandsJson(writeJson(baseResponse.commands()));
        entity.setBundledFilesJson(writeJson(baseResponse.bundledFiles()));
        entity.setExecutedAt(baseResponse.executedAt() == null ? Instant.now() : baseResponse.executedAt());
        entity.setMessage(baseResponse.message());
        entity.setErrorMessage(errorMessage);
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<AsteriskDeploymentAuditResponse> list(String packageId) {
        List<AsteriskDeploymentJobEntity> entities = (packageId == null || packageId.isBlank())
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByPackageIdOrderByCreatedAtDesc(packageId);
        return entities.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AsteriskDeploymentAuditResponse get(String deploymentJobId) {
        return repository.findById(deploymentJobId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Unknown deployment job: " + deploymentJobId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TelephonyDeploymentAuditResponse> listDeploymentAudits(String packageId) {
        return list(packageId).stream().map(this::toTelephonyResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TelephonyDeploymentAuditResponse getDeploymentAudit(String deploymentJobId) {
        return toTelephonyResponse(get(deploymentJobId));
    }

    private AsteriskDeploymentAuditResponse toResponse(AsteriskDeploymentJobEntity entity) {
        return new AsteriskDeploymentAuditResponse(
                entity.getDeploymentJobId(),
                entity.getPackageId(),
                entity.getPackageType(),
                entity.getClientType(),
                entity.getStatus().name(),
                entity.isDryRun(),
                entity.isDeployed(),
                entity.getHost(),
                entity.getPort(),
                entity.getRemoteBaseDirectory(),
                entity.getRemotePackageDirectory(),
                entity.getTargetDirectory(),
                readList(entity.getCommandsJson()),
                readList(entity.getBundledFilesJson()),
                readList(entity.getAgentIdsJson()),
                entity.getGeneratedAt(),
                entity.getExecutedAt(),
                entity.getCreatedAt(),
                entity.getMessage(),
                entity.getErrorMessage()
        );
    }

    private TelephonyDeploymentAuditResponse toTelephonyResponse(AsteriskDeploymentAuditResponse response) {
        return new TelephonyDeploymentAuditResponse(
                "ASTERISK",
                response.deploymentJobId(),
                response.packageId(),
                response.packageType(),
                response.clientType(),
                response.status(),
                response.dryRun(),
                response.deployed(),
                response.host(),
                response.port(),
                response.remoteBaseDirectory(),
                response.remotePackageDirectory(),
                response.targetDirectory(),
                response.commands(),
                response.bundledFiles(),
                response.agentIds(),
                response.generatedAt(),
                response.executedAt(),
                response.createdAt(),
                response.message(),
                response.errorMessage()
        );
    }

    private DeploymentExecutionStatus resolveStatus(AsteriskDeploymentExecutionResponse response) {
        if (response.dryRun()) {
            return DeploymentExecutionStatus.DRY_RUN;
        }
        return response.deployed() ? DeploymentExecutionStatus.DEPLOYED : DeploymentExecutionStatus.FAILED;
    }

    private String writeJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize deployment audit payload", e);
        }
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize deployment audit payload", e);
        }
    }
}
