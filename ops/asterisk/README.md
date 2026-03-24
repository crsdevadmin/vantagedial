# Asterisk WebRTC Setup

These templates are the starting point for browser softphone support with the
`dialer-softphone-ui` JsSIP adapter.

## Files

- `http.conf`
  - enables Asterisk HTTP and WebSocket listener
- `rtp.conf`
  - enables ICE-friendly RTP range
- `pjsip-webrtc.conf`
  - defines `transport-wss` and a reusable WebRTC endpoint template
- `modules.conf.append`
  - modules commonly required for WebRTC over WebSocket

## Apply

Copy the files to the Asterisk host and merge with your active configs:

```bash
sudo cp http.conf /etc/asterisk/http.conf
sudo cp rtp.conf /etc/asterisk/rtp.conf
sudo cp pjsip-webrtc.conf /etc/asterisk/pjsip-webrtc.conf
```

Reference `pjsip-webrtc.conf` from your main `pjsip.conf` if needed:

```ini
#include pjsip-webrtc.conf
#include generated/agents.generated.conf
```

Reload:

```bash
sudo asterisk -rx "module reload"
sudo asterisk -rx "http show status"
sudo asterisk -rx "pjsip reload"
```

## API-driven config generation

For a browser/WebRTC-compatible agent snippet:

```bash
curl "http://localhost:8081/agents/A10/asterisk-config?clientType=WEBRTC"
```

For a classic desktop/mobile SIP snippet:

```bash
curl "http://localhost:8081/agents/A10/asterisk-config?clientType=SOFTPHONE"
```

To generate a deployable WebRTC package:

```bash
curl -X POST "http://localhost:8081/agents/A10/asterisk-package?clientType=WEBRTC"
```

To dry-run the remote deploy workflow from server B:

```bash
curl -X POST "http://localhost:8081/agents/A10/asterisk-deploy?clientType=WEBRTC&dryRun=true"
```

To check server B -> server A deployment readiness first:

```bash
curl "http://localhost:8081/agents/asterisk-preflight?clientType=WEBRTC&performRemoteChecks=true"
```

To execute the remote deploy workflow:

```bash
curl -X POST "http://localhost:8081/agents/A10/asterisk-deploy?clientType=WEBRTC&dryRun=false"
```

That package includes:

- `agents.generated.conf`
- `http.conf`
- `rtp.conf`
- `pjsip-webrtc.conf`
- `modules.conf.append`
- `dialer-softphone.env`
- `README-WEBRTC.txt`

## Browser client expectations

The softphone UI in `dialer-softphone-ui` expects:

- SIP domain: your Asterisk host/domain
- WebSocket endpoint: typically `wss://your-host:8089/ws`
- valid TLS for browser access

If you terminate TLS in front of Asterisk, update the WebSocket URL accordingly.
