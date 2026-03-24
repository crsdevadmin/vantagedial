package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.AsteriskDeploymentAuditResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentExecutionResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import com.vantage.dialer.api.dto.TelephonyDeploymentAuditResponse;
import com.vantage.dialer.api.persistence.model.AsteriskDeploymentJobEntity;
import com.vantage.dialer.api.persistence.model.DeploymentExecutionStatus;
import com.vantage.dialer.api.persistence.repository.AsteriskDeploymentJobRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsteriskDeploymentAuditServiceTest {

    @Test
    void createPendingAndCompleteExposeAsteriskAndTelephonyAuditViews() throws Exception {
        AsteriskDeploymentJobRepository repository = mock(AsteriskDeploymentJobRepository.class);
        AsteriskDeploymentAuditService service = new AsteriskDeploymentAuditService(
                repository,
                CustomerServiceTestFixtures.objectMapper()
        );

        when(repository.save(any(AsteriskDeploymentJobEntity.class))).thenAnswer(invocation -> {
            AsteriskDeploymentJobEntity entity = invocation.getArgument(0);
            if (entity.getCreatedAt() == null) {
                setField(entity, "createdAt", Instant.parse("2026-03-22T12:00:00Z"));
            }
            return entity;
        });

        AsteriskDeploymentPackageResponse deploymentPackage = deploymentPackage();
        AsteriskDeploymentJobEntity entity = service.createPending(deploymentPackage, false);
        AsteriskDeploymentExecutionResponse execution = new AsteriskDeploymentExecutionResponse(
                entity.getDeploymentJobId(),
                deploymentPackage.packageId(),
                deploymentPackage.packageType(),
                deploymentPackage.clientType(),
                false,
                true,
                "pbx.acme.test",
                22,
                "/tmp/vantage-asterisk",
                "/tmp/vantage-asterisk/pkg-1",
                "/etc/asterisk/generated",
                List.of("scp pkg", "ssh apply"),
                deploymentPackage.bundledFiles(),
                deploymentPackage.generatedAt(),
                Instant.parse("2026-03-22T12:05:00Z"),
                "Deployment completed"
        );

        service.complete(entity, execution);
        when(repository.findById(entity.getDeploymentJobId())).thenReturn(Optional.of(entity));
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(entity));

        AsteriskDeploymentAuditResponse audit = service.get(entity.getDeploymentJobId());
        TelephonyDeploymentAuditResponse telephonyAudit = service.getDeploymentAudit(entity.getDeploymentJobId());
        List<TelephonyDeploymentAuditResponse> listed = service.listDeploymentAudits(null);

        assertEquals("DEPLOYED", audit.status());
        assertTrue(audit.deployed());
        assertEquals("pbx.acme.test", audit.host());
        assertEquals(List.of("scp pkg", "ssh apply"), audit.commands());
        assertEquals(List.of("agent-1", "agent-2"), audit.agentIds());
        assertEquals("ASTERISK", telephonyAudit.provider());
        assertEquals(audit.deploymentJobId(), telephonyAudit.deploymentJobId());
        assertEquals(audit.packageId(), telephonyAudit.packageId());
        assertEquals(1, listed.size());
        assertEquals("ASTERISK", listed.get(0).provider());
    }

    @Test
    void completeMarksDryRunExecutionsWithDryRunStatus() throws Exception {
        AsteriskDeploymentJobRepository repository = mock(AsteriskDeploymentJobRepository.class);
        AsteriskDeploymentAuditService service = new AsteriskDeploymentAuditService(
                repository,
                CustomerServiceTestFixtures.objectMapper()
        );

        when(repository.save(any(AsteriskDeploymentJobEntity.class))).thenAnswer(invocation -> {
            AsteriskDeploymentJobEntity entity = invocation.getArgument(0);
            if (entity.getCreatedAt() == null) {
                setField(entity, "createdAt", Instant.parse("2026-03-22T12:00:00Z"));
            }
            return entity;
        });

        AsteriskDeploymentPackageResponse deploymentPackage = deploymentPackage();
        AsteriskDeploymentJobEntity entity = service.createPending(deploymentPackage, true);
        AsteriskDeploymentExecutionResponse execution = new AsteriskDeploymentExecutionResponse(
                entity.getDeploymentJobId(),
                deploymentPackage.packageId(),
                deploymentPackage.packageType(),
                deploymentPackage.clientType(),
                true,
                false,
                "pbx.acme.test",
                22,
                "/tmp/vantage-asterisk",
                "/tmp/vantage-asterisk/pkg-1",
                "/etc/asterisk/generated",
                List.of("ssh review"),
                deploymentPackage.bundledFiles(),
                deploymentPackage.generatedAt(),
                Instant.parse("2026-03-22T12:05:00Z"),
                "Dry run only"
        );

        service.complete(entity, execution);
        when(repository.findById(entity.getDeploymentJobId())).thenReturn(Optional.of(entity));

        AsteriskDeploymentAuditResponse audit = service.get(entity.getDeploymentJobId());
        TelephonyDeploymentAuditResponse telephonyAudit = service.getDeploymentAudit(entity.getDeploymentJobId());

        assertEquals("DRY_RUN", audit.status());
        assertTrue(audit.dryRun());
        assertFalse(audit.deployed());
        assertEquals("DRY_RUN", telephonyAudit.status());
    }

    @Test
    void failAndListByPackageIdReturnFailedAudit() throws Exception {
        AsteriskDeploymentJobRepository repository = mock(AsteriskDeploymentJobRepository.class);
        AsteriskDeploymentAuditService service = new AsteriskDeploymentAuditService(
                repository,
                CustomerServiceTestFixtures.objectMapper()
        );

        when(repository.save(any(AsteriskDeploymentJobEntity.class))).thenAnswer(invocation -> {
            AsteriskDeploymentJobEntity entity = invocation.getArgument(0);
            if (entity.getCreatedAt() == null) {
                setField(entity, "createdAt", Instant.parse("2026-03-22T12:00:00Z"));
            }
            return entity;
        });

        AsteriskDeploymentPackageResponse deploymentPackage = deploymentPackage();
        AsteriskDeploymentJobEntity entity = service.createPending(deploymentPackage, false);
        AsteriskDeploymentExecutionResponse baseResponse = new AsteriskDeploymentExecutionResponse(
                entity.getDeploymentJobId(),
                deploymentPackage.packageId(),
                deploymentPackage.packageType(),
                deploymentPackage.clientType(),
                false,
                false,
                "pbx.acme.test",
                22,
                "/tmp/vantage-asterisk",
                "/tmp/vantage-asterisk/pkg-1",
                "/etc/asterisk/generated",
                List.of("scp pkg"),
                deploymentPackage.bundledFiles(),
                deploymentPackage.generatedAt(),
                null,
                "Failed while copying package"
        );

        service.fail(entity, baseResponse, "Permission denied");
        when(repository.findByPackageIdOrderByCreatedAtDesc("pkg-1")).thenReturn(List.of(entity));

        List<AsteriskDeploymentAuditResponse> audits = service.list("pkg-1");

        assertEquals(1, audits.size());
        assertEquals("FAILED", audits.get(0).status());
        assertFalse(audits.get(0).deployed());
        assertEquals("Permission denied", audits.get(0).errorMessage());
        assertEquals(List.of("scp pkg"), audits.get(0).commands());
        assertNotNull(audits.get(0).executedAt());
    }

    @Test
    void getUnknownDeploymentJobThrowsHelpfulError() {
        AsteriskDeploymentJobRepository repository = mock(AsteriskDeploymentJobRepository.class);
        AsteriskDeploymentAuditService service = new AsteriskDeploymentAuditService(
                repository,
                CustomerServiceTestFixtures.objectMapper()
        );

        when(repository.findById("missing-job")).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.get("missing-job"));

        assertEquals("Unknown deployment job: missing-job", error.getMessage());
    }

    private AsteriskDeploymentPackageResponse deploymentPackage() {
        return new AsteriskDeploymentPackageResponse(
                "pkg-1",
                "FULL",
                "WEBRTC",
                "build/asterisk/pkg-1",
                "build/asterisk/pkg-1/agents.generated.conf",
                "build/asterisk/pkg-1/apply-and-reload.sh",
                "build/asterisk/pkg-1/manifest.json",
                Instant.parse("2026-03-22T12:01:00Z"),
                List.of("agent-1", "agent-2"),
                List.of("agents.generated.conf", "apply-and-reload.sh", "manifest.json")
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
