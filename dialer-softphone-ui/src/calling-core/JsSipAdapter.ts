import JsSIP from "jssip";
import {
  CallSession,
  SipAdapter,
  SoftphoneConfig,
  SoftphoneSnapshot
} from "./types";

type PublishFn = (snapshot: Partial<SoftphoneSnapshot>) => void;

export class JsSipAdapter implements SipAdapter {
  private publish: PublishFn | null = null;
  private ua: any | null = null;
  private session: any | null = null;
  private currentCall: CallSession | null = null;

  async connect(config: SoftphoneConfig, publish: PublishFn): Promise<void> {
    if (!config.websocketUrl) {
      throw new Error("websocketUrl is required for JsSIP mode");
    }

    this.publish = publish;
    publish({ registrationState: "registering", lastError: null });

    const socket = new JsSIP.WebSocketInterface(config.websocketUrl);
    const ua = new JsSIP.UA({
      sockets: [socket],
      uri: `sip:${config.username}@${config.sipServer}`,
      password: config.password,
      display_name: config.displayName ?? config.username,
      session_timers: false,
      register: true
    });

    ua.on("registered", () => {
      publish({ registrationState: "registered", lastError: null });
    });

    ua.on("registrationFailed", (event: any) => {
      publish({
        registrationState: "registration_failed",
        lastError: event?.cause ?? "SIP registration failed"
      });
    });

    ua.on("disconnected", () => {
      publish({ registrationState: "offline" });
    });

    ua.on("newRTCSession", (event: any) => {
      const rtcSession = event.session;
      this.attachSession(rtcSession, event.originator === "remote" ? "incoming" : "outgoing");
    });

    ua.start();
    this.ua = ua;
  }

  async disconnect(): Promise<void> {
    try {
      this.session?.terminate();
    } catch {
      // Ignore local cleanup failures.
    }
    this.session = null;
    this.currentCall = null;
    if (this.ua) {
      this.ua.stop();
      this.ua = null;
    }
    this.publish?.({
      registrationState: "offline",
      currentCall: null
    });
  }

  async dial(destination: string): Promise<void> {
    if (!this.ua) {
      throw new Error("Softphone is not connected");
    }
    const target = destination.startsWith("sip:") ? destination : `sip:${destination}`;
    const rtcSession = this.ua.call(target, {
      mediaConstraints: { audio: true, video: false }
    });
    this.attachSession(rtcSession, "outgoing");
  }

  async answer(): Promise<void> {
    if (!this.session) {
      return;
    }
    this.session.answer({
      mediaConstraints: { audio: true, video: false }
    });
  }

  async reject(): Promise<void> {
    if (!this.session) {
      return;
    }
    this.session.terminate({ status_code: 486, reason_phrase: "Busy Here" });
  }

  async hangup(): Promise<void> {
    this.session?.terminate();
  }

  async mute(nextMuted: boolean): Promise<void> {
    if (!this.session) {
      return;
    }
    if (nextMuted) {
      this.session.mute({ audio: true });
    } else {
      this.session.unmute({ audio: true });
    }
    this.updateCurrentCall({ muted: nextMuted });
  }

  async hold(nextHeld: boolean): Promise<void> {
    if (!this.session) {
      return;
    }
    if (nextHeld) {
      this.session.hold();
      this.updateCurrentCall({ held: true, status: "held" });
    } else {
      this.session.unhold();
      this.updateCurrentCall({ held: false, status: "in_call" });
    }
  }

  private attachSession(session: any, direction: "incoming" | "outgoing"): void {
    this.session = session;
    this.currentCall = {
      id: session.id ?? crypto.randomUUID(),
      remoteIdentity: this.readRemoteIdentity(session),
      direction,
      status: direction === "incoming" ? "ringing" : "dialing",
      muted: false,
      held: false
    };
    this.publish?.({ currentCall: this.currentCall, lastError: null });

    session.on("progress", () => {
      this.updateCurrentCall({
        status: direction === "incoming" ? "ringing" : "dialing"
      });
    });

    session.on("accepted", () => {
      this.updateCurrentCall({
        status: "in_call",
        startedAt: Date.now()
      });
    });

    session.on("confirmed", () => {
      this.updateCurrentCall({
        status: "in_call",
        startedAt: this.currentCall?.startedAt ?? Date.now()
      });
    });

    session.on("ended", () => {
      this.finishCurrentCall("ended");
    });

    session.on("failed", (event: any) => {
      const cause = event?.cause ?? "Call failed";
      this.finishCurrentCall("failed", cause);
    });

    session.on("hold", () => {
      this.updateCurrentCall({ held: true, status: "held" });
    });

    session.on("unhold", () => {
      this.updateCurrentCall({ held: false, status: "in_call" });
    });

    session.on("muted", () => {
      this.updateCurrentCall({ muted: true });
    });

    session.on("unmuted", () => {
      this.updateCurrentCall({ muted: false });
    });
  }

  private finishCurrentCall(status: "ended" | "failed", errorMessage?: string): void {
    if (this.currentCall) {
      this.currentCall = {
        ...this.currentCall,
        status,
        endedAt: Date.now()
      };
    }
    this.publish?.({
      currentCall: this.currentCall
    });

    window.setTimeout(() => {
      this.session = null;
      this.currentCall = null;
      this.publish?.({
        currentCall: null,
        lastError: errorMessage ?? null
      });
    }, 500);
  }

  private updateCurrentCall(patch: Partial<CallSession>): void {
    if (!this.currentCall) {
      return;
    }
    this.currentCall = { ...this.currentCall, ...patch };
    this.publish?.({ currentCall: this.currentCall });
  }

  private readRemoteIdentity(session: any): string {
    return (
      session?.remote_identity?.display_name ||
      session?.remote_identity?.uri?.user ||
      session?.remote_identity?._uri?._user ||
      "Unknown"
    );
  }
}
