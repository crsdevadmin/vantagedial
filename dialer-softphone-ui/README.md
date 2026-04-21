# Vantage Softphone Demo UI

This is the first standalone browser softphone module for testing and future reuse.

## Design

- `src/calling-core`
  - reusable call engine abstractions
  - SIP adapter interface
  - softphone client state management
- `src/hooks`
  - React integration
- `src/backendSessionHistory.ts`
  - local backend session history parsing, trimming, and copy-brief helpers
- `src/App.tsx`
  - demo operator console with recent calls, search/filter queue controls, scheduled follow-up queue, wrap-up workspace, and activity feed

The current implementation uses a `MockSipAdapter` so the UI can be exercised immediately.
The app now supports both:

- `MockSipAdapter` for local UI testing
- `JsSipAdapter` for real Asterisk WebRTC/SIP registration

The reusable `SoftphoneClient` now also tracks:

- recent completed/failed calls
- recent-call wrap-up state for disposition and operator notes
- follow-up queue and summary rollups derived from saved dispositions
- searchable review queue with scope filters and next-follow-up focus
- scheduled callback time and priority, with queue ordering for overdue and due-today work
- quick reschedule and priority actions from both the queue cards and wrap-up workspace
- queue cards can now be worked directly with call-now and done actions, plus urgency states
- grouped callback board lanes for overdue, due-today, upcoming, and unscheduled work
- lane-level actions for focus-first, bulk reschedule, and bulk priority changes
- persistent contact policy flags for VIP, verified, and do-not-call handling
- do-not-call contacts now surface directly in queue/history and block redial actions
- persistent contact handoffs for supervisor, verification, and compliance review
- dedicated handoff desk plus contact-level handoff notes inside the wrap-up workspace
- handoff due times with overdue and due-today triage across the desk and selected contact
- generated handoff briefs with copy actions for desk items and the selected contact
- assignable handoff owner and open/claimed status, including claim/reopen actions from the desk
- resolved handoff state so completed escalations stay in contact history without remaining on the active desk
- handoff desk triage filters for mine, open, claimed, unassigned, and stale work
- handoff desk quick actions for focus-next and inline due-time presets without leaving the board
- recently resolved handoff lane with reopen, review, copy-brief, and clear actions
- contact lens history for the selected number, including repeated attempts and open follow-up context
- suggested next-step guidance for the selected contact, with one-click callback or scheduling actions
- operator activity feed for registration and call-state transitions
- locally remembered SIP host / websocket / username defaults
- persisted recent-call and activity state across browser refreshes

## Run

```bash
npm install
npm run dev
```

For real SIP/WebRTC mode:

```bash
VITE_SOFTPHONE_MODE=jssip npm run dev
```

To prefill the softphone from the dialer API customer configuration, start the UI
with the API location and customer id:

```bash
VITE_API_BASE_URL=http://localhost:8081 VITE_CUSTOMER_ID=customer-a npm run dev
```

Then use **Load customer config** in Agent Login. The UI fetches
`/customers/<customerId>` and applies the customer's SIP domain, WebSocket URL,
API base URL, and default agent UI mode while leaving the agent password manual.
If the API and UI run on different origins, set `APP_CORS_ALLOWED_ORIGINS` on
`dialer-api` to include the softphone origin.

When an operator saves wrap-up notes, the UI keeps the local history update and
also syncs the disposition, notes, priority, and follow-up time to
`PUT /outbound/sessions/<callSessionId>/wrap-up`. For backend-queued calls, the
UI matches the local softphone call back to the backend session history first so
wrap-up lands on the Vantage call session instead of an adapter-generated local
call id.

The dial pad also attempts to queue agent-assisted outbound calls through
`POST /outbound/start` using the configured Campaign ID and `PJSIP/<username>`
as the agent channel. If the API queue is unavailable, the UI falls back to the
direct softphone dial path.
Queued backend calls are tracked by polling `/outbound/sessions/<callSessionId>`
until the session reaches a terminal state, and the latest backend status is shown
beside the local softphone call state.
Recent backend-queued sessions remain visible in the Backend Session History panel
with focus, refresh, and copy actions, so operators can return to settled API
sessions or copy session context for support/debugging after the active poll has
moved on. The recent backend session list is also persisted locally across
browser refreshes.
The UI normalizes backend timestamp fields from either ISO strings or numeric
epoch-second values before storing session history, so it can tolerate both
Spring/Jackson `Instant` response shapes.

Before a browser pass, you can run the API-side smoke workflow from the repo root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ops\smoke\softphone-integration-smoke.ps1
```

The UI expects:

- SIP domain
- WebSocket URL, usually `wss://<asterisk-host>:8089/ws`
- SIP username
- SIP password

## Verify

```bash
npm test
npm run build
```

The frontend tests cover:

- softphone adapter lifecycle regressions, including duplicate session attachment and delayed cleanup
- customer configuration URL normalization and response mapping
- outbound start and wrap-up API request mapping
- backend timestamp normalization for ISO strings and numeric epoch-second values
- backend session history sanitization, matching, trimming, and copy-brief helpers

These same UI checks also run in the repository CI workflow on pushes and pull requests.

## Integration direction

- keep `calling-core` reusable
- plug the same core into future agent UI
- expose limited supervisor controls later
