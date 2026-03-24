import { useEffect, useMemo, useState } from "react";
import { JsSipAdapter } from "../calling-core/JsSipAdapter";
import { MockSipAdapter } from "../calling-core/MockSipAdapter";
import { SoftphoneClient } from "../calling-core/SoftphoneClient";
import {
  CallPriority,
  CallDisposition,
  CallSession,
  ContactFlag,
  ContactHandoffStatus,
  ContactHandoffType,
  SoftphoneActivityItem,
  SoftphonePersistedState,
  SoftphoneSnapshot
} from "../calling-core/types";

const SOFTPHONE_STATE_STORAGE_KEY = "vantage-softphone-runtime";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function toDisposition(value: unknown): CallDisposition | undefined {
  switch (value) {
    case "connected":
    case "callback":
    case "voicemail":
    case "no_answer":
    case "wrong_number":
      return value;
    default:
      return undefined;
  }
}

function toPriority(value: unknown): CallPriority | undefined {
  switch (value) {
    case "normal":
    case "high":
      return value;
    default:
      return undefined;
  }
}

function toContactFlag(value: unknown): ContactFlag | undefined {
  switch (value) {
    case "vip":
    case "verified":
    case "do_not_call":
      return value;
    default:
      return undefined;
  }
}

function toHandoffType(value: unknown): ContactHandoffType | undefined {
  switch (value) {
    case "supervisor":
    case "verification":
    case "compliance":
      return value;
    default:
      return undefined;
  }
}

function toHandoffStatus(value: unknown): ContactHandoffStatus | undefined {
  switch (value) {
    case "open":
    case "claimed":
    case "resolved":
      return value;
    default:
      return undefined;
  }
}

function toCallSession(value: unknown): CallSession | null {
  if (!isRecord(value)) {
    return null;
  }

  if (
    typeof value.id !== "string" ||
    typeof value.remoteIdentity !== "string" ||
    (value.direction !== "incoming" && value.direction !== "outgoing") ||
    !["idle", "dialing", "ringing", "in_call", "held", "ended", "failed"].includes(
      String(value.status)
    ) ||
    typeof value.muted !== "boolean" ||
    typeof value.held !== "boolean"
  ) {
    return null;
  }

  return {
    id: value.id,
    remoteIdentity: value.remoteIdentity,
    direction: value.direction,
    status: value.status as CallSession["status"],
    startedAt: typeof value.startedAt === "number" ? value.startedAt : undefined,
    endedAt: typeof value.endedAt === "number" ? value.endedAt : undefined,
    muted: value.muted,
    held: value.held,
    disposition: toDisposition(value.disposition),
    notes: typeof value.notes === "string" ? value.notes : undefined,
    priority: toPriority(value.priority),
    followUpAt: typeof value.followUpAt === "number" ? value.followUpAt : undefined,
    contactFlags: Array.isArray(value.contactFlags)
      ? [...new Set(value.contactFlags.map(toContactFlag).filter((flag): flag is ContactFlag => flag != null))]
      : undefined,
    handoffType: toHandoffType(value.handoffType),
    handoffStatus: toHandoffStatus(value.handoffStatus),
    handoffOwner: typeof value.handoffOwner === "string" ? value.handoffOwner : undefined,
    handoffNote: typeof value.handoffNote === "string" ? value.handoffNote : undefined,
    handoffDueAt: typeof value.handoffDueAt === "number" ? value.handoffDueAt : undefined,
    handoffUpdatedAt:
      typeof value.handoffUpdatedAt === "number" ? value.handoffUpdatedAt : undefined
  };
}

function toActivityItem(value: unknown): SoftphoneActivityItem | null {
  if (!isRecord(value)) {
    return null;
  }

  if (
    typeof value.id !== "string" ||
    typeof value.timestamp !== "number" ||
    !["info", "success", "warning", "error"].includes(String(value.level)) ||
    typeof value.title !== "string"
  ) {
    return null;
  }

  return {
    id: value.id,
    timestamp: value.timestamp,
    level: value.level as SoftphoneActivityItem["level"],
    title: value.title,
    detail: typeof value.detail === "string" ? value.detail : undefined
  };
}

function readPersistedState(): SoftphonePersistedState {
  if (typeof window === "undefined") {
    return { recentCalls: [], activity: [] };
  }

  try {
    const raw = window.localStorage.getItem(SOFTPHONE_STATE_STORAGE_KEY);
    if (!raw) {
      return { recentCalls: [], activity: [] };
    }

    const parsed = JSON.parse(raw) as {
      recentCalls?: unknown[];
      activity?: unknown[];
    };

    return {
      recentCalls: Array.isArray(parsed.recentCalls)
        ? parsed.recentCalls.map(toCallSession).filter((value): value is CallSession => value != null)
        : [],
      activity: Array.isArray(parsed.activity)
        ? parsed.activity
            .map(toActivityItem)
            .filter((value): value is SoftphoneActivityItem => value != null)
        : []
    };
  } catch {
    return { recentCalls: [], activity: [] };
  }
}

export function useSoftphone() {
  const client = useMemo(() => {
    const mode = import.meta.env.VITE_SOFTPHONE_MODE ?? "mock";
    return new SoftphoneClient(
      mode === "jssip" ? new JsSipAdapter() : new MockSipAdapter(),
      readPersistedState()
    );
  }, []);
  const [snapshot, setSnapshot] = useState<SoftphoneSnapshot>(client.getSnapshot());

  useEffect(() => client.subscribe(setSnapshot), [client]);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    window.localStorage.setItem(
      SOFTPHONE_STATE_STORAGE_KEY,
      JSON.stringify({
        recentCalls: snapshot.recentCalls,
        activity: snapshot.activity
      } satisfies SoftphonePersistedState)
    );
  }, [snapshot.activity, snapshot.recentCalls]);

  return { client, snapshot };
}
