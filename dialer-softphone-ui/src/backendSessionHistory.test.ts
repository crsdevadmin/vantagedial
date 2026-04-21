import { beforeEach, describe, expect, it } from "vitest";
import {
  buildBackendSessionBrief,
  findBackendSessionForCall,
  MAX_BACKEND_SESSION_HISTORY,
  readBackendSessionHistory,
  toBackendCallSession,
  upsertBackendSession,
  writeBackendSessionHistory
} from "./backendSessionHistory";
import type { BackendCallSession } from "./softphoneApi";

const storageKey = "vantage-softphone-backend-sessions";

describe("backendSessionHistory", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("sanitizes stored backend sessions and ignores malformed entries", () => {
    window.localStorage.setItem(
      storageKey,
      JSON.stringify([
        {
          callSessionId: "session-1",
          campaignId: "campaign-a",
          customerNumber: "+15551234567",
          status: "CALL_COMPLETED",
          operatorNotes: "Reached the customer",
          wrapUpUpdatedAt: "2026-04-20T10:00:00Z",
          ignored: "not part of the typed session"
        },
        { campaignId: "missing-id" },
        null
      ])
    );

    const history = readBackendSessionHistory();

    expect(history).toHaveLength(1);
    expect(history[0]).toMatchObject({
      callSessionId: "session-1",
      campaignId: "campaign-a",
      customerNumber: "+15551234567",
      status: "CALL_COMPLETED",
      operatorNotes: "Reached the customer",
      wrapUpUpdatedAt: "2026-04-20T10:00:00Z"
    });
    expect(history[0]).not.toHaveProperty("ignored");
  });

  it("returns null for invalid session-shaped data", () => {
    expect(toBackendCallSession({ campaignId: "campaign-a" })).toBeNull();
    expect(toBackendCallSession("session-1")).toBeNull();
  });

  it("normalizes numeric timestamps from persisted backend history", () => {
    window.localStorage.setItem(
      storageKey,
      JSON.stringify([
        {
          callSessionId: "session-1",
          createdAt: 1776675600,
          lastEventAt: 1776675900,
          followUpAt: 1776679200,
          wrapUpUpdatedAt: 1776679500
        }
      ])
    );

    expect(readBackendSessionHistory()[0]).toMatchObject({
      callSessionId: "session-1",
      createdAt: "2026-04-20T09:00:00.000Z",
      lastEventAt: "2026-04-20T09:05:00.000Z",
      followUpAt: "2026-04-20T10:00:00.000Z",
      wrapUpUpdatedAt: "2026-04-20T10:05:00.000Z"
    });
  });

  it("keeps the newest session first and replaces duplicate session ids", () => {
    const sessions: BackendCallSession[] = [
      { callSessionId: "session-1", status: "QUEUED" },
      { callSessionId: "session-2", status: "RINGING" }
    ];

    expect(
      upsertBackendSession(sessions, {
        callSessionId: "session-1",
        status: "CALL_COMPLETED"
      })
    ).toEqual([
      { callSessionId: "session-1", status: "CALL_COMPLETED" },
      { callSessionId: "session-2", status: "RINGING" }
    ]);
  });

  it("selects exact backend session id matches before number matches", () => {
    expect(
      findBackendSessionForCall(
        {
          id: "session-1",
          remoteIdentity: "+15551234567"
        },
        [
          {
            callSessionId: "session-2",
            customerNumber: "+15551234567",
            lastEventAt: "2026-04-20T10:00:00Z"
          },
          {
            callSessionId: "session-1",
            customerNumber: "+15550000000",
            lastEventAt: "2026-04-20T09:00:00Z"
          }
        ]
      )?.callSessionId
    ).toBe("session-1");
  });

  it("selects the newest matching backend session for the same number", () => {
    expect(
      findBackendSessionForCall(
        {
          id: "local-call-1",
          remoteIdentity: "+1 555 123 4567"
        },
        [
          {
            callSessionId: "older-session",
            customerNumber: "+15551234567",
            lastEventAt: "2026-04-20T09:00:00Z"
          },
          {
            callSessionId: "newer-session",
            customerNumber: "+15551234567",
            lastEventAt: "2026-04-20T10:00:00Z"
          }
        ]
      )?.callSessionId
    ).toBe("newer-session");
  });

  it("keeps first-seen session order when matching sessions have no timestamps", () => {
    expect(
      findBackendSessionForCall(
        {
          id: "local-call-1",
          remoteIdentity: "+15551234567"
        },
        [
          {
            callSessionId: "active-session",
            customerNumber: "+15551234567"
          },
          {
            callSessionId: "history-session",
            customerNumber: "+15551234567"
          }
        ]
      )?.callSessionId
    ).toBe("active-session");
  });

  it("limits persisted history to the configured maximum", () => {
    const sessions = Array.from({ length: MAX_BACKEND_SESSION_HISTORY + 2 }, (_, index) => ({
      callSessionId: `session-${index}`
    }));

    writeBackendSessionHistory(sessions);

    const history = readBackendSessionHistory();

    expect(history).toHaveLength(MAX_BACKEND_SESSION_HISTORY);
    expect(history[history.length - 1]?.callSessionId).toBe(
      `session-${MAX_BACKEND_SESSION_HISTORY - 1}`
    );
  });

  it("builds a copyable session brief with fallback campaign details", () => {
    expect(
      buildBackendSessionBrief(
        {
          callSessionId: "session-1",
          customerNumber: "+15551234567",
          status: "CALL_COMPLETED",
          lastEventType: "WRAP_UP",
          provider: "ASTERISK",
          agentId: "1001",
          agentChannel: "PJSIP/1001",
          createdAt: "2026-04-20T09:58:00Z",
          lastEventAt: "2026-04-20T10:00:00Z",
          wrapUpUpdatedAt: "2026-04-20T10:01:00Z"
        },
        "fallback-campaign"
      )
    ).toBe(
      [
        "Call session: session-1",
        "Customer number: +15551234567",
        "Campaign: fallback-campaign",
        "Status: CALL_COMPLETED",
        "Last event: WRAP_UP",
        "Provider: ASTERISK",
        "Agent: 1001",
        "Agent channel: PJSIP/1001",
        "Created: 2026-04-20T09:58:00Z",
        "Last event at: 2026-04-20T10:00:00Z",
        "Wrap-up synced: 2026-04-20T10:01:00Z"
      ].join("\n")
    );
  });
});
