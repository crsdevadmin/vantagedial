import type { CallSession } from "./calling-core/types";
import { normalizeApiTimestamp, type BackendCallSession } from "./softphoneApi";

const BACKEND_SESSION_HISTORY_STORAGE_KEY = "vantage-softphone-backend-sessions";

export const MAX_BACKEND_SESSION_HISTORY = 8;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function contactKey(value: string): string {
  return value.replace(/\s+/g, "").toLowerCase();
}

function sessionTimestamp(session: BackendCallSession): number {
  const rawTimestamp = session.wrapUpUpdatedAt ?? session.lastEventAt ?? session.createdAt;
  if (!rawTimestamp) {
    return -1;
  }

  const timestamp = Date.parse(rawTimestamp);
  return Number.isNaN(timestamp) ? -1 : timestamp;
}

export function toBackendCallSession(value: unknown): BackendCallSession | null {
  if (!isRecord(value) || typeof value.callSessionId !== "string") {
    return null;
  }

  return {
    callSessionId: value.callSessionId,
    campaignId: typeof value.campaignId === "string" ? value.campaignId : undefined,
    leadId: typeof value.leadId === "string" ? value.leadId : undefined,
    provider: typeof value.provider === "string" ? value.provider : undefined,
    customerNumber: typeof value.customerNumber === "string" ? value.customerNumber : undefined,
    agentId: typeof value.agentId === "string" ? value.agentId : undefined,
    agentChannel: typeof value.agentChannel === "string" ? value.agentChannel : undefined,
    callMode: typeof value.callMode === "string" ? value.callMode : undefined,
    ivrFlowId: typeof value.ivrFlowId === "string" ? value.ivrFlowId : undefined,
    status: typeof value.status === "string" ? value.status : undefined,
    lastEventType: typeof value.lastEventType === "string" ? value.lastEventType : undefined,
    lastEventAt:
      typeof value.lastEventAt === "string" || typeof value.lastEventAt === "number"
        ? normalizeApiTimestamp(value.lastEventAt)
        : undefined,
    createdAt:
      typeof value.createdAt === "string" || typeof value.createdAt === "number"
        ? normalizeApiTimestamp(value.createdAt)
        : undefined,
    operatorDisposition:
      typeof value.operatorDisposition === "string" ? value.operatorDisposition : undefined,
    operatorNotes: typeof value.operatorNotes === "string" ? value.operatorNotes : undefined,
    operatorPriority:
      typeof value.operatorPriority === "string" ? value.operatorPriority : undefined,
    followUpAt:
      typeof value.followUpAt === "string" || typeof value.followUpAt === "number"
        ? normalizeApiTimestamp(value.followUpAt)
        : undefined,
    wrapUpUpdatedAt:
      typeof value.wrapUpUpdatedAt === "string" || typeof value.wrapUpUpdatedAt === "number"
        ? normalizeApiTimestamp(value.wrapUpUpdatedAt)
        : undefined
  };
}

export function readBackendSessionHistory(): BackendCallSession[] {
  if (typeof window === "undefined") {
    return [];
  }

  try {
    const raw = window.localStorage.getItem(BACKEND_SESSION_HISTORY_STORAGE_KEY);
    if (!raw) {
      return [];
    }

    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) {
      return [];
    }

    return parsed
      .map(toBackendCallSession)
      .filter((session): session is BackendCallSession => session != null)
      .slice(0, MAX_BACKEND_SESSION_HISTORY);
  } catch {
    return [];
  }
}

export function writeBackendSessionHistory(sessions: BackendCallSession[]): void {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(
    BACKEND_SESSION_HISTORY_STORAGE_KEY,
    JSON.stringify(sessions.slice(0, MAX_BACKEND_SESSION_HISTORY))
  );
}

export function upsertBackendSession(
  sessions: BackendCallSession[],
  nextSession: BackendCallSession
): BackendCallSession[] {
  return [
    nextSession,
    ...sessions.filter((session) => session.callSessionId !== nextSession.callSessionId)
  ].slice(0, MAX_BACKEND_SESSION_HISTORY);
}

export function findBackendSessionForCall(
  call: Pick<CallSession, "id" | "remoteIdentity">,
  sessions: Array<BackendCallSession | null | undefined>
): BackendCallSession | null {
  const uniqueSessions = upsertManyBackendSessions(
    sessions.filter((session): session is BackendCallSession => session != null)
  );
  const exactMatch = uniqueSessions.find((session) => session.callSessionId === call.id);
  if (exactMatch) {
    return exactMatch;
  }

  const callNumber = contactKey(call.remoteIdentity);
  const numberMatches = uniqueSessions
    .filter(
      (session) => session.customerNumber != null && contactKey(session.customerNumber) === callNumber
    )
    .sort((left, right) => sessionTimestamp(right) - sessionTimestamp(left));

  return numberMatches[0] ?? null;
}

function upsertManyBackendSessions(sessions: BackendCallSession[]): BackendCallSession[] {
  const seenSessionIds = new Set<string>();
  return sessions.filter((session) => {
    if (seenSessionIds.has(session.callSessionId)) {
      return false;
    }

    seenSessionIds.add(session.callSessionId);
    return true;
  });
}

export function buildBackendSessionBrief(
  session: BackendCallSession,
  fallbackCampaignId: string
): string {
  const lines = [
    `Call session: ${session.callSessionId}`,
    `Customer number: ${session.customerNumber ?? "Unknown"}`,
    `Campaign: ${session.campaignId ?? fallbackCampaignId}`,
    `Status: ${session.status ?? "Unknown"}`,
    `Last event: ${session.lastEventType ?? "None"}`,
    `Provider: ${session.provider ?? "Unknown"}`,
    `Agent: ${session.agentId ?? "Unknown"}`,
    `Agent channel: ${session.agentChannel ?? "Unknown"}`
  ];

  if (session.createdAt) {
    lines.push(`Created: ${session.createdAt}`);
  }

  if (session.lastEventAt) {
    lines.push(`Last event at: ${session.lastEventAt}`);
  }

  if (session.wrapUpUpdatedAt) {
    lines.push(`Wrap-up synced: ${session.wrapUpUpdatedAt}`);
  }

  return lines.join("\n");
}
