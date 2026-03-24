package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.AsteriskDeploymentExecutionResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPreflightResponse;
import com.vantage.dialer.api.dto.AsteriskClientType;
import com.vantage.dialer.api.persistence.model.AsteriskDeploymentJobEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AsteriskDeploymentRunnerServiceTest {

    @Test
    void deployDryRunBuildsSanitizedCommandsAndCompletesAudit(@TempDir Path tempDir) throws Exception {
        AsteriskDeploymentAuditService auditService = mock(AsteriskDeploymentAuditService.class);
        AsteriskDeploymentRunnerService service = new AsteriskDeploymentRunnerService(
                auditService,
                true,
                "pbx.acme.test",
                22,
                "ubuntu",
                "C:/keys/pbx.pem",
                "/tmp/vantage-asterisk",
                "/etc/asterisk/generated",
                "no"
        );

        AsteriskDeploymentPackageResponse deploymentPackage = deploymentPackage(tempDir);
        AsteriskDeploymentJobEntity entity = auditEntity("job-1");
        when(auditService.createPending(deploymentPackage, true)).thenReturn(entity);

        AsteriskDeploymentExecutionResponse response = service.deploy(deploymentPackage, true);

        assertTrue(response.dryRun());
        assertFalse(response.deployed());
        assertEquals("job-1", response.deploymentJobId());
        assertEquals("/tmp/vantage-asterisk/pkg-1", response.remotePackageDirectory());
        assertEquals(3, response.commands().size());
        assertTrue(response.commands().stream().allMatch(command -> command.contains("<private-key>")));
        assertTrue(response.message().contains("Dry run only"));
        verify(auditService).complete(eq(entity), eq(response));
    }

    @Test
    void deployWhenDisabledFailsAuditWithoutRemoteExecution(@TempDir Path tempDir) throws Exception {
        AsteriskDeploymentAuditService auditService = mock(AsteriskDeploymentAuditService.class);
        AsteriskDeploymentRunnerService service = new AsteriskDeploymentRunnerService(
                auditService,
                false,
                "pbx.acme.test",
                22,
                "ubuntu",
                "",
                "/tmp/vantage-asterisk",
                "/etc/asterisk/generated",
                "no"
        );

        AsteriskDeploymentPackageResponse deploymentPackage = deploymentPackage(tempDir);
        AsteriskDeploymentJobEntity entity = auditEntity("job-2");
        when(auditService.createPending(deploymentPackage, false)).thenReturn(entity);

        AsteriskDeploymentExecutionResponse response = service.deploy(deploymentPackage, false);

        assertFalse(response.dryRun());
        assertFalse(response.deployed());
        assertTrue(response.message().contains("Remote deployment is disabled"));
        verify(auditService).fail(eq(entity), eq(response), eq(response.message()));
    }

    @Test
    void deployWithMissingHostFailsAuditDuringConfigurationValidation(@TempDir Path tempDir) throws Exception {
        AsteriskDeploymentAuditService auditService = mock(AsteriskDeploymentAuditService.class);
        AsteriskDeploymentRunnerService service = new AsteriskDeploymentRunnerService(
                auditService,
                true,
                "",
                22,
                "ubuntu",
                "C:/keys/pbx.pem",
                "/tmp/vantage-asterisk",
                "/etc/asterisk/generated",
                "no"
        );

        AsteriskDeploymentPackageResponse deploymentPackage = deploymentPackage(tempDir);
        AsteriskDeploymentJobEntity entity = auditEntity("job-3");
        when(auditService.createPending(deploymentPackage, false)).thenReturn(entity);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.deploy(deploymentPackage, false));

        assertEquals("Asterisk deploy host is not configured", error.getMessage());
        ArgumentCaptor<AsteriskDeploymentExecutionResponse> responseCaptor = ArgumentCaptor.forClass(AsteriskDeploymentExecutionResponse.class);
        verify(auditService).fail(
                eq(entity),
                responseCaptor.capture(),
                eq("Asterisk deploy host is not configured"));

        AsteriskDeploymentExecutionResponse response = responseCaptor.getValue();
        assertEquals(deploymentPackage.packageId(), response.packageId());
        assertEquals(deploymentPackage.packageType(), response.packageType());
        assertEquals(deploymentPackage.clientType(), response.clientType());
        assertFalse(response.deployed());
        assertFalse(response.dryRun());
        assertTrue(response.executedAt() != null);
        assertEquals("Deployment configuration validation failed.", response.message());
        assertEquals(List.of(
                "ssh -i <private-key> -p 22 -o StrictHostKeyChecking=no ubuntu@ mkdir -p '/tmp/vantage-asterisk'",
                "scp -r -i <private-key> -P 22 -o StrictHostKeyChecking=no " + tempDir.resolve("pkg-1") + " ubuntu@:/tmp/vantage-asterisk",
                "ssh -i <private-key> -p 22 -o StrictHostKeyChecking=no ubuntu@ chmod +x '/tmp/vantage-asterisk/pkg-1/apply-and-reload.sh' && '/tmp/vantage-asterisk/pkg-1/apply-and-reload.sh' '/etc/asterisk/generated'"
        ), response.commands());
    }

    @Test
    void preflightWithoutRemoteChecksReportsLocalConfigurationGaps() {
        AsteriskDeploymentAuditService auditService = mock(AsteriskDeploymentAuditService.class);
        AsteriskDeploymentRunnerService service = new AsteriskDeploymentRunnerService(
                auditService,
                false,
                "",
                22,
                "",
                "",
                "/tmp/vantage-asterisk",
                "/etc/asterisk/generated",
                "no"
        );

        AsteriskDeploymentPreflightResponse response = service.preflight(AsteriskClientType.WEBRTC, false);

        assertFalse(response.ready());
        assertFalse(response.remoteChecksExecuted());
        assertFalse(response.deploymentEnabled());
        assertTrue(response.commands().isEmpty());
        assertEquals("", response.host());
        assertEquals("", response.user());
        assertEquals("deployment-enabled", response.checks().get(0).checkName());
        assertFalse(response.checks().get(0).passed());
        assertTrue(response.checks().stream().anyMatch(check ->
                check.checkName().equals("deploy-host-configured")
                        && !check.passed()
                        && check.detail().contains("APP_ASTERISK_DEPLOY_HOST is not configured")));
        assertTrue(response.checks().stream().anyMatch(check ->
                check.checkName().equals("deploy-user-configured")
                        && !check.passed()
                        && check.detail().contains("APP_ASTERISK_DEPLOY_USER is not configured")));
        assertTrue(response.checks().stream().anyMatch(check ->
                check.checkName().equals("private-key-configured")
                        && !check.passed()
                        && check.detail().contains("APP_ASTERISK_DEPLOY_PRIVATE_KEY is not configured")));
        verifyNoInteractions(auditService);
    }

    @Test
    void preflightSkipsRemoteChecksWhenCoreConfigurationIsIncomplete() {
        AsteriskDeploymentAuditService auditService = mock(AsteriskDeploymentAuditService.class);
        AsteriskDeploymentRunnerService service = new AsteriskDeploymentRunnerService(
                auditService,
                true,
                "pbx.acme.test",
                22,
                "ubuntu",
                "",
                "/tmp/vantage-asterisk",
                "/etc/asterisk/generated",
                "no"
        );

        AsteriskDeploymentPreflightResponse response = service.preflight(AsteriskClientType.SOFTPHONE, true);

        assertFalse(response.ready());
        assertFalse(response.remoteChecksExecuted());
        assertTrue(response.commands().isEmpty());
        assertTrue(response.checks().stream().anyMatch(check ->
                check.checkName().equals("private-key-configured")
                        && !check.passed()));
    }

    @Test
    void preflightWithRemoteChecksForWebRtcBuildsSanitizedCommandPlan(@TempDir Path tempDir) throws Exception {
        AsteriskDeploymentAuditService auditService = mock(AsteriskDeploymentAuditService.class);
        Path privateKey = Files.writeString(tempDir.resolve("pbx.pem"), "key");
        TestableAsteriskDeploymentRunnerService service = testableRunner(
                auditService,
                true,
                "pbx.acme.test",
                "ubuntu",
                privateKey.toString(),
                command -> true,
                command -> {
                }
        );

        AsteriskDeploymentPreflightResponse response = service.preflight(AsteriskClientType.WEBRTC, true);

        assertTrue(response.ready());
        assertTrue(response.remoteChecksExecuted());
        assertEquals(13, response.commands().size());
        assertEquals(13, service.executedCommands().size());
        assertTrue(response.commands().stream().allMatch(command -> command.contains("<private-key>")));
        assertTrue(response.commands().stream().anyMatch(command -> command.contains("ubuntu@pbx.acme.test")));
        assertTrue(response.checks().stream().anyMatch(check ->
                check.checkName().equals("remote-pjsip-generated-include") && check.passed()));
        assertTrue(response.checks().stream().anyMatch(check ->
                check.checkName().equals("remote-webrtc-modules") && check.passed()));
    }

    @Test
    void preflightWithRemoteCheckFailureKeepsPlanAndMarksNotReady(@TempDir Path tempDir) throws Exception {
        AsteriskDeploymentAuditService auditService = mock(AsteriskDeploymentAuditService.class);
        Path privateKey = Files.writeString(tempDir.resolve("pbx.pem"), "key");
        TestableAsteriskDeploymentRunnerService service = testableRunner(
                auditService,
                true,
                "pbx.acme.test",
                "ubuntu",
                privateKey.toString(),
                command -> true,
                command -> {
                    if (command.get(command.size() - 1).contains("test -f /etc/asterisk/pjsip.conf")) {
                        throw new IOException("missing pjsip.conf");
                    }
                }
        );

        AsteriskDeploymentPreflightResponse response = service.preflight(AsteriskClientType.SOFTPHONE, true);

        assertFalse(response.ready());
        assertTrue(response.remoteChecksExecuted());
        assertEquals(7, response.commands().size());
        assertTrue(response.checks().stream().anyMatch(check ->
                check.checkName().equals("remote-pjsip-conf")
                        && !check.passed()
                        && check.detail().contains("missing pjsip.conf")));
        assertTrue(response.checks().stream().anyMatch(check ->
                check.checkName().equals("remote-base-directory") && check.passed()));
    }

    @Test
    void deployWhenRemoteCommandFailsFailsAuditAndThrowsIllegalStateException(@TempDir Path tempDir) throws Exception {
        AsteriskDeploymentAuditService auditService = mock(AsteriskDeploymentAuditService.class);
        TestableAsteriskDeploymentRunnerService service = testableRunner(
                auditService,
                true,
                "pbx.acme.test",
                "ubuntu",
                "C:/keys/pbx.pem",
                command -> true,
                new CommandHandler() {
                    private int index;

                    @Override
                    public void run(List<String> command) throws IOException {
                        if (index++ == 1) {
                            throw new IOException("scp failed");
                        }
                    }
                }
        );

        AsteriskDeploymentPackageResponse deploymentPackage = deploymentPackage(tempDir);
        AsteriskDeploymentJobEntity entity = auditEntity("job-4");
        when(auditService.createPending(deploymentPackage, false)).thenReturn(entity);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.deploy(deploymentPackage, false));

        assertEquals("Failed to deploy package to Asterisk host", error.getMessage());
        assertTrue(error.getCause() instanceof IOException);
        ArgumentCaptor<AsteriskDeploymentExecutionResponse> responseCaptor = ArgumentCaptor.forClass(AsteriskDeploymentExecutionResponse.class);
        verify(auditService).fail(eq(entity), responseCaptor.capture(), eq("scp failed"));
        assertEquals("Failed while copying or applying package on server A.", responseCaptor.getValue().message());
        assertFalse(responseCaptor.getValue().deployed());
        assertEquals(2, service.executedCommands().size());
    }

    @Test
    void deployWhenInterruptedFailsAuditAndRethrows(@TempDir Path tempDir) throws Exception {
        AsteriskDeploymentAuditService auditService = mock(AsteriskDeploymentAuditService.class);
        TestableAsteriskDeploymentRunnerService service = testableRunner(
                auditService,
                true,
                "pbx.acme.test",
                "ubuntu",
                "C:/keys/pbx.pem",
                command -> true,
                command -> {
                    throw new InterruptedException("stopped");
                }
        );

        AsteriskDeploymentPackageResponse deploymentPackage = deploymentPackage(tempDir);
        AsteriskDeploymentJobEntity entity = auditEntity("job-5");
        when(auditService.createPending(deploymentPackage, false)).thenReturn(entity);

        try {
            IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.deploy(deploymentPackage, false));

            assertEquals("Interrupted while deploying package to Asterisk host", error.getMessage());
            assertTrue(error.getCause() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
            ArgumentCaptor<AsteriskDeploymentExecutionResponse> responseCaptor = ArgumentCaptor.forClass(AsteriskDeploymentExecutionResponse.class);
            verify(auditService).fail(eq(entity), responseCaptor.capture(), eq("stopped"));
            assertEquals("Interrupted while deploying package to Asterisk host.", responseCaptor.getValue().message());
            assertFalse(responseCaptor.getValue().deployed());
            assertEquals(1, service.executedCommands().size());
        } finally {
            Thread.interrupted();
        }
    }

    private AsteriskDeploymentPackageResponse deploymentPackage(Path tempDir) throws Exception {
        Path packageDir = tempDir.resolve("pkg-1");
        Files.createDirectories(packageDir);
        return new AsteriskDeploymentPackageResponse(
                "pkg-1",
                "FULL",
                "WEBRTC",
                packageDir.toString(),
                packageDir.resolve("agents.generated.conf").toString(),
                packageDir.resolve("apply-and-reload.sh").toString(),
                packageDir.resolve("manifest.json").toString(),
                Instant.parse("2026-03-22T12:01:00Z"),
                List.of("agent-1"),
                List.of("agents.generated.conf", "apply-and-reload.sh", "manifest.json")
        );
    }

    private AsteriskDeploymentJobEntity auditEntity(String deploymentJobId) {
        AsteriskDeploymentJobEntity entity = new AsteriskDeploymentJobEntity();
        entity.setDeploymentJobId(deploymentJobId);
        entity.setPackageId("pkg-1");
        entity.setPackageType("FULL");
        entity.setClientType("WEBRTC");
        return entity;
    }

    private TestableAsteriskDeploymentRunnerService testableRunner(AsteriskDeploymentAuditService auditService,
                                                                   boolean enabled,
                                                                   String host,
                                                                   String user,
                                                                   String privateKeyPath,
                                                                   CapabilityHandler capabilityHandler,
                                                                   CommandHandler commandHandler) {
        return new TestableAsteriskDeploymentRunnerService(
                auditService,
                enabled,
                host,
                22,
                user,
                privateKeyPath,
                "/tmp/vantage-asterisk",
                "/etc/asterisk/generated",
                "no",
                capabilityHandler,
                commandHandler
        );
    }

    @FunctionalInterface
    private interface CapabilityHandler {
        boolean canRun(String... command);
    }

    @FunctionalInterface
    private interface CommandHandler {
        void run(List<String> command) throws IOException, InterruptedException;
    }

    private static final class TestableAsteriskDeploymentRunnerService extends AsteriskDeploymentRunnerService {

        private final CapabilityHandler capabilityHandler;
        private final CommandHandler commandHandler;
        private final List<List<String>> executedCommands = new ArrayList<>();

        private TestableAsteriskDeploymentRunnerService(AsteriskDeploymentAuditService auditService,
                                                        boolean enabled,
                                                        String host,
                                                        int port,
                                                        String user,
                                                        String privateKeyPath,
                                                        String remoteBaseDirectory,
                                                        String targetDirectory,
                                                        String strictHostKeyChecking,
                                                        CapabilityHandler capabilityHandler,
                                                        CommandHandler commandHandler) {
            super(auditService, enabled, host, port, user, privateKeyPath, remoteBaseDirectory, targetDirectory, strictHostKeyChecking);
            this.capabilityHandler = capabilityHandler;
            this.commandHandler = commandHandler;
        }

        @Override
        boolean canRun(String... command) {
            return capabilityHandler.canRun(command);
        }

        @Override
        void run(List<String> command) throws IOException, InterruptedException {
            executedCommands.add(List.copyOf(command));
            commandHandler.run(command);
        }

        private List<List<String>> executedCommands() {
            return executedCommands;
        }
    }
}
