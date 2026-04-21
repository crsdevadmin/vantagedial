import { afterEach, describe, expect, it, vi } from "vitest";
import {
  callSessionUrl,
  callWrapUpUrl,
  fetchCallSession,
  normalizeBackendCallSession,
  normalizeApiTimestamp,
  saveWrapUp,
  startOutboundCallUrl,
  toStartOutboundCallRequest,
  toWrapUpRequest
} from "./softphoneApi";

describe("softphoneApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("builds encoded wrap-up URLs", () => {
    expect(callWrapUpUrl("http://localhost:8081/", "call id")).toBe(
      "http://localhost:8081/outbound/sessions/call%20id/wrap-up"
    );
    expect(startOutboundCallUrl("http://localhost:8081/")).toBe(
      "http://localhost:8081/outbound/start"
    );
    expect(callSessionUrl("http://localhost:8081/", "session id")).toBe(
      "http://localhost:8081/outbound/sessions/session%20id"
    );
  });

  it("maps softphone dial attempts into backend outbound requests", () => {
    expect(
      toStartOutboundCallRequest({
        customerNumber: "+15551234567",
        campaignId: "customer-a-campaign",
        agentId: "1001"
      })
    ).toEqual({
      customerNumber: "+15551234567",
      campaignId: "customer-a-campaign",
      agentId: "1001",
      agentChannel: "PJSIP/1001",
      provider: "ASTERISK",
      callMode: "AGENT_ASSISTED"
    });
  });

  it("maps softphone calls into backend wrap-up requests", () => {
    expect(
      toWrapUpRequest({
        agentId: "1001",
        campaignId: "softphone-campaign",
        call: {
          id: "call-1",
          remoteIdentity: "+15551234567",
          direction: "outgoing",
          status: "ended",
          muted: false,
          held: false,
          disposition: "callback",
          notes: "Call tomorrow",
          priority: "high",
          followUpAt: Date.parse("2026-04-20T10:00:00Z")
        }
      })
    ).toEqual({
      campaignId: "softphone-campaign",
      provider: "SOFTPHONE",
      customerNumber: "+15551234567",
      agentId: "1001",
      callMode: "AGENT_SOFTPHONE",
      callDirection: "outgoing",
      callStatus: "ended",
      disposition: "callback",
      notes: "Call tomorrow",
      priority: "high",
      followUpAt: "2026-04-20T10:00:00.000Z"
    });
  });

  it("normalizes backend timestamp shapes for browser session history", () => {
    expect(normalizeApiTimestamp("2026-04-20T10:00:00Z")).toBe("2026-04-20T10:00:00Z");
    expect(normalizeApiTimestamp(1776679200)).toBe("2026-04-20T10:00:00.000Z");
    expect(normalizeApiTimestamp(null)).toBeUndefined();
    expect(normalizeApiTimestamp(undefined)).toBeUndefined();
  });

  it("normalizes backend call session timestamps", () => {
    expect(
      normalizeBackendCallSession({
        callSessionId: "session-1",
        lastEventAt: 1776675900,
        createdAt: 1776675600,
        followUpAt: "2026-04-20T10:00:00Z",
        wrapUpUpdatedAt: null
      })
    ).toEqual({
      callSessionId: "session-1",
      lastEventAt: "2026-04-20T09:05:00.000Z",
      createdAt: "2026-04-20T09:00:00.000Z",
      followUpAt: "2026-04-20T10:00:00Z",
      wrapUpUpdatedAt: undefined
    });
  });

  it("normalizes fetched backend call session timestamps", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          callSessionId: "session-1",
          status: "ENDED",
          lastEventAt: 1776675900,
          createdAt: 1776675600,
          followUpAt: 1776679200,
          wrapUpUpdatedAt: 1776675900
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" }
        }
      )
    );

    await expect(fetchCallSession("http://localhost:8081", "session-1")).resolves.toMatchObject({
      callSessionId: "session-1",
      status: "ENDED",
      lastEventAt: "2026-04-20T09:05:00.000Z",
      createdAt: "2026-04-20T09:00:00.000Z",
      followUpAt: "2026-04-20T10:00:00.000Z",
      wrapUpUpdatedAt: "2026-04-20T09:05:00.000Z"
    });
  });

  it("syncs wrap-up against an explicit backend call session id", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          callSessionId: "backend-session-1",
          campaignId: "softphone-campaign",
          disposition: "connected",
          followUpAt: 1776679200,
          wrapUpUpdatedAt: 1776675900
        }),
        {
        status: 200,
        headers: { "Content-Type": "application/json" }
        }
      )
    );

    await expect(
      saveWrapUp("http://localhost:8081", {
        agentId: "1001",
        callSessionId: "backend-session-1",
        campaignId: "softphone-campaign",
        call: {
          id: "local-softphone-call",
          remoteIdentity: "+15551234567",
          direction: "outgoing",
          status: "ended",
          muted: false,
          held: false,
          disposition: "connected",
          notes: "Reached customer"
        }
      })
    ).resolves.toEqual({
      callSessionId: "backend-session-1",
      campaignId: "softphone-campaign",
      disposition: "connected",
      followUpAt: 1776679200,
      wrapUpUpdatedAt: 1776675900
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8081/outbound/sessions/backend-session-1/wrap-up",
      expect.objectContaining({ method: "PUT" })
    );
    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toMatchObject({
      campaignId: "softphone-campaign",
      customerNumber: "+15551234567",
      agentId: "1001",
      callStatus: "ended",
      disposition: "connected",
      notes: "Reached customer"
    });
  });
});
