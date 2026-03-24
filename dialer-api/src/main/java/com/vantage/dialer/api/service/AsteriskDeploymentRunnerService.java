package com.vantage.dialer.api.service;

import com.vantage.dialer.api.dto.AsteriskDeploymentExecutionResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPackageResponse;
import com.vantage.dialer.api.dto.AsteriskDeploymentPreflightResponse;
import com.vantage.dialer.api.dto.AsteriskPreflightCheckResponse;
import com.vantage.dialer.api.dto.AsteriskClientType;
import com.vantage.dialer.api.persistence.model.AsteriskDeploymentJobEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AsteriskDeploymentRunnerService {

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String user;
    private final String privateKeyPath;
    private final String remoteBaseDirectory;
    private final String targetDirectory;
    private final String strictHostKeyChecking;
    private final AsteriskDeploymentAuditService auditService;

    public AsteriskDeploymentRunnerService(
            AsteriskDeploymentAuditService auditService,
            @Value("${app.asterisk.deploy.enabled:false}") boolean enabled,
            @Value("${app.asterisk.deploy.host:}") String host,
            @Value("${app.asterisk.deploy.port:22}") int port,
            @Value("${app.asterisk.deploy.user:ubuntu}") String user,
            @Value("${app.asterisk.deploy.private-key:}") String privateKeyPath,
            @Value("${app.asterisk.deploy.remote-base-directory:/tmp/vantage-asterisk}") String remoteBaseDirectory,
            @Value("${app.asterisk.deploy.target-directory:/etc/asterisk/generated}") String targetDirectory,
            @Value("${app.asterisk.deploy.strict-host-key-checking:no}") String strictHostKeyChecking) {
        this.auditService = auditService;
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.user = user;
        this.privateKeyPath = privateKeyPath;
        this.remoteBaseDirectory = remoteBaseDirectory;
        this.targetDirectory = targetDirectory;
        this.strictHostKeyChecking = strictHostKeyChecking;
    }

    public AsteriskDeploymentExecutionResponse deploy(AsteriskDeploymentPackageResponse deploymentPackage, boolean dryRun) {
        String remotePackageDirectory = remoteBaseDirectory + "/" + deploymentPackage.packageId();
        List<List<String>> commands = buildCommands(deploymentPackage, remotePackageDirectory);
        List<String> printableCommands = commands.stream().map(this::sanitize).toList();
        AsteriskDeploymentJobEntity auditRecord = auditService.createPending(deploymentPackage, dryRun);

        if (dryRun) {
            AsteriskDeploymentExecutionResponse response = buildResponse(deploymentPackage, auditRecord, true, false, remotePackageDirectory, printableCommands, Instant.now(),
                    "Dry run only. Review commands and execute when ready.");
            auditService.complete(auditRecord, response);
            return response;
        }

        if (!enabled) {
            AsteriskDeploymentExecutionResponse response = buildResponse(deploymentPackage, auditRecord, false, false, remotePackageDirectory, printableCommands, Instant.now(),
                    "Remote deployment is disabled. Set app.asterisk.deploy.enabled=true to allow ssh/scp execution.");
            auditService.fail(auditRecord, response, response.message());
            return response;
        }

        Instant executedAt = Instant.now();
        try {
            requireConfig();
            for (List<String> command : commands) {
                run(command);
            }
            AsteriskDeploymentExecutionResponse response = buildResponse(deploymentPackage, auditRecord, false, true, remotePackageDirectory, printableCommands, executedAt,
                    "Package copied to server A and apply-and-reload.sh executed successfully.");
            auditService.complete(auditRecord, response);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AsteriskDeploymentExecutionResponse response = buildResponse(deploymentPackage, auditRecord, false, false, remotePackageDirectory, printableCommands, Instant.now(),
                    "Interrupted while deploying package to Asterisk host.");
            auditService.fail(auditRecord, response, e.getMessage());
            throw new IllegalStateException("Interrupted while deploying package to Asterisk host", e);
        } catch (IOException e) {
            AsteriskDeploymentExecutionResponse response = buildResponse(deploymentPackage, auditRecord, false, false, remotePackageDirectory, printableCommands, Instant.now(),
                    "Failed while copying or applying package on server A.");
            auditService.fail(auditRecord, response, e.getMessage());
            throw new IllegalStateException("Failed to deploy package to Asterisk host", e);
        } catch (RuntimeException e) {
            AsteriskDeploymentExecutionResponse response = buildResponse(deploymentPackage, auditRecord, false, false, remotePackageDirectory, printableCommands, Instant.now(),
                    "Deployment configuration validation failed.");
            auditService.fail(auditRecord, response, e.getMessage());
            throw e;
        }
    }

    public AsteriskDeploymentPreflightResponse preflight(AsteriskClientType clientType, boolean performRemoteChecks) {
        List<AsteriskPreflightCheckResponse> checks = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        checks.add(check("deployment-enabled", enabled,
                enabled ? "Remote deployment is enabled." : "Set APP_ASTERISK_DEPLOY_ENABLED=true to allow remote execution."));
        checks.add(check("deploy-host-configured", host != null && !host.isBlank(),
                blankFallback(host, "APP_ASTERISK_DEPLOY_HOST is not configured.")));
        checks.add(check("deploy-user-configured", user != null && !user.isBlank(),
                blankFallback(user, "APP_ASTERISK_DEPLOY_USER is not configured.")));
        checks.add(check("ssh-binary-available", canRun("ssh", "-V"),
                "ssh must be installed on server B and available on PATH."));
        checks.add(check("scp-binary-available", canRun("scp", "-V"),
                "scp must be installed on server B and available on PATH."));

        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            checks.add(check("private-key-exists", Files.exists(Path.of(privateKeyPath)),
                    "Configured private key path: " + privateKeyPath));
        } else {
            checks.add(check("private-key-configured", false,
                    "APP_ASTERISK_DEPLOY_PRIVATE_KEY is not configured."));
        }

        boolean configReady = checks.stream().allMatch(AsteriskPreflightCheckResponse::passed);
        boolean remoteExecuted = false;

        if (performRemoteChecks && configReady) {
            remoteExecuted = true;
            checks.add(runRemoteCheck(commands, "remote-asterisk-binary", "command -v asterisk >/dev/null",
                    "Asterisk binary is available on server A."));
            checks.add(runRemoteCheck(commands, "remote-sudo-binary", "command -v sudo >/dev/null",
                    "sudo is available on server A."));
            checks.add(runRemoteCheck(commands, "remote-etc-asterisk", "test -d /etc/asterisk",
                    "/etc/asterisk exists on server A."));
            checks.add(runRemoteCheck(commands, "remote-pjsip-conf", "test -f /etc/asterisk/pjsip.conf",
                    "/etc/asterisk/pjsip.conf exists on server A."));
            checks.add(runRemoteCheck(commands, "remote-generated-parent", "sudo test -d " + shellQuote(parentDirectory(targetDirectory)),
                    parentDirectory(targetDirectory) + " exists and is visible to sudo on server A."));
            checks.add(runRemoteCheck(commands, "remote-base-directory", "mkdir -p " + shellQuote(remoteBaseDirectory) + " && test -d " + shellQuote(remoteBaseDirectory),
                    remoteBaseDirectory + " is accessible on server A."));
            checks.add(runRemoteCheck(commands, "remote-pjsip-generated-include",
                    "grep -F \"#include generated/agents.generated.conf\" /etc/asterisk/pjsip.conf >/dev/null",
                    "pjsip.conf includes generated/agents.generated.conf."));

            if (clientType == AsteriskClientType.WEBRTC) {
                checks.add(runRemoteCheck(commands, "remote-pjsip-webrtc-include",
                        "grep -F \"#include pjsip-webrtc.conf\" /etc/asterisk/pjsip.conf >/dev/null",
                        "pjsip.conf includes pjsip-webrtc.conf."));
                checks.add(runRemoteCheck(commands, "remote-pjsip-webrtc-file",
                        "test -f /etc/asterisk/pjsip-webrtc.conf",
                        "/etc/asterisk/pjsip-webrtc.conf exists on server A."));
                checks.add(runRemoteCheck(commands, "remote-http-conf",
                        "test -f /etc/asterisk/http.conf",
                        "/etc/asterisk/http.conf exists on server A."));
                checks.add(runRemoteCheck(commands, "remote-http-ws-enabled",
                        "grep -F \"[ws]\" /etc/asterisk/http.conf >/dev/null && grep -F \"enabled=yes\" /etc/asterisk/http.conf >/dev/null",
                        "http.conf contains an enabled [ws] section."));
                checks.add(runRemoteCheck(commands, "remote-webrtc-transport",
                        "grep -F \"[transport-wss]\" /etc/asterisk/pjsip-webrtc.conf >/dev/null",
                        "pjsip-webrtc.conf defines transport-wss."));
                checks.add(runRemoteCheck(commands, "remote-webrtc-modules",
                        "sudo asterisk -rx \"module show like res_http_websocket\" | grep -F \"1 modules loaded\" >/dev/null",
                        "res_http_websocket module is loaded in Asterisk."));
            }
        }

        boolean ready = checks.stream().allMatch(AsteriskPreflightCheckResponse::passed);
        return new AsteriskDeploymentPreflightResponse(
                ready,
                remoteExecuted,
                enabled,
                host,
                port,
                user,
                remoteBaseDirectory,
                targetDirectory,
                checks,
                commands,
                Instant.now()
        );
    }

    private void requireConfig() {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("Asterisk deploy host is not configured");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalStateException("Asterisk deploy user is not configured");
        }
    }

    boolean canRun(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private AsteriskPreflightCheckResponse check(String name, boolean passed, String detail) {
        return new AsteriskPreflightCheckResponse(name, passed, detail);
    }

    private AsteriskPreflightCheckResponse runRemoteCheck(List<String> commands,
                                                          String name,
                                                          String remoteCommand,
                                                          String successDetail) {
        List<String> command = buildSshCommand(remoteCommand);
        commands.add(sanitize(command));
        try {
            run(command);
            return check(name, true, successDetail);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return check(name, false, e.getMessage());
        }
    }

    private String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String parentDirectory(String value) {
        Path path = Path.of(value);
        Path parent = path.getParent();
        return parent == null ? value : parent.toString();
    }

    private AsteriskDeploymentExecutionResponse buildResponse(AsteriskDeploymentPackageResponse deploymentPackage,
                                                              AsteriskDeploymentJobEntity auditRecord,
                                                              boolean dryRun,
                                                              boolean deployed,
                                                              String remotePackageDirectory,
                                                              List<String> commands,
                                                              Instant executedAt,
                                                              String message) {
        return new AsteriskDeploymentExecutionResponse(
                auditRecord.getDeploymentJobId(),
                deploymentPackage.packageId(),
                deploymentPackage.packageType(),
                deploymentPackage.clientType(),
                dryRun,
                deployed,
                host,
                port,
                remoteBaseDirectory,
                remotePackageDirectory,
                targetDirectory,
                commands,
                deploymentPackage.bundledFiles(),
                deploymentPackage.generatedAt(),
                executedAt,
                message
        );
    }

    private List<List<String>> buildCommands(AsteriskDeploymentPackageResponse deploymentPackage, String remotePackageDirectory) {
        Path packageDirectory = Path.of(deploymentPackage.packageDirectory());

        List<List<String>> commands = new ArrayList<>();
        commands.add(buildSshCommand("mkdir -p " + shellQuote(remoteBaseDirectory)));
        commands.add(buildScpCommand(packageDirectory));
        commands.add(buildSshCommand(
                "chmod +x " + shellQuote(remotePackageDirectory + "/apply-and-reload.sh") +
                        " && " + shellQuote(remotePackageDirectory + "/apply-and-reload.sh") +
                        " " + shellQuote(targetDirectory)));
        return commands;
    }

    private List<String> buildSshCommand(String remoteCommand) {
        List<String> command = new ArrayList<>();
        command.add("ssh");
        appendCommonSshArgs(command);
        command.add(remoteUserHost());
        command.add(remoteCommand);
        return command;
    }

    private List<String> buildScpCommand(Path packageDirectory) {
        List<String> command = new ArrayList<>();
        command.add("scp");
        command.add("-r");
        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            command.add("-i");
            command.add(privateKeyPath);
        }
        command.add("-P");
        command.add(String.valueOf(port));
        command.add("-o");
        command.add("StrictHostKeyChecking=" + strictHostKeyChecking);
        command.add(packageDirectory.toString());
        command.add(remoteUserHost() + ":" + remoteBaseDirectory);
        return command;
    }

    private void appendCommonSshArgs(List<String> command) {
        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            command.add("-i");
            command.add(privateKeyPath);
        }
        command.add("-p");
        command.add(String.valueOf(port));
        command.add("-o");
        command.add("StrictHostKeyChecking=" + strictHostKeyChecking);
    }

    private String remoteUserHost() {
        return user + "@" + host;
    }

    private String sanitize(List<String> command) {
        List<String> sanitized = new ArrayList<>();
        for (int i = 0; i < command.size(); i++) {
            String token = command.get(i);
            sanitized.add(token);
            if (("-i".equals(token) || token.startsWith("-i")) && i + 1 < command.size()) {
                sanitized.add("<private-key>");
                i++;
            }
        }
        return String.join(" ", sanitized);
    }

    void run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String output = new String(process.getInputStream().readAllBytes());
            throw new IOException("Command failed with exit code " + exitCode + ": " + sanitize(command) + System.lineSeparator() + output);
        }
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
