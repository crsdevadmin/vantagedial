import { CallSession } from "./calling-core/types";
import { normalizeApiBaseUrl } from "./customerConfig";

export type ApiTimestamp = string | number | null;

export type SaveWrapUpPayload = {
  agentId: string;
  call: CallSession;
  callSessionId?: string;
  campaignId?: string;
};

export type StartOutboundCallPayload = {
  customerNumber: string;
  campaignId?: string;
  agentId: string;
  agentChannel?: string;
  provider?: string;
};

export type StartOutboundCallResponse = {
  status: string;
  callSessionId: string;
  provider?: string;
  campaignId?: string;
  customerNumber?: string;
  agentId?: string;
  agentChannel?: string;
  callMode?: string;
};

export type BackendCallSession = {
  callSessionId: string;
  campaignId?: string;
  leadId?: string;
  provider?: string;
  customerNumber?: string;
  agentId?: string;
  agentChannel?: string;
  callMode?: string;
  ivrFlowId?: string;
  status?: string;
  lastEventType?: string;
  lastEventAt?: string;
  createdAt?: string;
  operatorDisposition?: string;
  operatorNotes?: string;
  operatorPriority?: string;
  followUpAt?: string;
  wrapUpUpdatedAt?: string;
};

type RawBackendCallSession = Omit<
  BackendCallSession,
  "lastEventAt" | "createdAt" | "followUpAt" | "wrapUpUpdatedAt"
> & {
  lastEventAt?: ApiTimestamp;
  createdAt?: ApiTimestamp;
  followUpAt?: ApiTimestamp;
  wrapUpUpdatedAt?: ApiTimestamp;
};

export type OperatorWrapUpResponse = {
  callSessionId: string;
  campaignId?: string | null;
  customerNumber?: string | null;
  agentId?: string | null;
  disposition?: string | null;
  notes?: string | null;
  priority?: string | null;
  followUpAt?: ApiTimestamp;
  wrapUpUpdatedAt?: ApiTimestamp;
};

export function startOutboundCallUrl(apiBaseUrl: string): string {
  return `${normalizeApiBaseUrl(apiBaseUrl)}/outbound/start`;
}

export function callSessionUrl(apiBaseUrl: string, callSessionId: string): string {
  return `${normalizeApiBaseUrl(apiBaseUrl)}/outbound/sessions/${encodeURIComponent(
    callSessionId
  )}`;
}

export function callWrapUpUrl(apiBaseUrl: string, callSessionId: string): string {
  return `${normalizeApiBaseUrl(apiBaseUrl)}/outbound/sessions/${encodeURIComponent(
    callSessionId
  )}/wrap-up`;
}

export function toStartOutboundCallRequest({
  customerNumber,
  campaignId,
  agentId,
  agentChannel,
  provider
}: StartOutboundCallPayload) {
  return {
    customerNumber,
    campaignId: campaignId || "softphone-console",
    agentId,
    agentChannel: agentChannel || `PJSIP/${agentId}`,
    provider: provider || "ASTERISK",
    callMode: "AGENT_ASSISTED"
  };
}

export function toWrapUpRequest({ agentId, call, campaignId }: SaveWrapUpPayload) {
  return {
    campaignId,
    provider: "SOFTPHONE",
    customerNumber: call.remoteIdentity,
    agentId,
    callMode: "AGENT_SOFTPHONE",
    callDirection: call.direction,
    callStatus: call.status,
    disposition: call.disposition,
    notes: call.notes,
    priority: call.priority,
    followUpAt: call.followUpAt ? new Date(call.followUpAt).toISOString() : undefined
  };
}

export function normalizeApiTimestamp(value: ApiTimestamp | undefined): string | undefined {
  if (typeof value === "string") {
    return value || undefined;
  }

  if (typeof value === "number" && Number.isFinite(value)) {
    return new Date(value * 1000).toISOString();
  }

  return undefined;
}

export function normalizeBackendCallSession(session: RawBackendCallSession): BackendCallSession {
  return {
    ...session,
    lastEventAt: normalizeApiTimestamp(session.lastEventAt),
    createdAt: normalizeApiTimestamp(session.createdAt),
    followUpAt: normalizeApiTimestamp(session.followUpAt),
    wrapUpUpdatedAt: normalizeApiTimestamp(session.wrapUpUpdatedAt)
  };
}

export async function startOutboundCall(
  apiBaseUrl: string,
  payload: StartOutboundCallPayload
): Promise<StartOutboundCallResponse> {
  const response = await fetch(startOutboundCallUrl(apiBaseUrl), {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(toStartOutboundCallRequest(payload))
  });

  if (!response.ok) {
    throw new Error(`Outbound queue failed with ${response.status}`);
  }

  return (await response.json()) as StartOutboundCallResponse;
}

export async function fetchCallSession(
  apiBaseUrl: string,
  callSessionId: string
): Promise<BackendCallSession> {
  const response = await fetch(callSessionUrl(apiBaseUrl, callSessionId));

  if (!response.ok) {
    throw new Error(`Call session refresh failed with ${response.status}`);
  }

  return normalizeBackendCallSession((await response.json()) as RawBackendCallSession);
}

export async function saveWrapUp(
  apiBaseUrl: string,
  payload: SaveWrapUpPayload
): Promise<OperatorWrapUpResponse> {
  const response = await fetch(callWrapUpUrl(apiBaseUrl, payload.callSessionId ?? payload.call.id), {
    method: "PUT",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(toWrapUpRequest(payload))
  });

  if (!response.ok) {
    throw new Error(`Wrap-up sync failed with ${response.status}`);
  }

  return (await response.json()) as OperatorWrapUpResponse;
}
