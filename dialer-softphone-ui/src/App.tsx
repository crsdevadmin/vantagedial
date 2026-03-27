import { FormEvent, useEffect, useState } from "react";
import {
  CallDisposition,
  CallPriority,
  CallSession,
  ContactFlag,
  ContactHandoffStatus,
  ContactHandoffType,
  SoftphoneActivityItem
} from "./calling-core/types";
import { useSoftphone } from "./hooks/useSoftphone";

const SETTINGS_STORAGE_KEY = "vantage-softphone-settings";

type StoredSettings = {
  server: string;
  sipServer: string;
  username: string;
};

type ReviewScope =
  | "all"
  | "follow_up"
  | "overdue"
  | "scheduled_today"
  | "pending_wrap_up"
  | "failed";

type FollowUpBucket = "overdue" | "today" | "upcoming" | "unscheduled";

type HandoffScope =
  | "all"
  | "mine"
  | "open"
  | "claimed"
  | "unassigned"
  | "stale"
  | "due_today"
  | "overdue";

type ContactHandoffSummary = {
  key: string;
  remoteIdentity: string;
  handoffType: ContactHandoffType;
  handoffStatus?: ContactHandoffStatus;
  handoffOwner?: string;
  handoffNote?: string;
  handoffDueAt?: number;
  handoffUpdatedAt?: number;
  latestCall: CallSession;
  contactFlags: ContactFlag[];
  attemptCount: number;
  connectedCount: number;
  failedCount: number;
  nextFollowUpAt?: number;
  recentNotes: string[];
};

type BriefFeedback = {
  key: string;
  tone: "success" | "error";
  message: string;
};

type RecommendationAction = "call_now" | "schedule_1h" | "schedule_tomorrow" | "focus_wrap_up";

type ContactRecommendation = {
  tone: "info" | "success" | "warning" | "error";
  title: string;
  detail: string;
  primaryAction: RecommendationAction;
  primaryLabel: string;
  secondaryAction?: RecommendationAction;
  secondaryLabel?: string;
};

const defaultSettings: StoredSettings = {
  server: "wss://asterisk.example.com/ws",
  sipServer: "asterisk.example.com",
  username: "1001"
};

const dispositionOptions: Array<{ value: CallDisposition; label: string }> = [
  { value: "connected", label: "Connected" },
  { value: "callback", label: "Callback" },
  { value: "voicemail", label: "Voicemail" },
  { value: "no_answer", label: "No Answer" },
  { value: "wrong_number", label: "Wrong Number" }
];

const priorityOptions: Array<{ value: CallPriority; label: string }> = [
  { value: "normal", label: "Normal" },
  { value: "high", label: "High" }
];

const contactFlagOptions: Array<{ value: ContactFlag; label: string; detail: string }> = [
  { value: "vip", label: "VIP", detail: "Keep this contact visible and prioritized." },
  { value: "verified", label: "Verified", detail: "Number has been validated recently." },
  {
    value: "do_not_call",
    label: "Do Not Call",
    detail: "Dial actions should stay blocked until policy changes."
  }
];

const handoffTypeOptions: Array<{
  value: ContactHandoffType;
  label: string;
  detail: string;
}> = [
  {
    value: "supervisor",
    label: "Supervisor",
    detail: "Escalate this contact for supervisor review or intervention."
  },
  {
    value: "verification",
    label: "Verification",
    detail: "Number or identity needs another verification pass."
  },
  {
    value: "compliance",
    label: "Compliance",
    detail: "This contact needs compliance or policy confirmation."
  }
];

const handoffStatusOptions: Array<{
  value: ContactHandoffStatus;
  label: string;
}> = [
  { value: "open", label: "Open" },
  { value: "claimed", label: "Claimed" },
  { value: "resolved", label: "Resolved" }
];

const reviewScopeOptions: Array<{ value: ReviewScope; label: string }> = [
  { value: "all", label: "All calls" },
  { value: "follow_up", label: "Follow-up" },
  { value: "overdue", label: "Overdue" },
  { value: "scheduled_today", label: "Due today" },
  { value: "pending_wrap_up", label: "Pending wrap-up" },
  { value: "failed", label: "Failed" }
];

const handoffScopeOptions: Array<{ value: HandoffScope; label: string }> = [
  { value: "all", label: "All handoffs" },
  { value: "mine", label: "Mine" },
  { value: "open", label: "Open" },
  { value: "claimed", label: "Claimed" },
  { value: "unassigned", label: "Unassigned" },
  { value: "stale", label: "Stale" },
  { value: "due_today", label: "Due today" },
  { value: "overdue", label: "Overdue" }
];

const followUpBucketOrder: FollowUpBucket[] = [
  "overdue",
  "today",
  "upcoming",
  "unscheduled"
];

const followUpBucketCopy: Record<
  FollowUpBucket,
  { label: string; detail: string }
> = {
  overdue: {
    label: "Overdue",
    detail: "Callbacks whose due time has already passed."
  },
  today: {
    label: "Due today",
    detail: "Scheduled callbacks that should be worked before the day ends."
  },
  upcoming: {
    label: "Upcoming",
    detail: "Future follow-ups that are already slotted."
  },
  unscheduled: {
    label: "Unscheduled",
    detail: "Retry outcomes that still need a firm callback slot."
  }
};

function readStoredSettings(): StoredSettings {
  if (typeof window === "undefined") {
    return defaultSettings;
  }

  try {
    const raw = window.localStorage.getItem(SETTINGS_STORAGE_KEY);
    if (!raw) {
      return defaultSettings;
    }

    const parsed = JSON.parse(raw) as Partial<StoredSettings>;
    return {
      server: parsed.server || defaultSettings.server,
      sipServer: parsed.sipServer || defaultSettings.sipServer,
      username: parsed.username || defaultSettings.username
    };
  } catch {
    return defaultSettings;
  }
}

function formatLiveDuration(startedAt?: number): string {
  if (!startedAt) {
    return "00:00";
  }

  const elapsedSeconds = Math.max(0, Math.floor((Date.now() - startedAt) / 1000));
  const minutes = Math.floor(elapsedSeconds / 60)
    .toString()
    .padStart(2, "0");
  const seconds = (elapsedSeconds % 60).toString().padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function formatCompletedDuration(call: CallSession): string {
  if (!call.startedAt || !call.endedAt) {
    return "--";
  }

  const elapsedSeconds = Math.max(0, Math.floor((call.endedAt - call.startedAt) / 1000));
  const minutes = Math.floor(elapsedSeconds / 60)
    .toString()
    .padStart(2, "0");
  const seconds = (elapsedSeconds % 60).toString().padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function formatTimestamp(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit"
  });
}

function formatDateTime(timestamp: number): string {
  return new Date(timestamp).toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function formatLabel(value: string): string {
  return value
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

function formatCallResult(status: CallSession["status"]): string {
  return formatLabel(status);
}

function formatDisposition(disposition: CallDisposition): string {
  return formatLabel(disposition);
}

function formatContactFlag(flag: ContactFlag): string {
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
}

function formatHandoffType(handoffType: ContactHandoffType): string {
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

function formatHandoffStatus(handoffStatus: ContactHandoffStatus): string {
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

function formatHandoffDueTiming(timestamp: number | undefined, referenceTime: number): string {
  if (!timestamp) {
    return "No SLA set";
  }

  if (timestamp < referenceTime) {
    return `Overdue ${formatDateTime(timestamp)}`;
  }

  if (isSameLocalDay(timestamp, referenceTime)) {
    return `Today ${formatTimestamp(timestamp)}`;
  }

  return formatDateTime(timestamp);
}

function normalizeDestination(value: string): string {
  return value.replace(/\s+/g, "");
}

function activityToneLabel(item: SoftphoneActivityItem): string {
  return item.level.toUpperCase();
}

function contactKey(value: string): string {
  return normalizeDestination(value).toLowerCase();
}

function orderContactFlags(flags: ContactFlag[]): ContactFlag[] {
  const flagRank: Record<ContactFlag, number> = {
    vip: 0,
    verified: 1,
    do_not_call: 2
  };

  return [...new Set(flags)].sort((left, right) => flagRank[left] - flagRank[right]);
}

function hasContactFlag(
  call: Pick<CallSession, "contactFlags"> | null | undefined,
  flag: ContactFlag
): boolean {
  return call?.contactFlags?.includes(flag) ?? false;
}

function isCallableContact(call: Pick<CallSession, "contactFlags">): boolean {
  return !hasContactFlag(call, "do_not_call");
}

function hasTrackedHandoff(
  call: Pick<CallSession, "handoffType" | "handoffNote"> | null | undefined
): boolean {
  return Boolean(call?.handoffType || call?.handoffNote);
}

function hasActiveHandoff(
  call: Pick<CallSession, "handoffType" | "handoffNote" | "handoffStatus"> | null | undefined
): boolean {
  return hasTrackedHandoff(call) && call?.handoffStatus !== "resolved";
}

async function copyTextToClipboard(value: string): Promise<void> {
  if (typeof window === "undefined") {
    throw new Error("Clipboard is not available");
  }

  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(value);
    return;
  }

  const textarea = document.createElement("textarea");
  textarea.value = value;
  textarea.setAttribute("readonly", "true");
  textarea.style.position = "absolute";
  textarea.style.left = "-9999px";
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand("copy");
  document.body.removeChild(textarea);
}

function buildHandoffBrief(
  options: {
    remoteIdentity: string;
    handoffType?: ContactHandoffType;
    handoffStatus?: ContactHandoffStatus;
    handoffOwner?: string;
    handoffNote?: string;
    handoffDueAt?: number;
    handoffUpdatedAt?: number;
    contactFlags: ContactFlag[];
    attemptCount: number;
    connectedCount: number;
    failedCount: number;
    nextFollowUpAt?: number;
    latestCall: CallSession | null;
    recentNotes: string[];
  },
  referenceTime: number
): string {
  const lines = [
    `Contact: ${options.remoteIdentity}`,
    `Handoff: ${
      options.handoffType ? formatHandoffType(options.handoffType) : "Not set"
    }`,
    `Handoff status: ${
      options.handoffStatus ? formatHandoffStatus(options.handoffStatus) : "Open"
    }`,
    `Handoff owner: ${options.handoffOwner || "Unassigned"}`,
    `Handoff due: ${formatHandoffDueTiming(options.handoffDueAt, referenceTime)}`,
    `Flags: ${
      options.contactFlags.length > 0
        ? options.contactFlags.map(formatContactFlag).join(", ")
        : "None"
    }`,
    `Attempts: ${options.attemptCount}`,
    `Connected: ${options.connectedCount}`,
    `Failed: ${options.failedCount}`,
    `Latest outcome: ${
      options.latestCall?.disposition
        ? formatDisposition(options.latestCall.disposition)
        : options.latestCall
          ? formatCallResult(options.latestCall.status)
          : "Unknown"
    }`,
    `Latest attempt: ${
      options.latestCall
        ? formatDateTime(
            options.latestCall.endedAt ?? options.latestCall.startedAt ?? referenceTime
          )
        : "Unknown"
    }`,
    `Next follow-up: ${formatFollowUpTiming(options.nextFollowUpAt, referenceTime)}`
  ];

  if (options.handoffUpdatedAt) {
    lines.push(`Handoff updated: ${formatDateTime(options.handoffUpdatedAt)}`);
  }

  if (options.handoffNote) {
    lines.push(`Handoff note: ${options.handoffNote}`);
  }

  if (options.recentNotes.length > 0) {
    lines.push("Recent notes:");
    options.recentNotes.forEach((note) => {
      lines.push(`- ${note}`);
    });
  }

  return lines.join("\n");
}

function handoffTimestamp(handoff: ContactHandoffSummary, referenceTime: number): number {
  return (
    handoff.handoffUpdatedAt ??
    handoff.latestCall.endedAt ??
    handoff.latestCall.startedAt ??
    referenceTime
  );
}

function isOverdueHandoff(handoff: ContactHandoffSummary, referenceTime: number): boolean {
  return typeof handoff.handoffDueAt === "number" && handoff.handoffDueAt < referenceTime;
}

function isHandoffDueToday(handoff: ContactHandoffSummary, referenceTime: number): boolean {
  return (
    typeof handoff.handoffDueAt === "number" &&
    handoff.handoffDueAt >= referenceTime &&
    isSameLocalDay(handoff.handoffDueAt, referenceTime)
  );
}

function isStaleHandoff(handoff: ContactHandoffSummary, referenceTime: number): boolean {
  return referenceTime - handoffTimestamp(handoff, referenceTime) >= 2 * 60 * 60 * 1000;
}

function handoffAgeLabel(handoff: ContactHandoffSummary, referenceTime: number): string {
  const elapsedMinutes = Math.max(
    0,
    Math.floor((referenceTime - handoffTimestamp(handoff, referenceTime)) / 60_000)
  );

  if (elapsedMinutes < 60) {
    return `${elapsedMinutes}m old`;
  }

  const hours = Math.floor(elapsedMinutes / 60);
  const minutes = elapsedMinutes % 60;
  return minutes === 0 ? `${hours}h old` : `${hours}h ${minutes}m old`;
}

function formatHandoffDeskHint(handoff: ContactHandoffSummary, referenceTime: number): string {
  if (typeof handoff.handoffDueAt === "number") {
    return formatHandoffDueTiming(handoff.handoffDueAt, referenceTime);
  }

  if (!handoff.handoffOwner) {
    return `Unassigned / ${handoffAgeLabel(handoff, referenceTime)}`;
  }

  if (isStaleHandoff(handoff, referenceTime)) {
    return `Stale / ${handoffAgeLabel(handoff, referenceTime)}`;
  }

  return `Updated ${handoffAgeLabel(handoff, referenceTime)}`;
}

function isResolvedTodayHandoff(handoff: ContactHandoffSummary, referenceTime: number): boolean {
  return isSameLocalDay(
    handoff.handoffUpdatedAt ?? handoffTimestamp(handoff, referenceTime),
    referenceTime
  );
}

function compareHandoffs(
  left: ContactHandoffSummary,
  right: ContactHandoffSummary,
  referenceTime: number
): number {
  const leftOverdue = isOverdueHandoff(left, referenceTime) ? 0 : 1;
  const rightOverdue = isOverdueHandoff(right, referenceTime) ? 0 : 1;
  if (leftOverdue !== rightOverdue) {
    return leftOverdue - rightOverdue;
  }

  const leftDueToday = isHandoffDueToday(left, referenceTime) ? 0 : 1;
  const rightDueToday = isHandoffDueToday(right, referenceTime) ? 0 : 1;
  if (leftDueToday !== rightDueToday) {
    return leftDueToday - rightDueToday;
  }

  const leftStale = isStaleHandoff(left, referenceTime) ? 0 : 1;
  const rightStale = isStaleHandoff(right, referenceTime) ? 0 : 1;
  if (leftStale !== rightStale) {
    return leftStale - rightStale;
  }

  const leftUnassigned = left.handoffOwner ? 1 : 0;
  const rightUnassigned = right.handoffOwner ? 1 : 0;
  if (leftUnassigned !== rightUnassigned) {
    return leftUnassigned - rightUnassigned;
  }

  return handoffTimestamp(right, referenceTime) - handoffTimestamp(left, referenceTime);
}

function buildHandoffSummaries(
  calls: CallSession[],
  predicate: (call: CallSession) => boolean,
  referenceTime: number
): ContactHandoffSummary[] {
  const summariesByContact = calls.reduce<Record<string, ContactHandoffSummary>>(
    (accumulator, call) => {
      if (!predicate(call)) {
        return accumulator;
      }

      const key = contactKey(call.remoteIdentity);
      const existing = accumulator[key];
      const callTimestamp = call.handoffUpdatedAt ?? call.endedAt ?? call.startedAt ?? 0;
      const existingTimestamp =
        existing?.handoffUpdatedAt ??
        existing?.latestCall.endedAt ??
        existing?.latestCall.startedAt ??
        0;

      if (existing && existingTimestamp >= callTimestamp) {
        return accumulator;
      }

      const contactCalls = calls.filter((entry) => contactKey(entry.remoteIdentity) === key);

      accumulator[key] = {
        key,
        remoteIdentity: call.remoteIdentity,
        handoffType: call.handoffType ?? "supervisor",
        handoffStatus: call.handoffStatus ?? "open",
        handoffOwner: call.handoffOwner,
        handoffNote: call.handoffNote,
        handoffDueAt: call.handoffDueAt,
        handoffUpdatedAt: call.handoffUpdatedAt,
        latestCall: call,
        contactFlags: orderContactFlags(contactCalls.flatMap((entry) => entry.contactFlags ?? [])),
        attemptCount: contactCalls.length,
        connectedCount: contactCalls.filter((entry) => entry.disposition === "connected").length,
        failedCount: contactCalls.filter((entry) => entry.status === "failed").length,
        nextFollowUpAt:
          [...contactCalls.filter(needsFollowUp)].sort((left, right) =>
            compareFollowUpCalls(left, right, referenceTime)
          )[0]?.followUpAt,
        recentNotes: contactCalls
          .map((entry) => entry.notes?.trim())
          .filter((note): note is string => Boolean(note))
          .slice(0, 3)
      };
      return accumulator;
    },
    {}
  );

  return Object.values(summariesByContact);
}

function matchesHandoffSearch(handoff: ContactHandoffSummary, query: string): boolean {
  if (!query) {
    return true;
  }

  const haystack = [
    handoff.remoteIdentity,
    handoff.handoffType,
    handoff.handoffStatus ?? "open",
    handoff.handoffOwner ?? "",
    handoff.handoffNote ?? "",
    ...handoff.contactFlags
  ]
    .join(" ")
    .toLowerCase();

  return haystack.includes(query);
}

function matchesHandoffScope(
  handoff: ContactHandoffSummary,
  scope: HandoffScope,
  currentUsername: string,
  referenceTime: number
): boolean {
  const normalizedUsername = currentUsername.trim().toLowerCase();
  const normalizedOwner = handoff.handoffOwner?.trim().toLowerCase() ?? "";

  switch (scope) {
    case "mine":
      return Boolean(normalizedUsername) && normalizedOwner === normalizedUsername;
    case "open":
      return (handoff.handoffStatus ?? "open") === "open";
    case "claimed":
      return (handoff.handoffStatus ?? "open") === "claimed";
    case "unassigned":
      return !handoff.handoffOwner;
    case "stale":
      return isStaleHandoff(handoff, referenceTime);
    case "due_today":
      return isHandoffDueToday(handoff, referenceTime);
    case "overdue":
      return isOverdueHandoff(handoff, referenceTime);
    case "all":
    default:
      return true;
  }
}

function isFollowUpDisposition(disposition: CallDisposition | "" | undefined): boolean {
  return (
    disposition === "callback" ||
    disposition === "voicemail" ||
    disposition === "no_answer"
  );
}

function needsFollowUp(call: CallSession): boolean {
  return isFollowUpDisposition(call.disposition);
}

function isSameLocalDay(timestamp: number, referenceTime: number): boolean {
  const valueDate = new Date(timestamp);
  const referenceDate = new Date(referenceTime);
  return (
    valueDate.getFullYear() === referenceDate.getFullYear() &&
    valueDate.getMonth() === referenceDate.getMonth() &&
    valueDate.getDate() === referenceDate.getDate()
  );
}

function isOverdueFollowUp(call: CallSession, referenceTime: number): boolean {
  return needsFollowUp(call) && typeof call.followUpAt === "number" && call.followUpAt < referenceTime;
}

function isScheduledToday(call: CallSession, referenceTime: number): boolean {
  return (
    needsFollowUp(call) &&
    typeof call.followUpAt === "number" &&
    isSameLocalDay(call.followUpAt, referenceTime)
  );
}

function compareFollowUpCalls(
  left: CallSession,
  right: CallSession,
  referenceTime: number
): number {
  const bucket = (call: CallSession): number => {
    if (isOverdueFollowUp(call, referenceTime)) {
      return 0;
    }
    if (isScheduledToday(call, referenceTime)) {
      return 1;
    }
    if (typeof call.followUpAt === "number") {
      return 2;
    }
    return 3;
  };

  const bucketDifference = bucket(left) - bucket(right);
  if (bucketDifference !== 0) {
    return bucketDifference;
  }

  const priorityDifference =
    (left.priority === "high" ? 0 : 1) - (right.priority === "high" ? 0 : 1);
  if (priorityDifference !== 0) {
    return priorityDifference;
  }

  const dueDifference =
    (left.followUpAt ?? Number.MAX_SAFE_INTEGER) -
    (right.followUpAt ?? Number.MAX_SAFE_INTEGER);
  if (dueDifference !== 0) {
    return dueDifference;
  }

  return (right.endedAt ?? right.startedAt ?? 0) - (left.endedAt ?? left.startedAt ?? 0);
}

function formatDateTimeLocalValue(timestamp?: number): string {
  if (!timestamp) {
    return "";
  }

  const date = new Date(timestamp);
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60_000);
  return local.toISOString().slice(0, 16);
}

function parseDateTimeLocalValue(value: string): number | undefined {
  if (!value) {
    return undefined;
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return undefined;
  }

  return parsed.getTime();
}

function formatFollowUpTiming(timestamp: number | undefined, referenceTime: number): string {
  if (!timestamp) {
    return "No time set";
  }

  if (timestamp < referenceTime) {
    return `Overdue ${formatDateTime(timestamp)}`;
  }

  if (isSameLocalDay(timestamp, referenceTime)) {
    return `Today ${formatTimestamp(timestamp)}`;
  }

  return formatDateTime(timestamp);
}

function scheduleInMinutes(referenceTime: number, minutes: number): number {
  return referenceTime + minutes * 60_000;
}

function tomorrowAt(referenceTime: number, hours: number, minutes: number): number {
  const next = new Date(referenceTime);
  next.setDate(next.getDate() + 1);
  next.setHours(hours, minutes, 0, 0);
  return next.getTime();
}

function followUpState(
  call: CallSession,
  referenceTime: number
): FollowUpBucket {
  if (isOverdueFollowUp(call, referenceTime)) {
    return "overdue";
  }
  if (isScheduledToday(call, referenceTime)) {
    return "today";
  }
  if (typeof call.followUpAt === "number") {
    return "upcoming";
  }
  return "unscheduled";
}

function followUpStateLabel(
  state: FollowUpBucket
): string {
  switch (state) {
    case "overdue":
      return "Overdue";
    case "today":
      return "Due today";
    case "upcoming":
      return "Upcoming";
    case "unscheduled":
    default:
      return "Unscheduled";
  }
}

function createEmptyFollowUpGroups(): Record<FollowUpBucket, CallSession[]> {
  return {
    overdue: [],
    today: [],
    upcoming: [],
    unscheduled: []
  };
}

function groupFollowUpCalls(
  calls: CallSession[],
  referenceTime: number
): Record<FollowUpBucket, CallSession[]> {
  return calls.reduce((groups, call) => {
    groups[followUpState(call, referenceTime)].push(call);
    return groups;
  }, createEmptyFollowUpGroups());
}

function createInitialCollapsedBuckets(): Record<FollowUpBucket, boolean> {
  return {
    overdue: false,
    today: false,
    upcoming: false,
    unscheduled: false
  };
}

function matchesSearch(call: CallSession, query: string): boolean {
  if (!query) {
    return true;
  }

  const haystack = [
    call.remoteIdentity,
    call.direction,
    call.status,
    call.disposition ?? "",
    call.notes ?? ""
  ]
    .join(" ")
    .toLowerCase();

  return haystack.includes(query);
}

function matchesScope(call: CallSession, scope: ReviewScope, referenceTime: number): boolean {
  switch (scope) {
    case "follow_up":
      return needsFollowUp(call);
    case "overdue":
      return isOverdueFollowUp(call, referenceTime);
    case "scheduled_today":
      return isScheduledToday(call, referenceTime);
    case "pending_wrap_up":
      return !call.disposition;
    case "failed":
      return call.status === "failed";
    case "all":
    default:
      return true;
  }
}

function buildContactRecommendation(
  selectedRecentCall: CallSession | null,
  selectedContactFlags: ContactFlag[],
  selectedContactHandoffType: ContactHandoffType | undefined,
  selectedContactHandoffStatus: ContactHandoffStatus,
  selectedContactAttemptCount: number,
  selectedContactConnectedCount: number,
  selectedContactFailedCount: number,
  selectedContactNextFollowUp: CallSession | null,
  now: number
): ContactRecommendation | null {
  if (!selectedRecentCall) {
    return null;
  }

  if (selectedContactHandoffType && selectedContactHandoffStatus !== "resolved") {
    return {
      tone: "warning",
      title: `${formatHandoffType(selectedContactHandoffType)} handoff is open`,
      detail: "This contact already has an active handoff, so capture the next note carefully before retrying.",
      primaryAction: "focus_wrap_up",
      primaryLabel: "Review wrap-up"
    };
  }

  if (selectedContactFlags.includes("do_not_call")) {
    return {
      tone: "error",
      title: "Contact is marked Do Not Call",
      detail: "Dial actions should stay blocked until that policy flag is removed.",
      primaryAction: "focus_wrap_up",
      primaryLabel: "Review wrap-up"
    };
  }

  if (!selectedRecentCall.disposition) {
    return {
      tone: "warning",
      title: "Capture the wrap-up first",
      detail: "This attempt still has no saved outcome, so the contact history is incomplete.",
      primaryAction: "focus_wrap_up",
      primaryLabel: "Finish wrap-up",
      secondaryAction: "call_now",
      secondaryLabel: "Redial"
    };
  }

  if (selectedContactFlags.includes("vip") && selectedContactNextFollowUp) {
    return {
      tone: "warning",
      title: "VIP follow-up is waiting",
      detail: "This contact is flagged VIP, so the next step should stay visible and timely.",
      primaryAction: "call_now",
      primaryLabel: "Call now",
      secondaryAction: "schedule_1h",
      secondaryLabel: "Push 1h"
    };
  }

  if (selectedContactNextFollowUp && isOverdueFollowUp(selectedContactNextFollowUp, now)) {
    return {
      tone: "error",
      title: "Overdue callback needs action",
      detail: `${selectedRecentCall.remoteIdentity} is overdue for follow-up. Work it now or push a new slot.`,
      primaryAction: "call_now",
      primaryLabel: "Call now",
      secondaryAction: "schedule_1h",
      secondaryLabel: "Push 1h"
    };
  }

  if (
    needsFollowUp(selectedRecentCall) &&
    !selectedRecentCall.followUpAt
  ) {
    return {
      tone: "warning",
      title: "Callback has no scheduled time",
      detail: "The contact is queued for another attempt but does not yet have a due time.",
      primaryAction: "schedule_1h",
      primaryLabel: "Schedule 1h",
      secondaryAction: "schedule_tomorrow",
      secondaryLabel: "Tomorrow 10a"
    };
  }

  if (selectedContactFailedCount >= 2 && selectedContactConnectedCount === 0) {
    return {
      tone: "error",
      title: "Repeated failures on this contact",
      detail: `${selectedContactAttemptCount} attempts with no successful connection yet. Consider validating the number before retrying.`,
      primaryAction: "focus_wrap_up",
      primaryLabel: "Review notes",
      secondaryAction: "schedule_tomorrow",
      secondaryLabel: "Retry tomorrow"
    };
  }

  if (selectedContactConnectedCount > 0 && !selectedContactNextFollowUp) {
    return {
      tone: "success",
      title: "Contact looks resolved",
      detail: "You already have a successful outcome here and no open follow-up is scheduled.",
      primaryAction: "focus_wrap_up",
      primaryLabel: "Review wrap-up",
      secondaryAction: "call_now",
      secondaryLabel: "Dial anyway"
    };
  }

  return {
    tone: "info",
    title: "Ready for the next touch",
    detail: "Use the contact history and queue timing to decide whether to call now or plan the next attempt.",
    primaryAction: "call_now",
    primaryLabel: "Call now",
    secondaryAction: "schedule_1h",
    secondaryLabel: "Schedule 1h"
  };
}

export default function App() {
  const { client, snapshot } = useSoftphone();
  const adapterMode = import.meta.env.VITE_SOFTPHONE_MODE ?? "mock";
  const storedSettings = readStoredSettings();
  const [server, setServer] = useState(storedSettings.server);
  const [sipServer, setSipServer] = useState(storedSettings.sipServer);
  const [username, setUsername] = useState(storedSettings.username);
  const [password, setPassword] = useState("StrongPassword1001");
  const [dialNumber, setDialNumber] = useState("");
  const [timerText, setTimerText] = useState("00:00");
  const [selectedRecentCallId, setSelectedRecentCallId] = useState<string | null>(null);
  const [draftDisposition, setDraftDisposition] = useState<CallDisposition | "">("");
  const [draftNotes, setDraftNotes] = useState("");
  const [draftPriority, setDraftPriority] = useState<CallPriority>("normal");
  const [draftFollowUpAt, setDraftFollowUpAt] = useState("");
  const [historyQuery, setHistoryQuery] = useState("");
  const [reviewScope, setReviewScope] = useState<ReviewScope>("all");
  const [handoffQuery, setHandoffQuery] = useState("");
  const [handoffScope, setHandoffScope] = useState<HandoffScope>("all");
  const [draftHandoffType, setDraftHandoffType] = useState<ContactHandoffType | "">("");
  const [draftHandoffStatus, setDraftHandoffStatus] = useState<ContactHandoffStatus>("open");
  const [draftHandoffOwner, setDraftHandoffOwner] = useState("");
  const [draftHandoffDueAt, setDraftHandoffDueAt] = useState("");
  const [draftHandoffNote, setDraftHandoffNote] = useState("");
  const [briefFeedback, setBriefFeedback] = useState<BriefFeedback | null>(null);
  const [collapsedFollowUpBuckets, setCollapsedFollowUpBuckets] = useState<
    Record<FollowUpBucket, boolean>
  >(createInitialCollapsedBuckets);

  const hasLiveCall =
    snapshot.currentCall != null &&
    snapshot.currentCall.status !== "ended" &&
    snapshot.currentCall.status !== "failed";
  const now = Date.now();
  const selectedRecentCall =
    snapshot.recentCalls.find((call) => call.id === selectedRecentCallId) ??
    snapshot.recentCalls[0] ??
    null;
  const normalizedHistoryQuery = historyQuery.trim().toLowerCase();
  const selectedContactHistory = selectedRecentCall
    ? snapshot.recentCalls
        .filter((call) => contactKey(call.remoteIdentity) === contactKey(selectedRecentCall.remoteIdentity))
        .sort(
          (left, right) =>
            (right.endedAt ?? right.startedAt ?? 0) - (left.endedAt ?? left.startedAt ?? 0)
        )
    : [];
  const selectedContactFlags = orderContactFlags(
    selectedContactHistory.flatMap((call) => call.contactFlags ?? [])
  );
  const selectedContactHandoff =
    selectedContactHistory.find((call) => hasTrackedHandoff(call)) ?? null;
  const selectedContactHandoffType = selectedContactHandoff?.handoffType;
  const selectedContactHandoffStatus = selectedContactHandoff?.handoffStatus ?? "open";
  const selectedContactHandoffOwner = selectedContactHandoff?.handoffOwner ?? "";
  const selectedContactHandoffDueAt = selectedContactHandoff?.handoffDueAt;
  const selectedContactHandoffNote = selectedContactHandoff?.handoffNote ?? "";
  const selectedContactHandoffUpdatedAt = selectedContactHandoff?.handoffUpdatedAt;
  const selectedContactAttemptCount = selectedContactHistory.length;
  const selectedContactConnectedCount = selectedContactHistory.filter(
    (call) => call.disposition === "connected"
  ).length;
  const selectedContactFailedCount = selectedContactHistory.filter(
    (call) => call.status === "failed"
  ).length;
  const selectedContactOpenFollowUps = [...selectedContactHistory.filter(needsFollowUp)].sort(
    (left, right) => compareFollowUpCalls(left, right, now)
  );
  const selectedContactNextFollowUp = selectedContactOpenFollowUps[0] ?? null;
  const selectedContactRecentNotes = selectedContactHistory
    .map((call) => call.notes?.trim())
    .filter((note): note is string => Boolean(note))
    .slice(0, 3);
  const contactRecommendation = buildContactRecommendation(
    selectedRecentCall,
    selectedContactFlags,
    selectedContactHandoffType,
    selectedContactHandoffStatus,
    selectedContactAttemptCount,
    selectedContactConnectedCount,
    selectedContactFailedCount,
    selectedContactNextFollowUp,
    now
  );
  const followUpCalls = snapshot.recentCalls.filter(needsFollowUp);
  const sortedFollowUpCalls = [...followUpCalls].sort((left, right) =>
    compareFollowUpCalls(left, right, now)
  );
  const filteredRecentCalls = snapshot.recentCalls.filter(
    (call) =>
      matchesScope(call, reviewScope, now) && matchesSearch(call, normalizedHistoryQuery)
  );
  const filteredFollowUpCalls = sortedFollowUpCalls.filter(
    (call) =>
      matchesScope(call, reviewScope, now) && matchesSearch(call, normalizedHistoryQuery)
  );
  const groupedFollowUpCalls = groupFollowUpCalls(filteredFollowUpCalls, now);
  const groupedTotalFollowUpCalls = groupFollowUpCalls(sortedFollowUpCalls, now);
  const nextFollowUpCall =
    filteredFollowUpCalls.find(isCallableContact) ??
    sortedFollowUpCalls.find(isCallableContact) ??
    null;
  const overdueFollowUpCount = sortedFollowUpCalls.filter((call) =>
    isOverdueFollowUp(call, now)
  ).length;
  const todayFollowUpCount = sortedFollowUpCalls.filter((call) =>
    isScheduledToday(call, now)
  ).length;
  const blockedFollowUpCount = sortedFollowUpCalls.filter((call) =>
    hasContactFlag(call, "do_not_call")
  ).length;
  const openHandoffs = buildHandoffSummaries(
    snapshot.recentCalls,
    (call) => hasActiveHandoff(call),
    now
  ).sort(
    (left, right) =>
      (right.handoffUpdatedAt ?? right.latestCall.endedAt ?? right.latestCall.startedAt ?? 0) -
      (left.handoffUpdatedAt ?? left.latestCall.endedAt ?? left.latestCall.startedAt ?? 0)
  );
  const resolvedHandoffs = buildHandoffSummaries(
    snapshot.recentCalls,
    (call) => hasTrackedHandoff(call) && call.handoffStatus === "resolved",
    now
  ).sort(
    (left, right) =>
      (right.handoffUpdatedAt ?? right.latestCall.endedAt ?? right.latestCall.startedAt ?? 0) -
      (left.handoffUpdatedAt ?? left.latestCall.endedAt ?? left.latestCall.startedAt ?? 0)
  );
  const normalizedHandoffQuery = handoffQuery.trim().toLowerCase();
  const myHandoffCount = openHandoffs.filter(
    (handoff) =>
      Boolean(username.trim()) &&
      (handoff.handoffOwner?.trim().toLowerCase() ?? "") === username.trim().toLowerCase()
  ).length;
  const unassignedHandoffCount = openHandoffs.filter((handoff) => !handoff.handoffOwner).length;
  const dueTodayHandoffCount = openHandoffs.filter((handoff) => isHandoffDueToday(handoff, now)).length;
  const overdueHandoffCount = openHandoffs.filter((handoff) => isOverdueHandoff(handoff, now)).length;
  const staleHandoffCount = openHandoffs.filter((handoff) => isStaleHandoff(handoff, now)).length;
  const resolvedTodayHandoffCount = resolvedHandoffs.filter((handoff) =>
    isResolvedTodayHandoff(handoff, now)
  ).length;
  const filteredHandoffs = [...openHandoffs]
    .filter(
      (handoff) =>
        matchesHandoffScope(handoff, handoffScope, username, now) &&
        matchesHandoffSearch(handoff, normalizedHandoffQuery)
    )
    .sort((left, right) => compareHandoffs(left, right, now));
  const filteredResolvedHandoffs = resolvedHandoffs.filter((handoff) =>
    matchesHandoffSearch(handoff, normalizedHandoffQuery)
  );
  const visibleResolvedHandoffs = filteredResolvedHandoffs.slice(0, 4);
  const nextUrgentHandoff = filteredHandoffs[0] ?? null;
  const pendingWrapUpCount = snapshot.recentCalls.filter((call) => !call.disposition).length;
  const scheduleActive = isFollowUpDisposition(draftDisposition);
  const parsedDraftFollowUpAt = parseDateTimeLocalValue(draftFollowUpAt);
  const hasWrapUpChanges =
    selectedRecentCall != null &&
    (draftDisposition !== (selectedRecentCall.disposition ?? "") ||
      draftNotes !== (selectedRecentCall.notes ?? "") ||
      draftPriority !== (selectedRecentCall.priority ?? "normal") ||
      draftFollowUpAt !== formatDateTimeLocalValue(selectedRecentCall.followUpAt));
  const hasHandoffChanges =
    selectedRecentCall != null &&
    (draftHandoffType !== (selectedContactHandoffType ?? "") ||
      draftHandoffStatus !== selectedContactHandoffStatus ||
      draftHandoffOwner !== selectedContactHandoffOwner ||
      draftHandoffDueAt !== formatDateTimeLocalValue(selectedContactHandoffDueAt) ||
      draftHandoffNote !== selectedContactHandoffNote);
  const selectedHandoffBrief =
    selectedRecentCall &&
    (draftHandoffType ||
      draftHandoffNote.trim() ||
      draftHandoffOwner.trim() ||
      draftHandoffDueAt ||
      selectedContactHandoffType)
      ? buildHandoffBrief(
          {
            remoteIdentity: selectedRecentCall.remoteIdentity,
            handoffType: draftHandoffType || selectedContactHandoffType,
            handoffStatus: draftHandoffStatus,
            handoffOwner: draftHandoffOwner.trim() || undefined,
            handoffNote: draftHandoffNote.trim() || undefined,
            handoffDueAt: parseDateTimeLocalValue(draftHandoffDueAt) ?? selectedContactHandoffDueAt,
            handoffUpdatedAt: selectedContactHandoffUpdatedAt,
            contactFlags: selectedContactFlags,
            attemptCount: selectedContactAttemptCount,
            connectedCount: selectedContactConnectedCount,
            failedCount: selectedContactFailedCount,
            nextFollowUpAt: selectedContactNextFollowUp?.followUpAt,
            latestCall: selectedRecentCall,
            recentNotes: selectedContactRecentNotes
          },
          now
        )
      : null;

  useEffect(() => {
    window.localStorage.setItem(
      SETTINGS_STORAGE_KEY,
      JSON.stringify({
        server,
        sipServer,
        username
      } satisfies StoredSettings)
    );
  }, [server, sipServer, username]);

  useEffect(() => {
    if (!snapshot.currentCall?.startedAt) {
      setTimerText("00:00");
      return;
    }

    const interval = window.setInterval(() => {
      setTimerText(formatLiveDuration(snapshot.currentCall?.startedAt));
    }, 1000);
    return () => window.clearInterval(interval);
  }, [snapshot.currentCall?.startedAt]);

  useEffect(() => {
    if (snapshot.recentCalls.length === 0) {
      setSelectedRecentCallId(null);
      return;
    }

    if (!selectedRecentCallId || !snapshot.recentCalls.some((call) => call.id === selectedRecentCallId)) {
      setSelectedRecentCallId(snapshot.recentCalls[0].id);
    }
  }, [snapshot.recentCalls, selectedRecentCallId]);

  useEffect(() => {
    setDraftDisposition(selectedRecentCall?.disposition ?? "");
    setDraftNotes(selectedRecentCall?.notes ?? "");
    setDraftPriority(selectedRecentCall?.priority ?? "normal");
    setDraftFollowUpAt(formatDateTimeLocalValue(selectedRecentCall?.followUpAt));
    setDraftHandoffType(selectedContactHandoffType ?? "");
    setDraftHandoffStatus(selectedContactHandoffStatus);
    setDraftHandoffOwner(selectedContactHandoffOwner);
    setDraftHandoffDueAt(formatDateTimeLocalValue(selectedContactHandoffDueAt));
    setDraftHandoffNote(selectedContactHandoffNote);
  }, [
    selectedRecentCall?.id,
    selectedRecentCall?.disposition,
    selectedRecentCall?.notes,
    selectedRecentCall?.priority,
    selectedRecentCall?.followUpAt,
    selectedContactHandoffType,
    selectedContactHandoffStatus,
    selectedContactHandoffOwner,
    selectedContactHandoffDueAt,
    selectedContactHandoffNote
  ]);

  useEffect(() => {
    if (!briefFeedback) {
      return;
    }

    const timeout = window.setTimeout(() => setBriefFeedback(null), 2500);
    return () => window.clearTimeout(timeout);
  }, [briefFeedback]);

  async function handleRegister(event: FormEvent) {
    event.preventDefault();
    await client.connect({
      sipServer,
      websocketUrl: server,
      username,
      password,
      displayName: `Agent ${username}`
    });
  }

  function loadNumber(value: string) {
    setDialNumber(normalizeDestination(value));
  }

  function handleSaveWrapUp() {
    if (!selectedRecentCall) {
      return;
    }

    client.saveRecentCallWrapUp(selectedRecentCall.id, {
      disposition: draftDisposition || undefined,
      notes: draftNotes.trim() ? draftNotes.trim() : undefined,
      priority: scheduleActive ? draftPriority : undefined,
      followUpAt: scheduleActive ? parsedDraftFollowUpAt : undefined
    });
  }

  function handleQuickSchedule(call: CallSession, followUpAt: number, priority?: CallPriority) {
    client.saveRecentCallWrapUp(call.id, {
      disposition: call.disposition,
      notes: call.notes,
      priority: priority ?? call.priority ?? "normal",
      followUpAt
    });
  }

  function handleBulkSchedule(calls: CallSession[], followUpAt: number, priority?: CallPriority) {
    calls.filter(isCallableContact).forEach((call) => {
      client.saveRecentCallWrapUp(call.id, {
        disposition: call.disposition,
        notes: call.notes,
        priority: priority ?? call.priority ?? "normal",
        followUpAt
      });
    });
  }

  function handleQueueCallNow(call: CallSession) {
    setSelectedRecentCallId(call.id);
    setReviewScope("follow_up");
    if (!isCallableContact(call)) {
      return;
    }

    loadNumber(call.remoteIdentity);
  }

  function handleQueuePriority(call: CallSession, priority: CallPriority) {
    client.saveRecentCallWrapUp(call.id, {
      disposition: call.disposition,
      notes: call.notes,
      priority,
      followUpAt: call.followUpAt
    });
  }

  function handleBulkQueuePriority(calls: CallSession[], priority: CallPriority) {
    calls.forEach((call) => {
      client.saveRecentCallWrapUp(call.id, {
        disposition: call.disposition,
        notes: call.notes,
        priority,
        followUpAt: call.followUpAt
      });
    });
  }

  function handleResolveFollowUp(call: CallSession) {
    client.saveRecentCallWrapUp(call.id, {
      disposition: "connected",
      notes: call.notes,
      priority: undefined,
      followUpAt: undefined
    });
  }

  function handleClearQueueSchedule(call: CallSession) {
    client.saveRecentCallWrapUp(call.id, {
      disposition: call.disposition,
      notes: call.notes,
      priority: undefined,
      followUpAt: undefined
    });
  }

  function handleResetWrapUp() {
    setDraftDisposition(selectedRecentCall?.disposition ?? "");
    setDraftNotes(selectedRecentCall?.notes ?? "");
    setDraftPriority(selectedRecentCall?.priority ?? "normal");
    setDraftFollowUpAt(formatDateTimeLocalValue(selectedRecentCall?.followUpAt));
  }

  function handleSaveHandoff() {
    if (!selectedRecentCall) {
      return;
    }

    const hasDraftHandoff =
      Boolean(draftHandoffType || draftHandoffNote.trim() || draftHandoffOwner.trim());

    client.saveContactHandoff(selectedRecentCall.remoteIdentity, {
      handoffType: hasDraftHandoff ? draftHandoffType || selectedContactHandoffType || "supervisor" : undefined,
      handoffStatus: hasDraftHandoff ? draftHandoffStatus : undefined,
      handoffOwner: hasDraftHandoff && draftHandoffOwner.trim() ? draftHandoffOwner.trim() : undefined,
      handoffNote: draftHandoffNote.trim() ? draftHandoffNote.trim() : undefined,
      handoffDueAt: hasDraftHandoff ? parseDateTimeLocalValue(draftHandoffDueAt) : undefined,
      handoffUpdatedAt: hasDraftHandoff ? Date.now() : undefined
    });
  }

  function handleClearHandoff(remoteIdentity?: string) {
    const targetIdentity = remoteIdentity ?? selectedRecentCall?.remoteIdentity;
    if (!targetIdentity) {
      return;
    }

    client.saveContactHandoff(targetIdentity, {
      handoffType: undefined,
      handoffStatus: undefined,
      handoffOwner: undefined,
      handoffNote: undefined,
      handoffDueAt: undefined,
      handoffUpdatedAt: undefined
    });
  }

  function handleClaimHandoff(
    remoteIdentity: string,
    handoffType: ContactHandoffType,
    handoffNote?: string,
    handoffDueAt?: number
  ) {
    client.saveContactHandoff(remoteIdentity, {
      handoffType,
      handoffStatus: "claimed",
      handoffOwner: username,
      handoffNote,
      handoffDueAt,
      handoffUpdatedAt: Date.now()
    });
  }

  function handleReopenHandoff(
    remoteIdentity: string,
    handoffType: ContactHandoffType,
    handoffNote?: string,
    handoffDueAt?: number
  ) {
    client.saveContactHandoff(remoteIdentity, {
      handoffType,
      handoffStatus: "open",
      handoffOwner: undefined,
      handoffNote,
      handoffDueAt,
      handoffUpdatedAt: Date.now()
    });
  }

  function handleResolveHandoff(
    remoteIdentity: string,
    handoffType: ContactHandoffType,
    handoffNote?: string,
    handoffOwner?: string,
    handoffDueAt?: number
  ) {
    client.saveContactHandoff(remoteIdentity, {
      handoffType,
      handoffStatus: "resolved",
      handoffOwner,
      handoffNote,
      handoffDueAt,
      handoffUpdatedAt: Date.now()
    });
  }

  function handleQuickHandoffDue(handoff: ContactHandoffSummary, handoffDueAt?: number) {
    client.saveContactHandoff(handoff.remoteIdentity, {
      handoffType: handoff.handoffType,
      handoffStatus: handoff.handoffStatus ?? "open",
      handoffOwner: handoff.handoffOwner,
      handoffNote: handoff.handoffNote,
      handoffDueAt,
      handoffUpdatedAt: Date.now()
    });
  }

  async function handleCopyBrief(key: string, brief: string) {
    try {
      await copyTextToClipboard(brief);
      setBriefFeedback({
        key,
        tone: "success",
        message: "Brief copied"
      });
    } catch {
      setBriefFeedback({
        key,
        tone: "error",
        message: "Clipboard unavailable"
      });
    }
  }

  function handleToggleContactFlag(flag: ContactFlag) {
    if (!selectedRecentCall) {
      return;
    }

    const nextFlags = selectedContactFlags.includes(flag)
      ? selectedContactFlags.filter((value) => value !== flag)
      : orderContactFlags([...selectedContactFlags, flag]);

    client.saveContactFlags(selectedRecentCall.remoteIdentity, nextFlags);
  }

  function handleRecommendationAction(action: RecommendationAction) {
    if (!selectedRecentCall) {
      return;
    }

    switch (action) {
      case "call_now":
        handleQueueCallNow(selectedRecentCall);
        return;
      case "schedule_1h":
        setDraftDisposition(
          isFollowUpDisposition(draftDisposition) ? draftDisposition : "callback"
        );
        setDraftFollowUpAt(formatDateTimeLocalValue(scheduleInMinutes(now, 60)));
        return;
      case "schedule_tomorrow":
        setDraftDisposition(
          isFollowUpDisposition(draftDisposition) ? draftDisposition : "callback"
        );
        setDraftFollowUpAt(formatDateTimeLocalValue(tomorrowAt(now, 10, 0)));
        return;
      case "focus_wrap_up":
      default:
        return;
    }
  }

  function handleFocusNextFollowUp() {
    if (!nextFollowUpCall) {
      return;
    }

    setSelectedRecentCallId(nextFollowUpCall.id);
    setReviewScope("follow_up");
    loadNumber(nextFollowUpCall.remoteIdentity);
  }

  function handleFocusNextHandoff() {
    if (!nextUrgentHandoff) {
      return;
    }

    setSelectedRecentCallId(nextUrgentHandoff.latestCall.id);
    if (isCallableContact(nextUrgentHandoff.latestCall)) {
      loadNumber(nextUrgentHandoff.remoteIdentity);
    }
  }

  function resetReviewQueue() {
    setHistoryQuery("");
    setReviewScope("all");
  }

  function toggleFollowUpBucket(bucket: FollowUpBucket) {
    setCollapsedFollowUpBuckets((current) => ({
      ...current,
      [bucket]: !current[bucket]
    }));
  }

  return (
    <div className="page-shell">
      <div className="bg-grid" />
      <main className="softphone-card">
        <section className="hero-row">
          <div>
            <p className="eyebrow">Vantage Dialer</p>
            <h1>Softphone Console</h1>
            <p className="subcopy">
              The standalone softphone now keeps operator-friendly recent call history,
              wrap-up notes, and a live activity feed inside
              <code> calling-core </code>
              so the same state model can drop into the future agent workspace and survive
              a browser refresh.
            </p>
            <p className="subcopy">
              Adapter mode:
              <code> {adapterMode} </code>
            </p>
          </div>
          <div className={`status-pill ${snapshot.registrationState}`}>
            {snapshot.registrationState.replace(/_/g, " ")}
          </div>
        </section>

        <section className="grid-two">
          <form className="panel" onSubmit={handleRegister}>
            <h2>Agent Login</h2>
            <p className="panel-copy">Connection defaults are saved locally on this browser.</p>
            <label>
              SIP Domain
              <input value={sipServer} onChange={(event) => setSipServer(event.target.value)} />
            </label>
            <label>
              WebSocket / SIP Gateway
              <input value={server} onChange={(event) => setServer(event.target.value)} />
            </label>
            <label>
              Username
              <input value={username} onChange={(event) => setUsername(event.target.value)} />
            </label>
            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>
            <div className="button-row">
              <button type="submit">Register</button>
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  void client.disconnect();
                }}
              >
                Logout
              </button>
            </div>
          </form>

          <section className="panel">
            <h2>Dial Pad</h2>
            <p className="panel-copy">Prep the next number while the current call is live.</p>
            <label>
              Destination
              <input
                placeholder="+919840605775"
                value={dialNumber}
                onChange={(event) => setDialNumber(event.target.value)}
              />
            </label>
            <div className="button-row">
              <button
                type="button"
                onClick={() => {
                  void client.dial(normalizeDestination(dialNumber));
                }}
                disabled={snapshot.registrationState !== "registered" || !dialNumber}
              >
                Make Call
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => setDialNumber("")}
                disabled={!dialNumber}
              >
                Clear
              </button>
              <button
                type="button"
                className="ghost"
                onClick={() => setDialNumber((current) => current.slice(0, -1))}
                disabled={!dialNumber}
              >
                Backspace
              </button>
            </div>
            <div className="dial-grid">
              {["1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"].map((key) => (
                <button
                  key={key}
                  type="button"
                  className="digit"
                  onClick={() => setDialNumber((current) => current + key)}
                >
                  {key}
                </button>
              ))}
            </div>
          </section>
        </section>

        <section className="panel call-panel">
          <div className="call-header">
            <div>
              <h2>Call State</h2>
              <p className="call-identity">
                {snapshot.currentCall
                  ? `${snapshot.currentCall.direction.toUpperCase()} - ${snapshot.currentCall.remoteIdentity}`
                  : "No active call"}
              </p>
            </div>
            <div className="timer">{timerText}</div>
          </div>

          <div className="state-strip">
            <span>Call status: {snapshot.currentCall?.status ?? "idle"}</span>
            <span>Muted: {snapshot.currentCall?.muted ? "yes" : "no"}</span>
            <span>Held: {snapshot.currentCall?.held ? "yes" : "no"}</span>
          </div>

          <div className="button-row wrap">
            <button
              type="button"
              onClick={() => {
                void client.answer();
              }}
              disabled={snapshot.currentCall?.status !== "ringing"}
            >
              Answer
            </button>
            <button
              type="button"
              className="danger"
              onClick={() => {
                void client.reject();
              }}
              disabled={snapshot.currentCall?.status !== "ringing"}
            >
              Reject
            </button>
            <button
              type="button"
              onClick={() => {
                void client.hangup();
              }}
              disabled={!hasLiveCall}
            >
              Hangup
            </button>
            <button
              type="button"
              className="ghost"
              onClick={() => {
                void client.mute(!snapshot.currentCall?.muted);
              }}
              disabled={!hasLiveCall}
            >
              {snapshot.currentCall?.muted ? "Unmute" : "Mute"}
            </button>
            <button
              type="button"
              className="ghost"
              onClick={() => {
                void client.hold(!snapshot.currentCall?.held);
              }}
              disabled={!hasLiveCall}
            >
              {snapshot.currentCall?.held ? "Resume" : "Hold"}
            </button>
          </div>

          {snapshot.lastError ? <p className="error-text">{snapshot.lastError}</p> : null}
        </section>

        <section className="summary-grid">
          <article className="summary-card emphasis">
            <p className="summary-label">Follow-Up Queue</p>
            <p className="summary-value">{sortedFollowUpCalls.length}</p>
            <p className="summary-copy">Calls marked for callback, voicemail, or another try.</p>
          </article>
          <article className="summary-card">
            <p className="summary-label">Overdue</p>
            <p className="summary-value">{overdueFollowUpCount}</p>
            <p className="summary-copy">Follow-ups whose scheduled time has already passed.</p>
          </article>
          <article className="summary-card">
            <p className="summary-label">Due Today</p>
            <p className="summary-value">{todayFollowUpCount}</p>
            <p className="summary-copy">Scheduled follow-ups that should be worked today.</p>
          </article>
          <article className="summary-card">
            <p className="summary-label">Pending Wrap-Up</p>
            <p className="summary-value">{pendingWrapUpCount}</p>
            <p className="summary-copy">Finished calls still missing a saved disposition.</p>
          </article>
          <article className="summary-card">
            <p className="summary-label">DNC Blocked</p>
            <p className="summary-value">{blockedFollowUpCount}</p>
            <p className="summary-copy">Queued follow-ups currently blocked from redial.</p>
          </article>
          <article className="summary-card">
            <p className="summary-label">Open Handoffs</p>
            <p className="summary-value">{openHandoffs.length}</p>
            <p className="summary-copy">Contacts waiting on supervisor, verification, or compliance review.</p>
          </article>
          <article className="summary-card">
            <p className="summary-label">Resolved Today</p>
            <p className="summary-value">{resolvedTodayHandoffCount}</p>
            <p className="summary-copy">Escalations completed today but still easy to review or reopen.</p>
          </article>
        </section>

        <section className="panel workflow-panel">
          <div className="workflow-header">
            <div>
              <h2>Review Queue</h2>
              <p className="panel-copy">
                Search call history, narrow the review lane, or jump straight to the next follow-up.
              </p>
              <p className="queue-hint">
                {nextFollowUpCall
                  ? `Next due: ${nextFollowUpCall.remoteIdentity} / ${formatFollowUpTiming(
                      nextFollowUpCall.followUpAt,
                      now
                    )}`
                  : sortedFollowUpCalls.length > 0
                    ? "Next due: queued follow-ups are currently blocked by Do Not Call policy."
                    : "Next due: no follow-up currently queued."}
              </p>
            </div>
            <div className="workflow-actions">
              <button type="button" onClick={handleFocusNextFollowUp} disabled={!nextFollowUpCall}>
                Focus next follow-up
              </button>
              <button
                type="button"
                className="ghost"
                onClick={resetReviewQueue}
                disabled={!historyQuery && reviewScope === "all"}
              >
                Reset view
              </button>
            </div>
          </div>

          <label className="search-label">
            Search calls or notes
            <input
              placeholder="Search number, notes, disposition, or state"
              value={historyQuery}
              onChange={(event) => setHistoryQuery(event.target.value)}
            />
          </label>

          <div className="filter-chips">
            {reviewScopeOptions.map((option) => (
              <button
                key={option.value}
                type="button"
                className={`chip-button${reviewScope === option.value ? " selected" : ""}`}
                onClick={() => setReviewScope(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </section>

        <section className="support-grid">
          <section className="panel">
            <div className="section-head">
              <div>
                <h2>Recent Calls</h2>
                <p className="panel-copy">
                  Finished calls stay visible for quick redial, review, and wrap-up.
                </p>
              </div>
              <div className="section-actions">
                <button
                  type="button"
                  className="ghost mini-button"
                  onClick={() => client.clearRecentCalls()}
                  disabled={snapshot.recentCalls.length === 0}
                >
                  Clear history
                </button>
                <span className="section-badge">
                  {filteredRecentCalls.length}/{snapshot.recentCalls.length}
                </span>
              </div>
            </div>

            {snapshot.recentCalls.length === 0 ? (
              <p className="empty-state">
                Recent calls will appear here once the first conversation ends.
              </p>
            ) : filteredRecentCalls.length === 0 ? (
              <p className="empty-state">
                No recent calls match the current search and filter view.
              </p>
            ) : (
              <div className="history-list">
                {filteredRecentCalls.map((call) => (
                  <article
                    key={`${call.id}-${call.status}-${call.endedAt ?? call.startedAt ?? 0}`}
                    className={`history-item${selectedRecentCallId === call.id ? " selected" : ""}`}
                  >
                    <div className="history-main">
                      <div>
                        <p className="history-identity">{call.remoteIdentity}</p>
                        <p className="history-meta">
                          {call.direction} / {formatCallResult(call.status)} / {formatCompletedDuration(call)}
                        </p>
                      </div>
                      <div className="history-tags">
                        {call.disposition ? (
                          <span className="status-tag wrap-up">{formatDisposition(call.disposition)}</span>
                        ) : null}
                        {call.handoffType ? (
                          <span className={`status-tag handoff ${call.handoffType}`}>
                            {formatHandoffType(call.handoffType)}
                          </span>
                        ) : null}
                        {call.handoffType ? (
                          <span className={`status-tag handoff-status ${call.handoffStatus ?? "open"}`}>
                            {formatHandoffStatus(call.handoffStatus ?? "open")}
                          </span>
                        ) : null}
                        {(call.contactFlags ?? []).map((flag) => (
                          <span key={`${call.id}-${flag}`} className={`status-tag contact-flag ${flag}`}>
                            {formatContactFlag(flag)}
                          </span>
                        ))}
                        <span className={`status-tag ${call.status}`}>{formatCallResult(call.status)}</span>
                      </div>
                    </div>
                    {call.notes ? <p className="history-note">{call.notes}</p> : null}
                    <div className="history-actions">
                      <span className="history-time">
                        {formatTimestamp(call.endedAt ?? call.startedAt ?? Date.now())}
                      </span>
                      <div className="history-buttons">
                        <button
                          type="button"
                          className="ghost mini-button"
                          onClick={() => setSelectedRecentCallId(call.id)}
                        >
                          Review
                        </button>
                        <button
                          type="button"
                          className="ghost mini-button"
                          onClick={() => loadNumber(call.remoteIdentity)}
                          disabled={!isCallableContact(call)}
                        >
                          Use number
                        </button>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          <div className="stacked-panels">
            <section className="panel">
              <div className="section-head">
                <div>
                  <h2>Handoff Desk</h2>
                  <p className="panel-copy">
                    Contacts flagged for supervisor, verification, or compliance follow-up stay visible here.
                  </p>
                </div>
                <span className="section-badge">{filteredHandoffs.length}/{openHandoffs.length}</span>
              </div>

              <label className="search-label">
                Search handoffs
                <input
                  placeholder="Search contact, owner, type, note, or flag"
                  value={handoffQuery}
                  onChange={(event) => setHandoffQuery(event.target.value)}
                />
              </label>

              <div className="filter-chips">
                {handoffScopeOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`chip-button${handoffScope === option.value ? " selected" : ""}`}
                    onClick={() => setHandoffScope(option.value)}
                  >
                    {option.label}
                  </button>
                ))}
              </div>

              <div className="handoff-summary-strip">
                <span className="status-tag handoff-status claimed">Mine {myHandoffCount}</span>
                <span className="status-tag handoff-status open">Unassigned {unassignedHandoffCount}</span>
                <span className="status-tag due-today">Due today {dueTodayHandoffCount}</span>
                <span className="status-tag overdue">Overdue {overdueHandoffCount}</span>
                <span className="status-tag stale">Stale {staleHandoffCount}</span>
              </div>

              <div className="handoff-toolbar">
                <p className="handoff-toolbar-copy">
                  {nextUrgentHandoff
                    ? `Next urgent: ${nextUrgentHandoff.remoteIdentity} / ${formatHandoffType(
                        nextUrgentHandoff.handoffType
                      )} / ${formatHandoffDeskHint(nextUrgentHandoff, now)}`
                    : "No urgent handoff is visible in the current triage view."}
                </p>
                <div className="section-actions">
                  <button
                    type="button"
                    className="ghost mini-button"
                    onClick={handleFocusNextHandoff}
                    disabled={!nextUrgentHandoff}
                  >
                    Focus next urgent
                  </button>
                </div>
              </div>

              {openHandoffs.length === 0 ? (
                <p className="empty-state">
                  Save a contact handoff from the wrap-up workspace to build this desk.
                </p>
              ) : filteredHandoffs.length === 0 ? (
                <p className="empty-state">
                  No handoffs match the current search and triage view.
                </p>
              ) : (
                <div className="handoff-list">
                  {filteredHandoffs.map((handoff) => {
                    const handoffBrief = buildHandoffBrief(
                      {
                        remoteIdentity: handoff.remoteIdentity,
                        handoffType: handoff.handoffType,
                        handoffStatus: handoff.handoffStatus,
                        handoffOwner: handoff.handoffOwner,
                        handoffNote: handoff.handoffNote,
                        handoffDueAt: handoff.handoffDueAt,
                        handoffUpdatedAt: handoff.handoffUpdatedAt,
                        contactFlags: handoff.contactFlags,
                        attemptCount: handoff.attemptCount,
                        connectedCount: handoff.connectedCount,
                        failedCount: handoff.failedCount,
                        nextFollowUpAt: handoff.nextFollowUpAt,
                        latestCall: handoff.latestCall,
                        recentNotes: handoff.recentNotes
                      },
                      now
                    );

                    return (
                      <article key={`handoff-${handoff.key}`} className="handoff-item">
                        <div className="handoff-main">
                          <div>
                            <p className="handoff-identity">{handoff.remoteIdentity}</p>
                            <p className="handoff-meta">
                              {formatHandoffType(handoff.handoffType)} /{" "}
                              {formatDateTime(
                                handoff.handoffUpdatedAt ??
                                  handoff.latestCall.endedAt ??
                                  handoff.latestCall.startedAt ??
                                  now
                              )}
                            </p>
                          </div>
                          <div className="handoff-badges">
                            <span className={`status-tag handoff ${handoff.handoffType}`}>
                              {formatHandoffType(handoff.handoffType)}
                            </span>
                            <span className={`status-tag handoff-status ${handoff.handoffStatus ?? "open"}`}>
                              {formatHandoffStatus(handoff.handoffStatus ?? "open")}
                            </span>
                            {handoff.handoffDueAt ? (
                              <span
                                className={`status-tag handoff-due ${
                                  isOverdueHandoff(handoff, now)
                                    ? "overdue"
                                    : isHandoffDueToday(handoff, now)
                                      ? "due-today"
                                      : "upcoming"
                                }`}
                              >
                                {formatHandoffDueTiming(handoff.handoffDueAt, now)}
                              </span>
                            ) : null}
                            {isStaleHandoff(handoff, now) ? (
                              <span className="status-tag stale">{handoffAgeLabel(handoff, now)}</span>
                            ) : null}
                            {(handoff.handoffOwner?.trim().toLowerCase() ?? "") ===
                            username.trim().toLowerCase() ? (
                              <span className="status-tag mine">Mine</span>
                            ) : null}
                            {handoff.contactFlags.map((flag) => (
                              <span
                                key={`handoff-${handoff.key}-${flag}`}
                                className={`status-tag contact-flag ${flag}`}
                              >
                                {formatContactFlag(flag)}
                              </span>
                            ))}
                          </div>
                        </div>
                        <p className="handoff-note">
                          {handoff.handoffNote || "No handoff note saved yet."}
                        </p>
                        <p className="handoff-meta-line">
                          Owner: {handoff.handoffOwner || "Unassigned"} / {handoffAgeLabel(handoff, now)}
                          {handoff.handoffDueAt
                            ? ` / ${formatHandoffDueTiming(handoff.handoffDueAt, now)}`
                            : ""}
                        </p>
                        <div className="handoff-actions">
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => setSelectedRecentCallId(handoff.latestCall.id)}
                          >
                            Open
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => handleQueueCallNow(handoff.latestCall)}
                            disabled={!isCallableContact(handoff.latestCall)}
                          >
                            {isCallableContact(handoff.latestCall) ? "Load number" : "Dial blocked"}
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => void handleCopyBrief(`handoff-${handoff.key}`, handoffBrief)}
                          >
                            Copy brief
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() =>
                              handoff.handoffStatus === "claimed"
                                ? handleReopenHandoff(
                                    handoff.remoteIdentity,
                                    handoff.handoffType,
                                    handoff.handoffNote,
                                    handoff.handoffDueAt
                                  )
                                : handleClaimHandoff(
                                    handoff.remoteIdentity,
                                    handoff.handoffType,
                                    handoff.handoffNote,
                                    handoff.handoffDueAt
                                  )
                            }
                          >
                            {handoff.handoffStatus === "claimed" ? "Reopen" : "Claim to me"}
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() =>
                              handleResolveHandoff(
                                handoff.remoteIdentity,
                                handoff.handoffType,
                                handoff.handoffNote,
                                handoff.handoffOwner,
                                handoff.handoffDueAt
                              )
                            }
                          >
                            Resolve
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => handleClearHandoff(handoff.remoteIdentity)}
                          >
                            Clear handoff
                          </button>
                        </div>
                        <div className="handoff-presets">
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => handleQuickHandoffDue(handoff, scheduleInMinutes(now, 30))}
                          >
                            Due 30m
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => handleQuickHandoffDue(handoff, scheduleInMinutes(now, 120))}
                          >
                            Due 2h
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => handleQuickHandoffDue(handoff, tomorrowAt(now, 10, 0))}
                          >
                            Tomorrow 10a
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => handleQuickHandoffDue(handoff, undefined)}
                            disabled={!handoff.handoffDueAt}
                          >
                            Clear due
                          </button>
                        </div>
                        {briefFeedback?.key === `handoff-${handoff.key}` ? (
                          <p className={`brief-feedback ${briefFeedback.tone}`}>
                            {briefFeedback.message}
                          </p>
                        ) : null}
                      </article>
                    );
                  })}
                </div>
              )}
            </section>

            <section className="panel">
              <div className="section-head">
                <div>
                  <h2>Recently Resolved</h2>
                  <p className="panel-copy">
                    Completed handoffs stay visible here for quick reopen, review, or callback context.
                  </p>
                </div>
                <span className="section-badge">
                  {visibleResolvedHandoffs.length}/{resolvedHandoffs.length}
                </span>
              </div>

              {resolvedHandoffs.length === 0 ? (
                <p className="empty-state">
                  Resolved handoffs will collect here once an escalation is completed.
                </p>
              ) : visibleResolvedHandoffs.length === 0 ? (
                <p className="empty-state">
                  No resolved handoffs match the current handoff search.
                </p>
              ) : (
                <div className="handoff-list">
                  {visibleResolvedHandoffs.map((handoff) => {
                    const handoffBrief = buildHandoffBrief(
                      {
                        remoteIdentity: handoff.remoteIdentity,
                        handoffType: handoff.handoffType,
                        handoffStatus: handoff.handoffStatus,
                        handoffOwner: handoff.handoffOwner,
                        handoffNote: handoff.handoffNote,
                        handoffDueAt: handoff.handoffDueAt,
                        handoffUpdatedAt: handoff.handoffUpdatedAt,
                        contactFlags: handoff.contactFlags,
                        attemptCount: handoff.attemptCount,
                        connectedCount: handoff.connectedCount,
                        failedCount: handoff.failedCount,
                        nextFollowUpAt: handoff.nextFollowUpAt,
                        latestCall: handoff.latestCall,
                        recentNotes: handoff.recentNotes
                      },
                      now
                    );

                    return (
                      <article key={`resolved-handoff-${handoff.key}`} className="handoff-item resolved-handoff-item">
                        <div className="handoff-main">
                          <div>
                            <p className="handoff-identity">{handoff.remoteIdentity}</p>
                            <p className="handoff-meta">
                              {formatHandoffType(handoff.handoffType)} /{" "}
                              {formatDateTime(
                                handoff.handoffUpdatedAt ??
                                  handoff.latestCall.endedAt ??
                                  handoff.latestCall.startedAt ??
                                  now
                              )}
                            </p>
                          </div>
                          <div className="handoff-badges">
                            <span className={`status-tag handoff ${handoff.handoffType}`}>
                              {formatHandoffType(handoff.handoffType)}
                            </span>
                            <span className="status-tag handoff-status resolved">Resolved</span>
                            {handoff.contactFlags.map((flag) => (
                              <span
                                key={`resolved-handoff-${handoff.key}-${flag}`}
                                className={`status-tag contact-flag ${flag}`}
                              >
                                {formatContactFlag(flag)}
                              </span>
                            ))}
                          </div>
                        </div>
                        <p className="handoff-note">
                          {handoff.handoffNote || "No handoff note saved for this contact."}
                        </p>
                        <p className="handoff-meta-line">
                          Resolved: {formatDateTime(handoff.handoffUpdatedAt ?? handoffTimestamp(handoff, now))}
                          {handoff.handoffOwner ? ` / ${handoff.handoffOwner}` : ""}
                        </p>
                        <div className="handoff-actions">
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => setSelectedRecentCallId(handoff.latestCall.id)}
                          >
                            Open
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => handleQueueCallNow(handoff.latestCall)}
                            disabled={!isCallableContact(handoff.latestCall)}
                          >
                            {isCallableContact(handoff.latestCall) ? "Load number" : "Dial blocked"}
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => void handleCopyBrief(`resolved-handoff-${handoff.key}`, handoffBrief)}
                          >
                            Copy brief
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() =>
                              handleReopenHandoff(
                                handoff.remoteIdentity,
                                handoff.handoffType,
                                handoff.handoffNote,
                                handoff.handoffDueAt
                              )
                            }
                          >
                            Reopen
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => handleClearHandoff(handoff.remoteIdentity)}
                          >
                            Clear handoff
                          </button>
                        </div>
                        {briefFeedback?.key === `resolved-handoff-${handoff.key}` ? (
                          <p className={`brief-feedback ${briefFeedback.tone}`}>
                            {briefFeedback.message}
                          </p>
                        ) : null}
                      </article>
                    );
                  })}
                </div>
              )}
            </section>

            <section className="panel">
              <div className="section-head">
                <div>
                  <h2>Follow-Up Queue</h2>
                  <p className="panel-copy">
                    Calls tagged for callback or retry stay grouped here and sort by urgency.
                  </p>
                </div>
                <span className="section-badge">
                  {filteredFollowUpCalls.length}/{sortedFollowUpCalls.length}
                </span>
              </div>

              {sortedFollowUpCalls.length === 0 ? (
                <p className="empty-state">
                  Save a callback, voicemail, or no-answer outcome to build the queue.
                </p>
              ) : filteredFollowUpCalls.length === 0 ? (
                <p className="empty-state">
                  No follow-up calls match the current search view.
                </p>
              ) : (
                <div className="follow-up-board">
                  {followUpBucketOrder.map((bucket) => {
                    const bucketCalls = groupedFollowUpCalls[bucket];
                    const totalBucketCalls = groupedTotalFollowUpCalls[bucket];

                    if (bucketCalls.length === 0) {
                      return null;
                    }

                    const bucketLead = bucketCalls[0];
                    const actionableBucketLead = bucketCalls.find(isCallableContact) ?? null;
                    const everyHighPriority = bucketCalls.every(
                      (call) => call.priority === "high"
                    );
                    const collapsed = collapsedFollowUpBuckets[bucket];

                    return (
                      <section
                        key={`follow-up-group-${bucket}`}
                        className={`follow-up-group ${bucket}`}
                      >
                        <div className="follow-up-group-header">
                          <div>
                            <p className="follow-up-group-title">
                              {followUpBucketCopy[bucket].label}
                            </p>
                            <p className="follow-up-group-copy">
                              {followUpBucketCopy[bucket].detail}
                            </p>
                            <p className="follow-up-group-meta">
                              Lead: {bucketLead.remoteIdentity} /{" "}
                              {formatFollowUpTiming(bucketLead.followUpAt, now)}
                            </p>
                          </div>
                          <div className="follow-up-group-actions">
                            <span className="section-badge">
                              {bucketCalls.length}/{totalBucketCalls.length}
                            </span>
                            <button
                              type="button"
                              className="ghost mini-button"
                              onClick={() =>
                                actionableBucketLead && handleQueueCallNow(actionableBucketLead)
                              }
                              disabled={!actionableBucketLead}
                            >
                              Focus first
                            </button>
                            <button
                              type="button"
                              className="ghost mini-button"
                              onClick={() =>
                                handleBulkSchedule(bucketCalls, scheduleInMinutes(now, 60))
                              }
                              disabled={!bucketCalls.some(isCallableContact)}
                            >
                              Push +1h
                            </button>
                            <button
                              type="button"
                              className="ghost mini-button"
                              onClick={() =>
                                handleBulkSchedule(bucketCalls, tomorrowAt(now, 10, 0))
                              }
                              disabled={!bucketCalls.some(isCallableContact)}
                            >
                              Tomorrow 10a
                            </button>
                            <button
                              type="button"
                              className="ghost mini-button"
                              onClick={() =>
                                handleBulkQueuePriority(
                                  bucketCalls,
                                  everyHighPriority ? "normal" : "high"
                                )
                              }
                            >
                              {everyHighPriority ? "Normal all" : "High all"}
                            </button>
                            <button
                              type="button"
                              className="ghost mini-button"
                              onClick={() => toggleFollowUpBucket(bucket)}
                            >
                              {collapsed ? "Expand" : "Collapse"}
                            </button>
                          </div>
                        </div>

                        {!collapsed ? (
                          <div className="follow-up-list">
                            {bucketCalls.map((call) => {
                              const state = followUpState(call, now);
                              return (
                                <article
                                  key={`follow-up-${call.id}`}
                                  className={`follow-up-item ${state}${
                                    selectedRecentCallId === call.id ? " selected" : ""
                                  }`}
                                >
                                  <div className="follow-up-main">
                                    <div>
                                      <p className="follow-up-identity">{call.remoteIdentity}</p>
                                      <p className="follow-up-meta">
                                        {call.disposition
                                          ? formatDisposition(call.disposition)
                                          : "Needs review"}{" "}
                                        / {formatFollowUpTiming(call.followUpAt, now)}
                                      </p>
                                    </div>
                                    <div className="follow-up-badges">
                                      {call.priority === "high" ? (
                                        <span className="status-tag priority">High priority</span>
                                      ) : null}
                                      {call.handoffType ? (
                                        <span className={`status-tag handoff ${call.handoffType}`}>
                                          {formatHandoffType(call.handoffType)}
                                        </span>
                                      ) : null}
                                      {call.handoffType ? (
                                        <span className={`status-tag handoff-status ${call.handoffStatus ?? "open"}`}>
                                          {formatHandoffStatus(call.handoffStatus ?? "open")}
                                        </span>
                                      ) : null}
                                      {(call.contactFlags ?? []).map((flag) => (
                                        <span
                                          key={`${call.id}-${flag}`}
                                          className={`status-tag contact-flag ${flag}`}
                                        >
                                          {formatContactFlag(flag)}
                                        </span>
                                      ))}
                                      {call.disposition ? (
                                        <span className="status-tag wrap-up">
                                          {formatDisposition(call.disposition)}
                                        </span>
                                      ) : null}
                                    </div>
                                  </div>
                                  <p className="follow-up-note">
                                    {call.notes ||
                                      "No follow-up note saved yet. Open wrap-up to add context."}
                                  </p>
                                  <div className="follow-up-presets">
                                    <span className={`status-tag queue-state ${state}`}>
                                      {followUpStateLabel(state)}
                                    </span>
                                    <button
                                      type="button"
                                      className="ghost mini-button"
                                      onClick={() =>
                                        handleQuickSchedule(call, scheduleInMinutes(now, 15))
                                      }
                                      disabled={!isCallableContact(call)}
                                    >
                                      +15m
                                    </button>
                                    <button
                                      type="button"
                                      className="ghost mini-button"
                                      onClick={() =>
                                        handleQuickSchedule(call, scheduleInMinutes(now, 60))
                                      }
                                      disabled={!isCallableContact(call)}
                                    >
                                      +1h
                                    </button>
                                    <button
                                      type="button"
                                      className="ghost mini-button"
                                      onClick={() =>
                                        handleQuickSchedule(call, tomorrowAt(now, 10, 0))
                                      }
                                      disabled={!isCallableContact(call)}
                                    >
                                      Tomorrow 10a
                                    </button>
                                    <button
                                      type="button"
                                      className="ghost mini-button"
                                      onClick={() =>
                                        handleQueuePriority(
                                          call,
                                          call.priority === "high" ? "normal" : "high"
                                        )
                                      }
                                    >
                                      {call.priority === "high" ? "Normal" : "High"} priority
                                    </button>
                                    <button
                                      type="button"
                                      className="ghost mini-button"
                                      onClick={() => handleClearQueueSchedule(call)}
                                      disabled={!call.followUpAt && !call.priority}
                                    >
                                      Clear due
                                    </button>
                                  </div>
                                  <div className="follow-up-actions">
                                    <button
                                      type="button"
                                      className="mini-button"
                                      onClick={() => handleQueueCallNow(call)}
                                      disabled={!isCallableContact(call)}
                                    >
                                      {isCallableContact(call) ? "Call now" : "Dial blocked"}
                                    </button>
                                    <button
                                      type="button"
                                      className="ghost mini-button"
                                      onClick={() => handleResolveFollowUp(call)}
                                    >
                                      Done
                                    </button>
                                    <button
                                      type="button"
                                      className="ghost mini-button"
                                      onClick={() => setSelectedRecentCallId(call.id)}
                                    >
                                      Review
                                    </button>
                                  </div>
                                </article>
                              );
                            })}
                          </div>
                        ) : (
                          <p className="follow-up-group-collapsed">
                            {bucketCalls.length} calls hidden in this lane.
                          </p>
                        )}
                      </section>
                    );
                  })}
                </div>
              )}
            </section>

            <section className="panel">
              <div className="section-head">
                <div>
                  <h2>Wrap-Up</h2>
                  <p className="panel-copy">
                    Tag the selected conversation before moving to the next dial.
                  </p>
                </div>
                {selectedRecentCall ? (
                  <span className="section-badge">{selectedRecentCall.direction.toUpperCase()}</span>
                ) : null}
              </div>

              {!selectedRecentCall ? (
                <p className="empty-state">
                  Finish a call to capture disposition notes and follow-up context here.
                </p>
              ) : (
                <>
                  <section className="contact-panel">
                    <div className="section-head">
                      <div>
                        <h2>Contact Lens</h2>
                        <p className="panel-copy">
                          Recent history for this number stays visible while you work the next step.
                        </p>
                      </div>
                      <span className="section-badge">{selectedContactAttemptCount}</span>
                    </div>

                    <div className="contact-grid">
                      <article className="contact-card">
                        <p className="contact-label">Attempts</p>
                        <p className="contact-value">{selectedContactAttemptCount}</p>
                      </article>
                      <article className="contact-card">
                        <p className="contact-label">Connected</p>
                        <p className="contact-value">{selectedContactConnectedCount}</p>
                      </article>
                      <article className="contact-card">
                        <p className="contact-label">Failed</p>
                        <p className="contact-value">{selectedContactFailedCount}</p>
                      </article>
                      <article className="contact-card">
                        <p className="contact-label">Next follow-up</p>
                        <p className="contact-value compact">
                          {selectedContactNextFollowUp
                            ? formatFollowUpTiming(selectedContactNextFollowUp.followUpAt, now)
                            : "None"}
                        </p>
                      </article>
                    </div>

                    <section className="policy-panel">
                      <div className="section-head">
                        <div>
                          <h2>Contact Policy</h2>
                          <p className="panel-copy">
                            Shared flags stay attached to every saved attempt for this number.
                          </p>
                        </div>
                        <div className="policy-tags">
                          {selectedContactFlags.length > 0 ? (
                            selectedContactFlags.map((flag) => (
                              <span
                                key={`selected-contact-flag-${flag}`}
                                className={`status-tag contact-flag ${flag}`}
                              >
                                {formatContactFlag(flag)}
                              </span>
                            ))
                          ) : (
                            <span className="status-tag queue-state unscheduled">No flags</span>
                          )}
                        </div>
                      </div>

                      <div className="policy-grid">
                        {contactFlagOptions.map((option) => {
                          const selected = selectedContactFlags.includes(option.value);
                          return (
                            <button
                              key={option.value}
                              type="button"
                              className={`policy-chip${selected ? " selected" : ""}`}
                              onClick={() => handleToggleContactFlag(option.value)}
                            >
                              <span className="policy-chip-title">{option.label}</span>
                              <span className="policy-chip-copy">{option.detail}</span>
                            </button>
                          );
                        })}
                      </div>

                      {selectedContactFlags.includes("do_not_call") ? (
                        <p className="policy-warning">
                          Do Not Call is active, so dial actions stay blocked until the flag is cleared.
                        </p>
                      ) : null}
                    </section>

                    <section className="handoff-panel">
                      <div className="section-head">
                        <div>
                          <h2>Contact Handoff</h2>
                          <p className="panel-copy">
                            Escalate this number into a supervisor or review queue with a short handoff note.
                          </p>
                        </div>
                        {selectedContactHandoffType ? (
                          <div className="handoff-badges">
                            <span className={`status-tag handoff ${selectedContactHandoffType}`}>
                              {formatHandoffType(selectedContactHandoffType)}
                            </span>
                            <span className={`status-tag handoff-status ${selectedContactHandoffStatus}`}>
                              {formatHandoffStatus(selectedContactHandoffStatus)}
                            </span>
                            {selectedContactHandoffDueAt && selectedContactHandoffStatus !== "resolved" ? (
                              <span
                                className={`status-tag handoff-due ${
                                  selectedContactHandoffDueAt < now
                                    ? "overdue"
                                    : isSameLocalDay(selectedContactHandoffDueAt, now)
                                      ? "due-today"
                                      : "upcoming"
                                }`}
                              >
                                {formatHandoffDueTiming(selectedContactHandoffDueAt, now)}
                              </span>
                            ) : null}
                          </div>
                        ) : (
                          <span className="status-tag queue-state unscheduled">No handoff</span>
                        )}
                      </div>

                      <div className="handoff-type-grid">
                        {handoffTypeOptions.map((option) => (
                          <button
                            key={option.value}
                            type="button"
                            className={`policy-chip${draftHandoffType === option.value ? " selected" : ""}`}
                            onClick={() => setDraftHandoffType(option.value)}
                          >
                            <span className="policy-chip-title">{option.label}</span>
                            <span className="policy-chip-copy">{option.detail}</span>
                          </button>
                        ))}
                      </div>

                      <div className="handoff-status-row">
                        <div className="priority-chips">
                          {handoffStatusOptions.map((option) => (
                            <button
                              key={option.value}
                              type="button"
                              className={`chip-button${draftHandoffStatus === option.value ? " selected" : ""}`}
                              onClick={() => setDraftHandoffStatus(option.value)}
                            >
                              {option.label}
                            </button>
                          ))}
                        </div>
                        <label className="handoff-owner-field">
                          Handoff owner
                          <input
                            placeholder="Supervisor A or Agent 1001"
                            value={draftHandoffOwner}
                            onChange={(event) => setDraftHandoffOwner(event.target.value)}
                          />
                        </label>
                      </div>

                      <label>
                        Handoff due
                        <input
                          type="datetime-local"
                          value={draftHandoffDueAt}
                          onChange={(event) => setDraftHandoffDueAt(event.target.value)}
                        />
                      </label>

                      <div className="schedule-presets">
                        <button
                          type="button"
                          className="ghost mini-button"
                          onClick={() =>
                            setDraftHandoffDueAt(
                              formatDateTimeLocalValue(scheduleInMinutes(now, 30))
                            )
                          }
                        >
                          Due 30m
                        </button>
                        <button
                          type="button"
                          className="ghost mini-button"
                          onClick={() =>
                            setDraftHandoffDueAt(
                              formatDateTimeLocalValue(scheduleInMinutes(now, 120))
                            )
                          }
                        >
                          Due 2h
                        </button>
                        <button
                          type="button"
                          className="ghost mini-button"
                          onClick={() =>
                            setDraftHandoffDueAt(
                              formatDateTimeLocalValue(tomorrowAt(now, 10, 0))
                            )
                          }
                        >
                          Tomorrow 10a
                        </button>
                        <button
                          type="button"
                          className="ghost mini-button"
                          onClick={() => setDraftHandoffDueAt("")}
                          disabled={!draftHandoffDueAt}
                        >
                          Clear due
                        </button>
                      </div>

                      <label>
                        Handoff note
                        <textarea
                          placeholder="Why does this contact need a handoff?"
                          value={draftHandoffNote}
                          onChange={(event) => setDraftHandoffNote(event.target.value)}
                        />
                      </label>

                      <div className="button-row">
                        <button
                          type="button"
                          onClick={handleSaveHandoff}
                          disabled={!hasHandoffChanges}
                        >
                          Save handoff
                        </button>
                        <button
                          type="button"
                          className="ghost"
                          onClick={() => {
                            setDraftHandoffType(selectedContactHandoffType ?? "");
                            setDraftHandoffStatus(selectedContactHandoffStatus);
                            setDraftHandoffOwner(selectedContactHandoffOwner);
                            setDraftHandoffDueAt(formatDateTimeLocalValue(selectedContactHandoffDueAt));
                            setDraftHandoffNote(selectedContactHandoffNote);
                          }}
                          disabled={!hasHandoffChanges}
                        >
                          Reset handoff
                        </button>
                        <button
                          type="button"
                          className="ghost"
                          onClick={() =>
                            selectedRecentCall && selectedContactHandoffType
                              ? handleResolveHandoff(
                                  selectedRecentCall.remoteIdentity,
                                  selectedContactHandoffType,
                                  draftHandoffNote.trim() || selectedContactHandoffNote || undefined,
                                  draftHandoffOwner.trim() || selectedContactHandoffOwner || undefined,
                                  parseDateTimeLocalValue(draftHandoffDueAt) ?? selectedContactHandoffDueAt
                                )
                              : undefined
                          }
                          disabled={!selectedRecentCall || !selectedContactHandoffType}
                        >
                          Resolve handoff
                        </button>
                        <button
                          type="button"
                          className="ghost"
                          onClick={() => handleClearHandoff()}
                          disabled={!selectedContactHandoffType && !selectedContactHandoffNote}
                        >
                          Clear handoff
                        </button>
                      </div>

                      {selectedContactHandoffUpdatedAt ? (
                        <p className="handoff-meta-line">
                          Last updated {formatDateTime(selectedContactHandoffUpdatedAt)}
                          {selectedContactHandoffOwner ? ` / ${selectedContactHandoffOwner}` : ""}
                          {selectedContactHandoffDueAt
                            ? ` / ${formatHandoffDueTiming(selectedContactHandoffDueAt, now)}`
                            : ""}
                        </p>
                      ) : null}

                      {selectedHandoffBrief ? (
                        <article className="brief-panel">
                          <div className="section-head">
                            <div>
                              <h2>Handoff Brief</h2>
                              <p className="panel-copy">
                                Copy this summary into supervisor chat, ticket notes, or a follow-up handoff.
                              </p>
                            </div>
                            <button
                              type="button"
                              className="ghost mini-button"
                              onClick={() => void handleCopyBrief("selected-handoff", selectedHandoffBrief)}
                            >
                              Copy brief
                            </button>
                          </div>
                          <pre className="brief-preview">{selectedHandoffBrief}</pre>
                          {briefFeedback?.key === "selected-handoff" ? (
                            <p className={`brief-feedback ${briefFeedback.tone}`}>
                              {briefFeedback.message}
                            </p>
                          ) : null}
                        </article>
                      ) : null}
                    </section>

                    {selectedContactHistory.length > 1 ? (
                      <div className="contact-timeline">
                        {selectedContactHistory.slice(0, 4).map((call) => (
                          <article
                            key={`contact-${call.id}-${call.endedAt ?? call.startedAt ?? 0}`}
                            className="contact-event"
                          >
                            <div className="contact-event-main">
                              <p className="contact-event-title">
                                {call.disposition
                                  ? formatDisposition(call.disposition)
                                  : formatCallResult(call.status)}
                              </p>
                              <p className="contact-event-meta">
                                {formatDateTime(call.endedAt ?? call.startedAt ?? now)}
                              </p>
                            </div>
                            {call.notes ? (
                              <p className="contact-event-note">{call.notes}</p>
                            ) : (
                              <p className="contact-event-note empty">
                                No saved notes on this attempt.
                              </p>
                            )}
                          </article>
                        ))}
                      </div>
                    ) : (
                      <p className="empty-state">
                        This is the first saved attempt for the selected number.
                      </p>
                    )}

                    {contactRecommendation ? (
                      <article className={`recommendation-card ${contactRecommendation.tone}`}>
                        <div className="recommendation-main">
                          <p className="recommendation-title">{contactRecommendation.title}</p>
                          <p className="recommendation-detail">{contactRecommendation.detail}</p>
                        </div>
                        <div className="recommendation-actions">
                          <button
                            type="button"
                            className="mini-button"
                            onClick={() =>
                              handleRecommendationAction(contactRecommendation.primaryAction)
                            }
                          >
                            {contactRecommendation.primaryLabel}
                          </button>
                          {contactRecommendation.secondaryAction &&
                          contactRecommendation.secondaryLabel ? (
                            <button
                              type="button"
                              className="ghost mini-button"
                              onClick={() =>
                                handleRecommendationAction(
                                  contactRecommendation.secondaryAction as RecommendationAction
                                )
                              }
                            >
                              {contactRecommendation.secondaryLabel}
                            </button>
                          ) : null}
                        </div>
                      </article>
                    ) : null}
                  </section>

                  <div className="wrap-up-summary">
                    <p className="wrap-up-number">{selectedRecentCall.remoteIdentity}</p>
                    <div className="wrap-up-metrics">
                      <span>{formatCallResult(selectedRecentCall.status)}</span>
                      <span>{formatCompletedDuration(selectedRecentCall)}</span>
                      <span>
                        {formatTimestamp(
                          selectedRecentCall.endedAt ??
                            selectedRecentCall.startedAt ??
                            Date.now()
                        )}
                      </span>
                    </div>
                  </div>

                  <div className="disposition-grid">
                    {dispositionOptions.map((option) => (
                      <button
                        key={option.value}
                        type="button"
                        className={`chip-button${draftDisposition === option.value ? " selected" : ""}`}
                        onClick={() => setDraftDisposition(option.value)}
                      >
                        {option.label}
                      </button>
                    ))}
                  </div>

                  {scheduleActive ? (
                    <div className="schedule-grid">
                      <label>
                        Follow-up time
                        <input
                          type="datetime-local"
                          value={draftFollowUpAt}
                          onChange={(event) => setDraftFollowUpAt(event.target.value)}
                        />
                      </label>
                      <div className="priority-panel">
                        <p className="schedule-title">Priority</p>
                        <div className="priority-chips">
                          {priorityOptions.map((option) => (
                            <button
                              key={option.value}
                              type="button"
                              className={`chip-button${draftPriority === option.value ? " selected" : ""}`}
                              onClick={() => setDraftPriority(option.value)}
                            >
                              {option.label}
                            </button>
                          ))}
                        </div>
                        <div className="schedule-presets">
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() =>
                              setDraftFollowUpAt(
                                formatDateTimeLocalValue(scheduleInMinutes(now, 15))
                              )
                            }
                          >
                            In 15m
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() =>
                              setDraftFollowUpAt(
                                formatDateTimeLocalValue(scheduleInMinutes(now, 60))
                              )
                            }
                          >
                            In 1h
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() =>
                              setDraftFollowUpAt(
                                formatDateTimeLocalValue(tomorrowAt(now, 10, 0))
                              )
                            }
                          >
                            Tomorrow 10a
                          </button>
                          <button
                            type="button"
                            className="ghost mini-button"
                            onClick={() => setDraftFollowUpAt("")}
                            disabled={!draftFollowUpAt}
                          >
                            Clear time
                          </button>
                        </div>
                        <p className="schedule-copy">
                          {parsedDraftFollowUpAt
                            ? `${draftPriority === "high" ? "High" : "Normal"} priority for ${formatFollowUpTiming(
                                parsedDraftFollowUpAt,
                                now
                              )}.`
                            : "No time set yet. It will stay in the queue without a due slot."}
                        </p>
                      </div>
                    </div>
                  ) : null}

                  <label>
                    Call notes
                    <textarea
                      rows={5}
                      placeholder="Add callback timing, objections, or next-step context."
                      value={draftNotes}
                      onChange={(event) => setDraftNotes(event.target.value)}
                    />
                  </label>

                  <div className="button-row">
                    <button type="button" onClick={handleSaveWrapUp} disabled={!hasWrapUpChanges}>
                      Save Wrap-Up
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => loadNumber(selectedRecentCall.remoteIdentity)}
                    >
                      Load Number
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => setDraftDisposition("")}
                      disabled={!draftDisposition}
                    >
                      Clear Tag
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      onClick={handleResetWrapUp}
                      disabled={!hasWrapUpChanges}
                    >
                      Reset
                    </button>
                  </div>
                  {!scheduleActive && (selectedRecentCall?.followUpAt || selectedRecentCall?.priority) ? (
                    <p className="schedule-copy">
                      Saving a non-follow-up disposition will clear the existing callback schedule.
                    </p>
                  ) : null}
                </>
              )}
            </section>

            <section className="panel">
              <div className="section-head">
                <div>
                  <h2>Activity Feed</h2>
                  <p className="panel-copy">
                    Registration, call events, and wrap-up saves are tracked in the client state.
                  </p>
                </div>
                <div className="section-actions">
                  <button
                    type="button"
                    className="ghost mini-button"
                    onClick={() => client.clearActivity()}
                    disabled={snapshot.activity.length === 0}
                  >
                    Clear feed
                  </button>
                  <span className="section-badge">{snapshot.activity.length}</span>
                </div>
              </div>

              {snapshot.activity.length === 0 ? (
                <p className="empty-state">Connection and call activity will stream in here.</p>
              ) : (
                <div className="activity-list">
                  {snapshot.activity.map((item) => (
                    <article key={item.id} className={`activity-item ${item.level}`}>
                      <div className="activity-main">
                        <p className="activity-title">{item.title}</p>
                        {item.detail ? <p className="activity-detail">{item.detail}</p> : null}
                      </div>
                      <div className="activity-meta">
                        <span className="activity-tone">{activityToneLabel(item)}</span>
                        <span className="activity-time">{formatTimestamp(item.timestamp)}</span>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </div>
        </section>
      </main>
    </div>
  );
}
