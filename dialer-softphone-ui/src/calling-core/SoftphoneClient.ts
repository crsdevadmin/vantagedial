import {
  CallSession,
  ContactFlag,
  ContactHandoffStatus,
  ContactHandoffType,
  SipAdapter,
  SoftphoneActivityItem,
  SoftphoneConfig,
  SoftphoneListener,
  SoftphonePersistedState,
  SoftphoneSnapshot
} from "./types";

const MAX_RECENT_CALLS = 8;
const MAX_ACTIVITY_ITEMS = 14;

function normalizeContactIdentity(value: string): string {
  return value.replace(/\s+/g, "").toLowerCase();
}

function formatContactFlags(flags: ContactFlag[]): string {
  if (flags.length === 0) {
    return "No flags";
  }

  return flags
    .map((flag) => {
      switch (flag) {
        case "vip":
          return "VIP";
        case "verified":
          return "Verified";
        case "do_not_call":
          return "Do Not Call";
        default:
          return flag;
      }
    })
    .join(", ");
}

function formatContactHandoffType(handoffType: ContactHandoffType): string {
  switch (handoffType) {
    case "supervisor":
      return "Supervisor";
    case "verification":
      return "Verification";
    case "compliance":
      return "Compliance";
    default:
      return handoffType;
  }
}

function formatContactHandoffStatus(handoffStatus: ContactHandoffStatus): string {
  switch (handoffStatus) {
    case "open":
      return "Open";
    case "claimed":
      return "Claimed";
    case "resolved":
      return "Resolved";
    default:
      return handoffStatus;
  }
}

function formatContactHandoffDueAt(timestamp: number): string {
  return new Date(timestamp).toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit"
  });
}

function createInitialSnapshot(seed: Partial<SoftphonePersistedState> = {}): SoftphoneSnapshot {
  return {
    registrationState: "offline",
    currentCall: null,
    recentCalls: seed.recentCalls ?? [],
    activity: seed.activity ?? [],
    lastError: null
  };
}

export class SoftphoneClient {
  private snapshot: SoftphoneSnapshot;
  private listeners = new Set<SoftphoneListener>();

  constructor(
    private readonly adapter: SipAdapter,
    initialState: Partial<SoftphonePersistedState> = {}
  ) {
    this.snapshot = createInitialSnapshot(initialState);
  }

  getSnapshot(): SoftphoneSnapshot {
    return this.snapshot;
  }

  subscribe(listener: SoftphoneListener): () => void {
    this.listeners.add(listener);
    listener(this.snapshot);
    return () => this.listeners.delete(listener);
  }

  async connect(config: SoftphoneConfig): Promise<void> {
    try {
      await this.adapter.connect(config, (patch) => this.applyPatch(patch));
    } catch (error) {
      this.applyPatch({
        registrationState: "registration_failed",
        lastError: error instanceof Error ? error.message : "Unknown connection error"
      });
    }
  }

  async disconnect(): Promise<void> {
    await this.adapter.disconnect();
    this.applyPatch({
      registrationState: "offline",
      currentCall: null,
      lastError: null
    });
  }

  async dial(destination: string): Promise<void> {
    await this.adapter.dial(destination);
  }

  async answer(): Promise<void> {
    await this.adapter.answer();
  }

  async reject(): Promise<void> {
    await this.adapter.reject();
  }

  async hangup(): Promise<void> {
    await this.adapter.hangup();
  }

  async mute(nextMuted: boolean): Promise<void> {
    await this.adapter.mute(nextMuted);
  }

  async hold(nextHeld: boolean): Promise<void> {
    await this.adapter.hold(nextHeld);
  }

  saveRecentCallWrapUp(
    callId: string,
    patch: Pick<CallSession, "disposition" | "notes" | "priority" | "followUpAt">
  ): void {
    const target = this.snapshot.recentCalls.find((call) => call.id === callId);
    if (!target) {
      return;
    }

    const nextDisposition = patch.disposition ?? undefined;
    const nextNotes = patch.notes ?? undefined;
    const nextPriority = patch.priority ?? undefined;
    const nextFollowUpAt = patch.followUpAt ?? undefined;
    if (
      (target.disposition ?? undefined) === nextDisposition &&
      (target.notes ?? undefined) === nextNotes &&
      (target.priority ?? undefined) === nextPriority &&
      (target.followUpAt ?? undefined) === nextFollowUpAt
    ) {
      return;
    }

    const recentCalls = this.snapshot.recentCalls.map((call) =>
      call.id === callId
        ? {
            ...call,
            disposition: nextDisposition,
            notes: nextNotes,
            priority: nextPriority,
            followUpAt: nextFollowUpAt
          }
        : call
    );

    this.applyPatch({
      recentCalls,
      activity: [
        this.activity("info", "Wrap-up saved", target.remoteIdentity),
        ...this.snapshot.activity
      ].slice(0, MAX_ACTIVITY_ITEMS)
    });
  }

  saveContactFlags(remoteIdentity: string, contactFlags: ContactFlag[]): void {
    const normalizedIdentity = normalizeContactIdentity(remoteIdentity);
    const targetCalls = this.snapshot.recentCalls.filter(
      (call) => normalizeContactIdentity(call.remoteIdentity) === normalizedIdentity
    );

    if (targetCalls.length === 0) {
      return;
    }

    const nextFlags = [...new Set(contactFlags)];
    const unchanged = targetCalls.every((call) => {
      const currentFlags = [...(call.contactFlags ?? [])].sort();
      const comparableNextFlags = [...nextFlags].sort();
      return (
        currentFlags.length === comparableNextFlags.length &&
        currentFlags.every((flag, index) => flag === comparableNextFlags[index])
      );
    });

    if (unchanged) {
      return;
    }

    const recentCalls = this.snapshot.recentCalls.map((call) =>
      normalizeContactIdentity(call.remoteIdentity) === normalizedIdentity
        ? {
            ...call,
            contactFlags: nextFlags.length > 0 ? nextFlags : undefined
          }
        : call
    );

    this.applyPatch({
      recentCalls,
      activity: [
        this.activity("info", "Contact policy updated", `${remoteIdentity} / ${formatContactFlags(nextFlags)}`),
        ...this.snapshot.activity
      ].slice(0, MAX_ACTIVITY_ITEMS)
    });
  }

  saveContactHandoff(
    remoteIdentity: string,
    patch: Pick<
      CallSession,
      | "handoffType"
      | "handoffStatus"
      | "handoffOwner"
      | "handoffNote"
      | "handoffDueAt"
      | "handoffUpdatedAt"
    >
  ): void {
    const normalizedIdentity = normalizeContactIdentity(remoteIdentity);
    const targetCalls = this.snapshot.recentCalls.filter(
      (call) => normalizeContactIdentity(call.remoteIdentity) === normalizedIdentity
    );

    if (targetCalls.length === 0) {
      return;
    }

    const nextHandoffType = patch.handoffType ?? undefined;
    const nextHandoffStatus = patch.handoffStatus ?? undefined;
    const nextHandoffOwner = patch.handoffOwner ?? undefined;
    const nextHandoffNote = patch.handoffNote ?? undefined;
    const nextHandoffDueAt = patch.handoffDueAt ?? undefined;
    const nextHandoffUpdatedAt = patch.handoffUpdatedAt ?? undefined;

    const unchanged = targetCalls.every(
      (call) =>
        (call.handoffType ?? undefined) === nextHandoffType &&
        (call.handoffStatus ?? undefined) === nextHandoffStatus &&
        (call.handoffOwner ?? undefined) === nextHandoffOwner &&
        (call.handoffNote ?? undefined) === nextHandoffNote &&
        (call.handoffDueAt ?? undefined) === nextHandoffDueAt &&
        (call.handoffUpdatedAt ?? undefined) === nextHandoffUpdatedAt
    );

    if (unchanged) {
      return;
    }

    const recentCalls = this.snapshot.recentCalls.map((call) =>
      normalizeContactIdentity(call.remoteIdentity) === normalizedIdentity
        ? {
            ...call,
            handoffType: nextHandoffType,
            handoffStatus: nextHandoffStatus,
            handoffOwner: nextHandoffOwner,
            handoffNote: nextHandoffNote,
            handoffDueAt: nextHandoffDueAt,
            handoffUpdatedAt: nextHandoffUpdatedAt
          }
        : call
    );

    this.applyPatch({
      recentCalls,
      activity: [
        this.activity(
          nextHandoffType ? "warning" : "info",
          nextHandoffType ? "Contact handoff queued" : "Contact handoff cleared",
          nextHandoffType
            ? `${remoteIdentity} / ${formatContactHandoffType(nextHandoffType)} / ${
                nextHandoffStatus ? formatContactHandoffStatus(nextHandoffStatus) : "Open"
              }${nextHandoffOwner ? ` / ${nextHandoffOwner}` : ""}${
                nextHandoffDueAt ? ` / due ${formatContactHandoffDueAt(nextHandoffDueAt)}` : ""
              }`
            : remoteIdentity
        ),
        ...this.snapshot.activity
      ].slice(0, MAX_ACTIVITY_ITEMS)
    });
  }

  clearActivity(): void {
    if (this.snapshot.activity.length === 0) {
      return;
    }

    this.applyPatch({ activity: [] });
  }

  clearRecentCalls(): void {
    if (this.snapshot.recentCalls.length === 0) {
      return;
    }

    this.applyPatch({
      recentCalls: [],
      activity: [
        this.activity("warning", "Recent history cleared"),
        ...this.snapshot.activity
      ].slice(0, MAX_ACTIVITY_ITEMS)
    });
  }

  private applyPatch(patch: Partial<SoftphoneSnapshot>): void {
    const previous = this.snapshot;
    const next: SoftphoneSnapshot = {
      ...previous,
      ...patch
    };

    next.recentCalls = patch.recentCalls ?? this.nextRecentCalls(previous, next);
    next.activity = patch.activity ?? this.nextActivity(previous, next);
    this.snapshot = next;
    this.emit();
  }

  private nextRecentCalls(previous: SoftphoneSnapshot, next: SoftphoneSnapshot): CallSession[] {
    const terminalCall = this.findTerminalCall(previous, next);
    if (!terminalCall) {
      return previous.recentCalls;
    }

    const alreadyTracked = previous.recentCalls.some(
      (entry) =>
        entry.id === terminalCall.id &&
        entry.status === terminalCall.status &&
        entry.endedAt === terminalCall.endedAt
    );
    if (alreadyTracked) {
      return previous.recentCalls;
    }

    const matchingContact = previous.recentCalls.find(
      (entry) =>
        normalizeContactIdentity(entry.remoteIdentity) ===
        normalizeContactIdentity(terminalCall.remoteIdentity)
    );

    return [
      {
        ...terminalCall,
        contactFlags: terminalCall.contactFlags ?? matchingContact?.contactFlags,
        handoffType: terminalCall.handoffType ?? matchingContact?.handoffType,
        handoffStatus: terminalCall.handoffStatus ?? matchingContact?.handoffStatus,
        handoffOwner: terminalCall.handoffOwner ?? matchingContact?.handoffOwner,
        handoffNote: terminalCall.handoffNote ?? matchingContact?.handoffNote,
        handoffDueAt: terminalCall.handoffDueAt ?? matchingContact?.handoffDueAt,
        handoffUpdatedAt: terminalCall.handoffUpdatedAt ?? matchingContact?.handoffUpdatedAt
      },
      ...previous.recentCalls
    ].slice(0, MAX_RECENT_CALLS);
  }

  private nextActivity(previous: SoftphoneSnapshot, next: SoftphoneSnapshot): SoftphoneActivityItem[] {
    const items: SoftphoneActivityItem[] = [];

    if (previous.registrationState !== next.registrationState) {
      const registrationItem = this.registrationActivity(next.registrationState);
      if (registrationItem) {
        items.push(registrationItem);
      }
    }

    items.push(...this.callActivity(previous.currentCall, next.currentCall));

    if (next.lastError && next.lastError !== previous.lastError) {
      items.push(this.activity("error", "Softphone error", next.lastError));
    }

    return [...items.reverse(), ...previous.activity].slice(0, MAX_ACTIVITY_ITEMS);
  }

  private registrationActivity(state: SoftphoneSnapshot["registrationState"]): SoftphoneActivityItem | null {
    switch (state) {
      case "registering":
        return this.activity("info", "Registering softphone");
      case "registered":
        return this.activity("success", "Softphone registered");
      case "registration_failed":
        return this.activity("error", "Registration failed");
      case "offline":
        return this.activity("warning", "Softphone offline");
      default:
        return null;
    }
  }

  private callActivity(
    previousCall: CallSession | null,
    nextCall: CallSession | null
  ): SoftphoneActivityItem[] {
    const items: SoftphoneActivityItem[] = [];

    if (nextCall && (!previousCall || previousCall.id !== nextCall.id)) {
      items.push(
        this.activity(
          nextCall.direction === "incoming" ? "warning" : "info",
          nextCall.direction === "incoming" ? "Incoming call" : "Dialing call",
          nextCall.remoteIdentity
        )
      );
      return items;
    }

    if (!previousCall || !nextCall || previousCall.id !== nextCall.id) {
      return items;
    }

    if (previousCall.status !== nextCall.status) {
      if (nextCall.status === "in_call") {
        items.push(
          this.activity(
            previousCall.status === "held" ? "info" : "success",
            previousCall.status === "held" ? "Call resumed" : "Call connected",
            nextCall.remoteIdentity
          )
        );
      } else if (nextCall.status === "held") {
        items.push(this.activity("warning", "Call placed on hold", nextCall.remoteIdentity));
      } else if (nextCall.status === "ended") {
        items.push(this.activity("info", "Call ended", nextCall.remoteIdentity));
      } else if (nextCall.status === "failed") {
        items.push(this.activity("error", "Call failed", nextCall.remoteIdentity));
      } else if (nextCall.status === "ringing") {
        items.push(this.activity("warning", "Call ringing", nextCall.remoteIdentity));
      }
    }

    if (previousCall.muted !== nextCall.muted) {
      items.push(
        this.activity(
          nextCall.muted ? "warning" : "info",
          nextCall.muted ? "Microphone muted" : "Microphone unmuted",
          nextCall.remoteIdentity
        )
      );
    }

    return items;
  }

  private findTerminalCall(
    previous: SoftphoneSnapshot,
    next: SoftphoneSnapshot
  ): CallSession | null {
    if (next.currentCall && this.isTerminal(next.currentCall.status)) {
      return next.currentCall;
    }

    if (previous.currentCall && !next.currentCall && this.isTerminal(previous.currentCall.status)) {
      return previous.currentCall;
    }

    return null;
  }

  private isTerminal(status: CallSession["status"]): boolean {
    return status === "ended" || status === "failed";
  }

  private activity(
    level: SoftphoneActivityItem["level"],
    title: string,
    detail?: string
  ): SoftphoneActivityItem {
    return {
      id: globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`,
      timestamp: Date.now(),
      level,
      title,
      detail
    };
  }

  private emit(): void {
    for (const listener of this.listeners) {
      listener(this.snapshot);
    }
  }
}
