export type RegistrationState =
  | "offline"
  | "registering"
  | "registered"
  | "registration_failed";

export type CallDirection = "incoming" | "outgoing";

export type CallStatus =
  | "idle"
  | "dialing"
  | "ringing"
  | "in_call"
  | "held"
  | "ended"
  | "failed";

export type CallDisposition =
  | "connected"
  | "callback"
  | "voicemail"
  | "no_answer"
  | "wrong_number";

export type CallPriority = "normal" | "high";

export type ContactFlag = "vip" | "verified" | "do_not_call";

export type ContactHandoffType = "supervisor" | "verification" | "compliance";

export type ContactHandoffStatus = "open" | "claimed" | "resolved";

export type SoftphoneConfig = {
  sipServer: string;
  websocketUrl?: string;
  username: string;
  password: string;
  displayName?: string;
};

export type CallSession = {
  id: string;
  remoteIdentity: string;
  direction: CallDirection;
  status: CallStatus;
  startedAt?: number;
  endedAt?: number;
  muted: boolean;
  held: boolean;
  disposition?: CallDisposition;
  notes?: string;
  priority?: CallPriority;
  followUpAt?: number;
  contactFlags?: ContactFlag[];
  handoffType?: ContactHandoffType;
  handoffStatus?: ContactHandoffStatus;
  handoffOwner?: string;
  handoffNote?: string;
  handoffDueAt?: number;
  handoffUpdatedAt?: number;
};

export type SoftphoneActivityLevel = "info" | "success" | "warning" | "error";

export type SoftphoneActivityItem = {
  id: string;
  timestamp: number;
  level: SoftphoneActivityLevel;
  title: string;
  detail?: string;
};

export type SoftphoneSnapshot = {
  registrationState: RegistrationState;
  currentCall: CallSession | null;
  recentCalls: CallSession[];
  activity: SoftphoneActivityItem[];
  lastError: string | null;
};

export type SoftphonePersistedState = Pick<SoftphoneSnapshot, "recentCalls" | "activity">;

export type SoftphoneListener = (snapshot: SoftphoneSnapshot) => void;

export interface SipAdapter {
  connect(config: SoftphoneConfig, publish: (snapshot: Partial<SoftphoneSnapshot>) => void): Promise<void>;
  disconnect(): Promise<void>;
  dial(destination: string): Promise<void>;
  answer(): Promise<void>;
  reject(): Promise<void>;
  hangup(): Promise<void>;
  mute(nextMuted: boolean): Promise<void>;
  hold(nextHeld: boolean): Promise<void>;
}
