# Vantage Dialer Ops

This folder contains production-oriented templates for running the v1 stack.

## Recommended topology

- `server-a`: Asterisk only
- `server-b`: PostgreSQL + Kafka + `dialer-api` + `dialer-worker`

## Logging

- Asterisk uses conservative file logging with log rotation.
- `dialer-api`, `dialer-worker`, and Kafka are intended to run under `systemd`.
- Service logs are viewable with `journalctl`.

## Containerized app stack

For customer replication with minimal manual setup, use the app-stack compose file from the repo root:

```bash
cp .env.app-stack.example .env
docker compose -f docker-compose.app-stack.yml up -d --build
```

This starts:

- PostgreSQL
- Kafka
- `dialer-api`
- `dialer-worker`

Only Asterisk stays on a separate host in the 2-server topology.

## Agent provisioning by curl

Create an agent:

```bash
curl -X POST http://localhost:8081/agents \
  -H "Content-Type: application/json" \
  -d "{\"agentId\":\"A10\",\"agentName\":\"Agent 10\",\"extensionNumber\":\"1010\",\"sipUsername\":\"1010\",\"sipPassword\":\"StrongPassword1010\"}"
```

Fetch the generated Asterisk config snippet:

```bash
curl http://localhost:8081/agents/A10/asterisk-config
```

Fetch a browser/WebRTC-compatible snippet:

```bash
curl "http://localhost:8081/agents/A10/asterisk-config?clientType=WEBRTC"
```

This keeps extension creation API-driven and lets you automate PJSIP config generation instead of hand-authoring each block.

Generate a deployable package for one agent:

```bash
curl -X POST http://localhost:8081/agents/A10/asterisk-package
```

Generate a browser/WebRTC-ready package for one agent:

```bash
curl -X POST "http://localhost:8081/agents/A10/asterisk-package?clientType=WEBRTC"
```

Generate a deployable package for all agents:

```bash
curl -X POST http://localhost:8081/agents/asterisk-package
```

Dry-run a remote deployment to server A and get the exact ssh/scp commands back:

```bash
curl -X POST "http://localhost:8081/agents/A10/asterisk-deploy?clientType=WEBRTC&dryRun=true"
```

Run a deployment preflight check first:

```bash
curl "http://localhost:8081/agents/asterisk-preflight?clientType=WEBRTC&performRemoteChecks=true"
```

Execute the remote deployment from server B to server A:

```bash
curl -X POST "http://localhost:8081/agents/A10/asterisk-deploy?clientType=WEBRTC&dryRun=false"
```

List deployment audit history:

```bash
curl http://localhost:8081/reports/deployments
```

Inspect one deployment audit record:

```bash
curl http://localhost:8081/reports/deployments/<deploymentJobId>
```

Platform reporting note:

- the low-level deployment audit endpoints above are still Asterisk-backed today
- the platform ops surfaces now normalize deployment data through a provider-aware model, so future backends like Cisco can plug into the same platform reporting layer more cleanly
- recent and latest platform deployment summaries now expose both provider and client-type context

Operational dashboard:

```bash
curl "http://localhost:8081/reports/dashboard?campaignId=<campaignId>"
curl -X POST "http://localhost:8081/reports/dashboard/export?campaignId=<campaignId>"
curl -X POST "http://localhost:8081/reports/dashboard/bundle?campaignId=<campaignId>"
curl -X POST "http://localhost:8081/reports/agents/activity/bundle"
curl -X POST "http://localhost:8081/reports/ivr/<campaignId>/bundle"
curl -X POST "http://localhost:8081/reports/campaigns/<campaignId>/sessions/bundle"
curl -X POST "http://localhost:8081/reports/campaigns/<campaignId>/bundle"
```

Call timeline investigation:

```bash
curl http://localhost:8081/outbound/timeline/<callSessionId>
curl -X POST http://localhost:8081/outbound/timeline/<callSessionId>/bundle
```

Run a full customer installation workflow as one tracked job:

```bash
curl -X POST http://localhost:8081/provisioning/installations \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":\"customer-a\",\"installationName\":\"customer-a-rollout\",\"useCustomerPresetDefaults\":true,\"agents\":[{\"agentId\":\"A10\",\"agentName\":\"Agent 10\",\"extensionNumber\":\"1010\",\"sipUsername\":\"1010\",\"sipPassword\":\"StrongPassword1010\"}]}"
```

When `useCustomerPresetDefaults=true`, missing install fields such as `clientType`, `performRemoteChecks`, and `deployAfterProvision` are resolved from the saved customer preset.

List installation jobs:

```bash
curl http://localhost:8081/provisioning/installations
curl "http://localhost:8081/provisioning/installations?customerId=customer-a"
```

Inspect one installation job:

```bash
curl http://localhost:8081/provisioning/installations/<installationJobId>
```

Generate a customer bootstrap bundle from an installation job:

```bash
curl -X POST http://localhost:8081/provisioning/installations/<installationJobId>/bootstrap-bundle
```

Generate a delivery handoff bundle from an installation job:

```bash
curl "http://localhost:8081/provisioning/installations/<installationJobId>/handoff"
curl -X POST "http://localhost:8081/provisioning/installations/<installationJobId>/handoff-export"
curl -X POST http://localhost:8081/provisioning/installations/<installationJobId>/handoff-bundle
curl "http://localhost:8081/provisioning/installations/<installationJobId>/delivery-package"
curl -X POST "http://localhost:8081/provisioning/installations/<installationJobId>/delivery-package/export"
curl -X POST "http://localhost:8081/provisioning/installations/<installationJobId>/delivery-package"
```

Inspect the installation dashboard:

```bash
curl "http://localhost:8081/provisioning/installations/dashboard?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/dashboard/export?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/dashboard/bundle?customerId=customer-a"
curl "http://localhost:8081/provisioning/installations/timeline?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/timeline/export?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/timeline/bundle?customerId=customer-a"
curl "http://localhost:8081/provisioning/installations/report?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/report/export?customerId=customer-a"
curl "http://localhost:8081/provisioning/installations/health?customerId=customer-a"
curl "http://localhost:8081/provisioning/installations/overview?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/health/export?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/overview/export?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/health/bundle?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/report/bundle?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/overview/bundle?customerId=customer-a"
curl "http://localhost:8081/provisioning/installations/workspace?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/workspace/export?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/workspace/bundle?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/artifacts/catalog?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/artifacts/catalog/export?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/installations/artifacts/catalog/bundle?customerId=customer-a"
```

Generate a quote-ready summary for an installation:

```bash
curl -X POST http://localhost:8081/provisioning/installations/<installationJobId>/quote-summary \
  -H "Content-Type: application/json" \
  -d "{\"monthlyCallMinutes\":10000,\"monthlyTtsUnits\":50000,\"monthlySttMinutes\":2000,\"monthlyRecordingGb\":50,\"agentCount\":10,\"concurrentChannels\":20,\"desiredMarginPercent\":30}"
```

Persist a quote snapshot and CSV export artifact:

```bash
curl -X POST http://localhost:8081/provisioning/installations/<installationJobId>/quote-snapshots \
  -H "Content-Type: application/json" \
  -d "{\"monthlyCallMinutes\":10000,\"monthlyTtsUnits\":50000,\"monthlySttMinutes\":2000,\"monthlyRecordingGb\":50,\"agentCount\":10,\"concurrentChannels\":20,\"desiredMarginPercent\":30}"

curl http://localhost:8081/provisioning/quote-snapshots
curl "http://localhost:8081/provisioning/quote-snapshots?customerId=customer-a"
curl "http://localhost:8081/provisioning/quote-snapshots/summary?customerId=customer-a"
curl "http://localhost:8081/provisioning/quote-snapshots/dashboard?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/quote-snapshots/dashboard-export?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/quote-snapshots/dashboard-bundle?customerId=customer-a"
curl "http://localhost:8081/provisioning/quote-snapshots/timeline?customerId=customer-a"
curl -X POST "http://localhost:8081/provisioning/quote-snapshots/timeline/bundle?customerId=customer-a"
curl http://localhost:8081/provisioning/quote-snapshots/<quoteSnapshotId>
curl http://localhost:8081/provisioning/quote-snapshots/<quoteSnapshotId>/assumptions
curl "http://localhost:8081/provisioning/quote-snapshots/compare?baseQuoteSnapshotId=<baseId>&targetQuoteSnapshotId=<targetId>"
curl http://localhost:8081/provisioning/quote-snapshots/<quoteSnapshotId>/compare-previous
curl -X POST http://localhost:8081/provisioning/quote-snapshots/<quoteSnapshotId>/compare-previous-bundle
curl -X POST http://localhost:8081/provisioning/quote-snapshots/<quoteSnapshotId>/bundle
curl -X POST http://localhost:8081/provisioning/quote-snapshots/<quoteSnapshotId>/proposal
```

The generated proposal artifact now includes:

- `customer-proposal.md`
- `customer-proposal.html`
- `commercial-assumptions.json`
- `pricing-breakdown.json`

The bootstrap bundle now includes:

- `installation-summary.json`
- `customer-config.json`
- `commercial-profile.json`
- `.env.app-stack`
- `agents.json`
- `.env.softphone`
- `ui-connection.json`
- `asterisk-handoff.txt`
- `README.txt`

The installation handoff bundle now includes:

- `installation.json`
- `bootstrap-bundle.json`
- `quote-summary.json`
- `quote-dashboard.json`
- `handoff.md`
- `handoff.html`
- `README.txt`

Store customer-specific infra and UI settings:

```bash
curl http://localhost:8081/customers/proposal-presets

curl -X POST http://localhost:8081/customers \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":\"customer-a\",\"customerName\":\"Customer A\",\"serverAHost\":\"asterisk.customer-a.example.com\",\"serverAPrivateIp\":\"10.0.0.10\",\"serverBHost\":\"app.customer-a.example.com\",\"asteriskDeployUser\":\"ubuntu\",\"asteriskDeployPrivateKeyPath\":\"/keys/customer-a.pem\",\"asteriskDeployTargetDirectory\":\"/etc/asterisk/generated\",\"amiUsername\":\"admin\",\"amiEndpoint\":\"vivphone-endpoint\",\"dialPrefix\":\"91\",\"sipDomain\":\"asterisk.customer-a.example.com\",\"webSocketUrl\":\"wss://asterisk.customer-a.example.com:8089/ws\",\"apiBaseUrl\":\"http://app.customer-a.example.com:8081\",\"defaultAgentUiMode\":\"jssip\",\"defaultSupervisorUiMode\":\"MONITOR_ONLY\",\"brandDisplayName\":\"Customer A Contact Center\",\"brandLogoUrl\":\"https://example.com/logo.png\",\"brandPrimaryColor\":\"#203040\",\"brandAccentColor\":\"#c46b2d\",\"proposalPreset\":\"AGENT_PLUS_IVR\",\"proposalTemplate\":\"EXECUTIVE\",\"proposalTitle\":\"Customer A Outbound Dialer Proposal\",\"proposalSubtitle\":\"Agent and IVR outbound solution proposal\",\"proposalIncludeWebRtc\":false,\"defaultMonthlyCallMinutes\":15000,\"defaultMonthlyTtsUnits\":60000,\"defaultMonthlySttMinutes\":2000,\"defaultMonthlyRecordingGb\":12.5,\"defaultAgentCount\":20,\"defaultConcurrentChannels\":35,\"defaultMarginPercent\":33,\"proposalTerms\":\"Pricing excludes telephony carrier pass-through charges unless stated otherwise.\"}"
```

Store customer-specific pricing and estimate against it:

```bash
curl -X POST http://localhost:8081/pricing/config \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":\"customer-a\",\"asteriskServerMonthlyCost\":15.18,\"appServerMonthlyCost\":30.37,\"ebsMonthlyCost\":4.00,\"snapshotMonthlyCost\":1.50,\"voiceMinuteCost\":0.015,\"ttsUnitCost\":0.00002,\"sttMinuteCost\":0.012,\"recordingGbCost\":0.10}"

curl -X POST http://localhost:8081/pricing/estimate \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":\"customer-a\",\"useCustomerPresetDefaults\":true}"
```

When `useCustomerPresetDefaults=true`, omitted pricing fields first use saved customer commercial defaults, then fall back to the saved preset.

Each package contains:

- `agents.generated.conf`
- `apply-and-reload.sh`
- `manifest.json`

For `clientType=WEBRTC`, the package also includes:

- `http.conf`
- `rtp.conf`
- `pjsip-webrtc.conf`
- `modules.conf.append`
- `dialer-softphone.env`
- `README-WEBRTC.txt`

The intended automation path is:

1. call the package API, or call the deploy API in dry-run mode
2. configure server B with ssh access to server A
3. call the deploy API with `dryRun=false`

Required app server environment for remote deployment:

- `APP_ASTERISK_DEPLOY_ENABLED=true`
- `APP_ASTERISK_DEPLOY_HOST=<server-a-host>`
- `APP_ASTERISK_DEPLOY_PORT=22`
- `APP_ASTERISK_DEPLOY_USER=<ssh-user>`
- `APP_ASTERISK_DEPLOY_PRIVATE_KEY=/path/to/private/key`
- `APP_ASTERISK_DEPLOY_REMOTE_BASE_DIRECTORY=/tmp/vantage-asterisk`
- `APP_ASTERISK_DEPLOY_TARGET_DIRECTORY=/etc/asterisk/generated`

The app server also needs `ssh` and `scp` binaries available on its PATH for remote execution.

## Suggested commands

```bash
sudo systemctl status kafka
sudo systemctl status dialer-api
sudo systemctl status dialer-worker
sudo journalctl -u dialer-api -f
sudo journalctl -u dialer-worker -f
sudo journalctl -u kafka -f
```
Customer operations workspace:
```bash
curl http://localhost:8081/provisioning/customers/customer-a/workspace
curl -X POST http://localhost:8081/provisioning/customers/customer-a/workspace/export
curl -X POST http://localhost:8081/provisioning/customers/customer-a/workspace/bundle
```
This now exposes direct health, report/catalog readiness, status message, latest installation, and latest sell-price fields.
It also now exposes direct `latestInstallationStatus` and `latestQuoteSnapshotId` fields.

Customer health:
```bash
curl http://localhost:8081/provisioning/customers/customer-a/health
curl -X POST http://localhost:8081/provisioning/customers/customer-a/health/export
curl -X POST http://localhost:8081/provisioning/customers/customer-a/health/bundle
```
This now exposes direct report-ready and artifact-catalog-ready flags.
It also now includes direct `latestInstallationName`, `latestInstallationStatus`, and `latestQuoteSnapshotId`.

Customer overview:
```bash
curl http://localhost:8081/provisioning/customers/customer-a/overview
curl -X POST http://localhost:8081/provisioning/customers/customer-a/overview/export
curl -X POST http://localhost:8081/provisioning/customers/customer-a/overview/bundle
```
This now exposes direct health, report-ready, and artifact-catalog-ready flags.
It also now promotes direct `latestInstallationName`, `latestInstallationStatus`, and `latestQuoteSnapshotId` fields.

Customer artifact catalog:
```bash
curl -X POST http://localhost:8081/provisioning/customers/customer-a/artifacts/catalog
curl -X POST http://localhost:8081/provisioning/customers/customer-a/artifacts/catalog/export
curl -X POST http://localhost:8081/provisioning/customers/customer-a/artifacts/catalog/bundle
```
This now includes customer health, workspace, overview, delivery package, report, and account-center artifacts plus direct status/install/quote fields.
It also now includes direct `latestInstallationName`, `latestInstallationStatus`, and `latestQuoteSnapshotId`.

Customer delivery package:
```bash
curl http://localhost:8081/provisioning/customers/customer-a/delivery-package
curl -X POST http://localhost:8081/provisioning/customers/customer-a/delivery-package/export
curl -X POST http://localhost:8081/provisioning/customers/customer-a/delivery-package/bundle
```
This now includes direct `latestInstallationStatus` and `latestQuoteSnapshotId`.
It also now includes direct `latestInstallationName`.

Customer report:
```bash
curl http://localhost:8081/provisioning/customers/customer-a/report
curl -X POST http://localhost:8081/provisioning/customers/customer-a/report/export
curl -X POST http://localhost:8081/provisioning/customers/customer-a/report/bundle
```
This now includes direct `latestInstallationStatus` and `latestQuoteSnapshotId`.
It also now includes direct `latestInstallationName`.

Customer account center:
```bash
curl http://localhost:8081/provisioning/customers/customer-a/account
curl -X POST http://localhost:8081/provisioning/customers/customer-a/account/export
curl -X POST http://localhost:8081/provisioning/customers/customer-a/account/bundle
```
This now exposes direct status message, latest installation id/name, and latest sell-price fields too.
It also now includes direct `latestInstallationStatus` and `latestQuoteSnapshotId`.

Customer portfolio:
```bash
curl http://localhost:8081/provisioning/customers/portfolio
curl -X POST http://localhost:8081/provisioning/customers/portfolio/export
curl -X POST http://localhost:8081/provisioning/customers/portfolio/bundle
```
This now exposes customer health, delivery/report/catalog readiness, and direct portfolio status fields.
Each portfolio entry now also includes direct `latestInstallationStatus` and `latestQuoteSnapshotId`.

Customer command center:
```bash
curl http://localhost:8081/provisioning/customers/command-center
curl -X POST http://localhost:8081/provisioning/customers/command-center/export
curl -X POST http://localhost:8081/provisioning/customers/command-center/bundle
```
This now exposes direct overall healthy/status fields, and each entry includes direct health/install/quote/delivery/report/catalog plus latest-install/latest-quote fields.
Each entry now also exposes direct `latestInstallationStatus` and `latestQuoteSnapshotId`.

Platform control center:
```bash
curl http://localhost:8081/provisioning/platform/control-center
curl -X POST http://localhost:8081/provisioning/platform/control-center/export
curl -X POST http://localhost:8081/provisioning/platform/control-center/bundle
```
This now surfaces healthy-customer, report-ready, and artifact-catalog-ready counts directly.
It also now includes direct `recentDeploymentJobIds`.
It also now includes direct `recentDeploymentPackageIds`.
It also now includes direct `recentDeploymentStatuses`.
It also now includes direct `recentDeploymentClientTypes`.
It also now includes direct `recentDeploymentHosts`.
It also now includes direct `recentDeploymentPorts`.
It also now includes direct `recentDeploymentPackageTypes`.
It also now includes direct `recentDeploymentTargetDirectories`.
It also now includes direct `recentDeploymentRemotePackageDirectories`.
It also now includes direct `recentDeploymentRemoteBaseDirectories`.
It also now includes direct `recentDeploymentDryRuns`.
It also now includes direct `recentDeploymentDeployedFlags`.
It also now includes direct `recentDeploymentAgentCounts`.
It also now includes direct `recentDeploymentBundledFileCounts`.
It also now includes direct `recentDeploymentCommandCounts`.
It also now includes direct `recentDeploymentExecutedAts`.
It also now includes direct `recentDeploymentGeneratedAts`.
It also now includes direct `recentDeploymentCreatedAts`.
It also now includes direct `recentDeploymentAts`.
It also now includes direct `recentDeploymentMessages`.
It also now includes direct `recentDeploymentErrorMessages`.
It also now includes direct `recentDeploymentAgentIds`.
It also now includes direct `recentDeploymentBundledFiles`.
It also now includes direct `recentDeploymentCommands`.
It also now includes direct `recentDeployments`.
It also now includes direct `latestDeploymentSummary`.
It also now includes direct `deploymentSnapshot`.
It also now includes direct `recentDeploymentHistory`.
It also now includes direct `deploymentStatusCounts`.
It also now includes direct `latestDeploymentDetail`.
It also now includes direct `deploymentOverview`.
It also now includes direct `latestDeploymentPackageId`.
It also now includes direct `latestDeploymentDryRun`.
It also now includes direct `latestDeploymentDeployed`.
It also now includes direct `latestDeploymentAgentCount`.
It also now includes direct `latestDeploymentAgentIds`.
It also now includes direct `latestDeploymentBundledFiles`.
It also now includes direct `latestDeploymentBundledFileCount`.
It also now includes direct `latestDeploymentCommands`.
It also now includes direct `latestDeploymentCommandCount`.
It also now includes direct `latestDeploymentExecutedAt`.
It also now includes direct `latestDeploymentGeneratedAt`.
It also now includes direct `latestDeploymentCreatedAt`.
It also now includes direct `latestDeploymentAt`.
It also now includes direct `latestDeploymentMessage`.
It also now includes direct `latestDeploymentErrorMessage`.
It also now includes direct `latestDeploymentHost`.
It also now includes direct `latestDeploymentPort`.
It also now includes direct `latestDeploymentClientType`.
It also now includes direct `latestDeploymentPackageType`.
It also now includes direct `latestDeploymentTargetDirectory`.
It also now includes direct `latestDeploymentRemotePackageDirectory`.
It also now includes direct `latestDeploymentRemoteBaseDirectory`.

Platform workspace:
```bash
curl http://localhost:8081/provisioning/platform/workspace
curl -X POST http://localhost:8081/provisioning/platform/workspace/export
curl -X POST http://localhost:8081/provisioning/platform/workspace/bundle
```
This now includes direct `healthy`, `statusMessage`, `latestDeploymentJobId`, and `latestDeploymentStatus` fields.
It also now includes direct `recentDeploymentJobIds`.
It also now includes direct `recentDeploymentPackageIds`.
It also now includes direct `recentDeploymentStatuses`.
It also now includes direct `recentDeploymentClientTypes`.
It also now includes direct `recentDeploymentHosts`.
It also now includes direct `recentDeploymentPorts`.
It also now includes direct `recentDeploymentPackageTypes`.
It also now includes direct `recentDeploymentTargetDirectories`.
It also now includes direct `recentDeploymentRemotePackageDirectories`.
It also now includes direct `recentDeploymentRemoteBaseDirectories`.
It also now includes direct `recentDeploymentDryRuns`.
It also now includes direct `recentDeploymentDeployedFlags`.
It also now includes direct `recentDeploymentAgentCounts`.
It also now includes direct `recentDeploymentBundledFileCounts`.
It also now includes direct `recentDeploymentCommandCounts`.
It also now includes direct `recentDeploymentExecutedAts`.
It also now includes direct `recentDeploymentGeneratedAts`.
It also now includes direct `recentDeploymentCreatedAts`.
It also now includes direct `recentDeploymentAts`.
It also now includes direct `recentDeploymentMessages`.
It also now includes direct `recentDeploymentErrorMessages`.
It also now includes direct `recentDeploymentAgentIds`.
It also now includes direct `recentDeploymentBundledFiles`.
It also now includes direct `recentDeploymentCommands`.
It also now includes direct `recentDeployments`.
It also now includes direct `latestDeploymentSummary`.
It also now includes direct `deploymentSnapshot`.
It also now includes direct `recentDeploymentHistory`.
It also now includes direct `deploymentStatusCounts`.
It also now includes direct `latestDeploymentDetail`.
It also now includes direct `deploymentOverview`.
It also now includes direct `latestDeploymentPackageId`.
It also now includes direct `latestDeploymentDryRun`.
It also now includes direct `latestDeploymentDeployed`.
It also now includes direct `latestDeploymentAgentCount`.
It also now includes direct `latestDeploymentAgentIds`.
It also now includes direct `latestDeploymentBundledFiles`.
It also now includes direct `latestDeploymentBundledFileCount`.
It also now includes direct `latestDeploymentCommands`.
It also now includes direct `latestDeploymentCommandCount`.
It also now includes direct `latestDeploymentExecutedAt`.
It also now includes direct `latestDeploymentGeneratedAt`.
It also now includes direct `latestDeploymentCreatedAt`.
It also now includes direct `latestDeploymentAt`.
It also now includes direct `latestDeploymentMessage`.
It also now includes direct `latestDeploymentErrorMessage`.
It also now includes direct `latestDeploymentHost`.
It also now includes direct `latestDeploymentPort`.
It also now includes direct `latestDeploymentClientType`.
It also now includes direct `latestDeploymentPackageType`.
It also now includes direct `latestDeploymentTargetDirectory`.
It also now includes direct `latestDeploymentRemotePackageDirectory`.
It also now includes direct `latestDeploymentRemoteBaseDirectory`.

Platform artifact catalog:
```bash
curl -X POST http://localhost:8081/provisioning/platform/artifacts/catalog
curl -X POST http://localhost:8081/provisioning/platform/artifacts/catalog/export
curl -X POST http://localhost:8081/provisioning/platform/artifacts/catalog/bundle
```
This now includes top-level customer readiness totals plus direct healthy/status/latest-deployment fields.
It also now includes direct `recentDeploymentJobIds`.
It also now includes direct `recentDeploymentPackageIds`.
It also now includes direct `recentDeploymentStatuses`.
It also now includes direct `recentDeploymentClientTypes`.
It also now includes direct `recentDeploymentHosts`.
It also now includes direct `recentDeploymentPorts`.
It also now includes direct `recentDeploymentPackageTypes`.
It also now includes direct `recentDeploymentTargetDirectories`.
It also now includes direct `recentDeploymentRemotePackageDirectories`.
It also now includes direct `recentDeploymentRemoteBaseDirectories`.
It also now includes direct `recentDeploymentDryRuns`.
It also now includes direct `recentDeploymentDeployedFlags`.
It also now includes direct `recentDeploymentAgentCounts`.
It also now includes direct `recentDeploymentBundledFileCounts`.
It also now includes direct `recentDeploymentCommandCounts`.
It also now includes direct `recentDeploymentExecutedAts`.
It also now includes direct `recentDeploymentGeneratedAts`.
It also now includes direct `recentDeploymentCreatedAts`.
It also now includes direct `recentDeploymentAts`.
It also now includes direct `recentDeploymentMessages`.
It also now includes direct `recentDeploymentErrorMessages`.
It also now includes direct `recentDeploymentAgentIds`.
It also now includes direct `recentDeploymentBundledFiles`.
It also now includes direct `recentDeploymentCommands`.
It also now includes direct `recentDeployments`.
It also now includes direct `latestDeploymentSummary`.
It also now includes direct `deploymentSnapshot`.
It also now includes direct `recentDeploymentHistory`.
It also now includes direct `deploymentStatusCounts`.
It also now includes direct `latestDeploymentDetail`.
It also now includes direct `deploymentOverview`.
It also now includes direct `latestDeploymentPackageId`.
It also now includes direct `latestDeploymentDryRun`.
It also now includes direct `latestDeploymentDeployed`.
It also now includes direct `latestDeploymentAgentCount`.
It also now includes direct `latestDeploymentAgentIds`.
It also now includes direct `latestDeploymentBundledFiles`.
It also now includes direct `latestDeploymentBundledFileCount`.
It also now includes direct `latestDeploymentCommands`.
It also now includes direct `latestDeploymentCommandCount`.
It also now includes direct `latestDeploymentExecutedAt`.
It also now includes direct `latestDeploymentGeneratedAt`.
It also now includes direct `latestDeploymentCreatedAt`.
It also now includes direct `latestDeploymentAt`.
It also now includes direct `latestDeploymentMessage`.
It also now includes direct `latestDeploymentErrorMessage`.
It also now includes direct `latestDeploymentHost`.
It also now includes direct `latestDeploymentPort`.
It also now includes direct `latestDeploymentClientType`.
It also now includes direct `latestDeploymentPackageType`.
It also now includes direct `latestDeploymentTargetDirectory`.
It also now includes direct `latestDeploymentRemotePackageDirectory`.
It also now includes direct `latestDeploymentRemoteBaseDirectory`.

Platform delivery package:
```bash
curl http://localhost:8081/provisioning/platform/delivery-package
curl -X POST http://localhost:8081/provisioning/platform/delivery-package/export
curl -X POST http://localhost:8081/provisioning/platform/delivery-package
```
This now carries direct healthy/report/catalog readiness counters in addition to the nested payloads.
It also now includes direct `recentDeploymentJobIds`.
It also now includes direct `recentDeploymentPackageIds`.
It also now includes direct `recentDeploymentStatuses`.
It also now includes direct `recentDeploymentClientTypes`.
It also now includes direct `recentDeploymentHosts`.
It also now includes direct `recentDeploymentPorts`.
It also now includes direct `recentDeploymentPackageTypes`.
It also now includes direct `recentDeploymentTargetDirectories`.
It also now includes direct `recentDeploymentRemotePackageDirectories`.
It also now includes direct `recentDeploymentRemoteBaseDirectories`.
It also now includes direct `recentDeploymentDryRuns`.
It also now includes direct `recentDeploymentDeployedFlags`.
It also now includes direct `recentDeploymentAgentCounts`.
It also now includes direct `recentDeploymentBundledFileCounts`.
It also now includes direct `recentDeploymentCommandCounts`.
It also now includes direct `recentDeploymentExecutedAts`.
It also now includes direct `recentDeploymentGeneratedAts`.
It also now includes direct `recentDeploymentCreatedAts`.
It also now includes direct `recentDeploymentAts`.
It also now includes direct `recentDeploymentMessages`.
It also now includes direct `recentDeploymentErrorMessages`.
It also now includes direct `recentDeploymentAgentIds`.
It also now includes direct `recentDeploymentBundledFiles`.
It also now includes direct `recentDeploymentCommands`.
It also now includes direct `recentDeployments`.
It also now includes direct `latestDeploymentSummary`.
It also now includes direct `deploymentSnapshot`.
It also now includes direct `recentDeploymentHistory`.
It also now includes direct `deploymentStatusCounts`.
It also now includes direct `latestDeploymentDetail`.
It also now includes direct `deploymentOverview`.
It also now includes direct `latestDeploymentPackageId`.
It also now includes direct `latestDeploymentDryRun`.
It also now includes direct `latestDeploymentDeployed`.
It also now includes direct `latestDeploymentAgentCount`.
It also now includes direct `latestDeploymentAgentIds`.
It also now includes direct `latestDeploymentBundledFiles`.
It also now includes direct `latestDeploymentBundledFileCount`.
It also now includes direct `latestDeploymentCommands`.
It also now includes direct `latestDeploymentCommandCount`.
It also now includes direct `latestDeploymentExecutedAt`.
It also now includes direct `latestDeploymentGeneratedAt`.
It also now includes direct `latestDeploymentCreatedAt`.
It also now includes direct `latestDeploymentAt`.
It also now includes direct `latestDeploymentMessage`.
It also now includes direct `latestDeploymentErrorMessage`.
It also now includes direct `latestDeploymentHost`.
It also now includes direct `latestDeploymentPort`.
It also now includes direct `latestDeploymentClientType`.
It also now includes direct `latestDeploymentPackageType`.
It also now includes direct `latestDeploymentTargetDirectory`.
It also now includes direct `latestDeploymentRemotePackageDirectory`.
It also now includes direct `latestDeploymentRemoteBaseDirectory`.

Platform report:
```bash
curl http://localhost:8081/provisioning/platform/report
curl -X POST http://localhost:8081/provisioning/platform/report/export
curl -X POST http://localhost:8081/provisioning/platform/report/bundle
```
This now exposes direct healthy/status/latest-deployment fields too.
It also now includes direct `recentDeploymentJobIds`.
It also now includes direct `recentDeploymentPackageIds`.
It also now includes direct `recentDeploymentStatuses`.
It also now includes direct `recentDeploymentClientTypes`.
It also now includes direct `recentDeploymentHosts`.
It also now includes direct `recentDeploymentPorts`.
It also now includes direct `recentDeploymentPackageTypes`.
It also now includes direct `recentDeploymentTargetDirectories`.
It also now includes direct `recentDeploymentRemotePackageDirectories`.
It also now includes direct `recentDeploymentRemoteBaseDirectories`.
It also now includes direct `recentDeploymentDryRuns`.
It also now includes direct `recentDeploymentDeployedFlags`.
It also now includes direct `recentDeploymentAgentCounts`.
It also now includes direct `recentDeploymentBundledFileCounts`.
It also now includes direct `recentDeploymentCommandCounts`.
It also now includes direct `recentDeploymentExecutedAts`.
It also now includes direct `recentDeploymentGeneratedAts`.
It also now includes direct `recentDeploymentCreatedAts`.
It also now includes direct `recentDeploymentAts`.
It also now includes direct `recentDeploymentMessages`.
It also now includes direct `recentDeploymentErrorMessages`.
It also now includes direct `recentDeploymentAgentIds`.
It also now includes direct `recentDeploymentBundledFiles`.
It also now includes direct `recentDeploymentCommands`.
It also now includes direct `recentDeployments`.
It also now includes direct `latestDeploymentSummary`.
It also now includes direct `deploymentSnapshot`.
It also now includes direct `recentDeploymentHistory`.
It also now includes direct `deploymentStatusCounts`.
It also now includes direct `latestDeploymentDetail`.
It also now includes direct `deploymentOverview`.
It also now includes direct `latestDeploymentPackageId`.
It also now includes direct `latestDeploymentDryRun`.
It also now includes direct `latestDeploymentDeployed`.
It also now includes direct `latestDeploymentAgentCount`.
It also now includes direct `latestDeploymentAgentIds`.
It also now includes direct `latestDeploymentBundledFiles`.
It also now includes direct `latestDeploymentBundledFileCount`.
It also now includes direct `latestDeploymentCommands`.
It also now includes direct `latestDeploymentCommandCount`.
It also now includes direct `latestDeploymentExecutedAt`.
It also now includes direct `latestDeploymentGeneratedAt`.
It also now includes direct `latestDeploymentCreatedAt`.
It also now includes direct `latestDeploymentAt`.
It also now includes direct `latestDeploymentMessage`.
It also now includes direct `latestDeploymentErrorMessage`.
It also now includes direct `latestDeploymentHost`.
It also now includes direct `latestDeploymentPort`.
It also now includes direct `latestDeploymentClientType`.
It also now includes direct `latestDeploymentPackageType`.
It also now includes direct `latestDeploymentTargetDirectory`.
It also now includes direct `latestDeploymentRemotePackageDirectory`.
It also now includes direct `latestDeploymentRemoteBaseDirectory`.

Platform health:
```bash
curl http://localhost:8081/provisioning/platform/health
curl -X POST http://localhost:8081/provisioning/platform/health/export
curl -X POST http://localhost:8081/provisioning/platform/health/bundle
```
This now includes direct `latestDeploymentAt`.
It also now includes direct `recentDeploymentJobIds`.
It also now includes direct `recentDeploymentPackageIds`.
It also now includes direct `recentDeploymentStatuses`.
It also now includes direct `recentDeploymentClientTypes`.
It also now includes direct `recentDeploymentHosts`.
It also now includes direct `recentDeploymentPorts`.
It also now includes direct `recentDeploymentPackageTypes`.
It also now includes direct `recentDeploymentTargetDirectories`.
It also now includes direct `recentDeploymentRemotePackageDirectories`.
It also now includes direct `recentDeploymentRemoteBaseDirectories`.
It also now includes direct `recentDeploymentDryRuns`.
It also now includes direct `recentDeploymentDeployedFlags`.
It also now includes direct `recentDeploymentAgentCounts`.
It also now includes direct `recentDeploymentBundledFileCounts`.
It also now includes direct `recentDeploymentCommandCounts`.
It also now includes direct `recentDeploymentExecutedAts`.
It also now includes direct `recentDeploymentGeneratedAts`.
It also now includes direct `recentDeploymentCreatedAts`.
It also now includes direct `recentDeploymentAts`.
It also now includes direct `recentDeploymentMessages`.
It also now includes direct `recentDeploymentErrorMessages`.
It also now includes direct `recentDeploymentAgentIds`.
It also now includes direct `recentDeploymentBundledFiles`.
It also now includes direct `recentDeploymentCommands`.
It also now includes direct `recentDeployments`.
It also now includes direct `latestDeploymentSummary`.
It also now includes direct `deploymentSnapshot`.
It also now includes direct `recentDeploymentHistory`.
It also now includes direct `deploymentStatusCounts`.
It also now includes direct `latestDeploymentDetail`.
It also now includes direct `deploymentOverview`.
It also now includes direct `latestDeploymentPackageId`.
It also now includes direct `latestDeploymentDryRun`.
It also now includes direct `latestDeploymentDeployed`.
It also now includes direct `latestDeploymentAgentCount`.
It also now includes direct `latestDeploymentAgentIds`.
It also now includes direct `latestDeploymentBundledFiles`.
It also now includes direct `latestDeploymentBundledFileCount`.
It also now includes direct `latestDeploymentCommands`.
It also now includes direct `latestDeploymentCommandCount`.
It also now includes direct `latestDeploymentExecutedAt`.
It also now includes direct `latestDeploymentGeneratedAt`.
It also now includes direct `latestDeploymentCreatedAt`.
It also now includes direct `latestDeploymentMessage`.
It also now includes direct `latestDeploymentErrorMessage`.
It also now includes direct `latestDeploymentHost`.
It also now includes direct `latestDeploymentPort`.
It also now includes direct `latestDeploymentClientType`.
It also now includes direct `latestDeploymentPackageType`.
It also now includes direct `latestDeploymentTargetDirectory`.
It also now includes direct `latestDeploymentRemotePackageDirectory`.
It also now includes direct `latestDeploymentRemoteBaseDirectory`.

Platform overview:
```bash
curl http://localhost:8081/provisioning/platform/overview
curl -X POST http://localhost:8081/provisioning/platform/overview/export
curl -X POST http://localhost:8081/provisioning/platform/overview/bundle
```
This now exposes healthy/report/catalog readiness counters directly in the overview payload.
It also now includes direct `recentDeploymentJobIds`.
It also now includes direct `recentDeploymentPackageIds`.
It also now includes direct `recentDeploymentStatuses`.
It also now includes direct `recentDeploymentClientTypes`.
It also now includes direct `recentDeploymentHosts`.
It also now includes direct `recentDeploymentPorts`.
It also now includes direct `recentDeploymentPackageTypes`.
It also now includes direct `recentDeploymentTargetDirectories`.
It also now includes direct `recentDeploymentRemotePackageDirectories`.
It also now includes direct `recentDeploymentRemoteBaseDirectories`.
It also now includes direct `recentDeploymentDryRuns`.
It also now includes direct `recentDeploymentDeployedFlags`.
It also now includes direct `recentDeploymentAgentCounts`.
It also now includes direct `recentDeploymentBundledFileCounts`.
It also now includes direct `recentDeploymentCommandCounts`.
It also now includes direct `recentDeploymentExecutedAts`.
It also now includes direct `recentDeploymentGeneratedAts`.
It also now includes direct `recentDeploymentCreatedAts`.
It also now includes direct `recentDeploymentAts`.
It also now includes direct `recentDeploymentMessages`.
It also now includes direct `recentDeploymentErrorMessages`.
It also now includes direct `recentDeploymentAgentIds`.
It also now includes direct `recentDeploymentBundledFiles`.
It also now includes direct `recentDeploymentCommands`.
It also now includes direct `recentDeployments`.
It also now includes direct `latestDeploymentSummary`.
It also now includes direct `deploymentSnapshot`.
It also now includes direct `recentDeploymentHistory`.
It also now includes direct `deploymentStatusCounts`.
It also now includes direct `latestDeploymentDetail`.
It also now includes direct `deploymentOverview`.
It also now includes direct `latestDeploymentPackageId`.
It also now includes direct `latestDeploymentDryRun`.
It also now includes direct `latestDeploymentDeployed`.
It also now includes direct `latestDeploymentAgentCount`.
It also now includes direct `latestDeploymentAgentIds`.
It also now includes direct `latestDeploymentBundledFiles`.
It also now includes direct `latestDeploymentBundledFileCount`.
It also now includes direct `latestDeploymentCommands`.
It also now includes direct `latestDeploymentCommandCount`.
It also now includes direct `latestDeploymentExecutedAt`.
It also now includes direct `latestDeploymentGeneratedAt`.
It also now includes direct `latestDeploymentCreatedAt`.
It also now includes direct `latestDeploymentAt`.
It also now includes direct `latestDeploymentMessage`.
It also now includes direct `latestDeploymentErrorMessage`.
It also now includes direct `latestDeploymentHost`.
It also now includes direct `latestDeploymentPort`.
It also now includes direct `latestDeploymentClientType`.
It also now includes direct `latestDeploymentPackageType`.
It also now includes direct `latestDeploymentTargetDirectory`.
It also now includes direct `latestDeploymentRemotePackageDirectory`.
It also now includes direct `latestDeploymentRemoteBaseDirectory`.
