# Vantage Softphone Demo UI

This is the first standalone browser softphone module for testing and future reuse.

## Design

- `src/calling-core`
  - reusable call engine abstractions
  - SIP adapter interface
  - softphone client state management
- `src/hooks`
  - React integration
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

The UI expects:

- SIP domain
- WebSocket URL, usually `wss://<asterisk-host>:8089/ws`
- SIP username
- SIP password

## Integration direction

- keep `calling-core` reusable
- plug the same core into future agent UI
- expose limited supervisor controls later
