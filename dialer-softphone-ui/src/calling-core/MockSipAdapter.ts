import {
  CallSession,
  SipAdapter,
  SoftphoneConfig,
  SoftphoneSnapshot
} from "./types";

export class MockSipAdapter implements SipAdapter {
  private publish: ((snapshot: Partial<SoftphoneSnapshot>) => void) | null = null;
  private currentCall: CallSession | null = null;
  private incomingTimer: number | null = null;

  async connect(_config: SoftphoneConfig, publish: (snapshot: Partial<SoftphoneSnapshot>) => void): Promise<void> {
    this.publish = publish;
    publish({ registrationState: "registering", lastError: null });

    window.setTimeout(() => {
      publish({ registrationState: "registered" });
      this.scheduleIncomingDemoCall();
    }, 700);
  }

  async disconnect(): Promise<void> {
    if (this.incomingTimer) {
      window.clearTimeout(this.incomingTimer);
      this.incomingTimer = null;
    }
    this.currentCall = null;
    this.publish?.({
      registrationState: "offline",
      currentCall: null
    });
  }

  async dial(destination: string): Promise<void> {
    const call: CallSession = {
      id: crypto.randomUUID(),
      remoteIdentity: destination,
      direction: "outgoing",
      status: "dialing",
      muted: false,
      held: false
    };

    this.currentCall = call;
    this.publish?.({ currentCall: call, lastError: null });

    window.setTimeout(() => {
      if (!this.currentCall || this.currentCall.id !== call.id) {
        return;
      }
      this.currentCall = { ...call, status: "in_call", startedAt: Date.now() };
      this.publish?.({ currentCall: this.currentCall });
    }, 1500);
  }

  async answer(): Promise<void> {
    if (!this.currentCall) {
      return;
    }
    this.currentCall = {
      ...this.currentCall,
      status: "in_call",
      startedAt: Date.now()
    };
    this.publish?.({ currentCall: this.currentCall });
  }

  async reject(): Promise<void> {
    await this.finishCall("ended");
  }

  async hangup(): Promise<void> {
    await this.finishCall("ended");
  }

  async mute(nextMuted: boolean): Promise<void> {
    if (!this.currentCall) {
      return;
    }
    this.currentCall = { ...this.currentCall, muted: nextMuted };
    this.publish?.({ currentCall: this.currentCall });
  }

  async hold(nextHeld: boolean): Promise<void> {
    if (!this.currentCall) {
      return;
    }
    this.currentCall = {
      ...this.currentCall,
      held: nextHeld,
      status: nextHeld ? "held" : "in_call"
    };
    this.publish?.({ currentCall: this.currentCall });
  }

  private async finishCall(status: "ended" | "failed"): Promise<void> {
    if (!this.currentCall) {
      return;
    }
    this.currentCall = {
      ...this.currentCall,
      status,
      endedAt: Date.now()
    };
    this.publish?.({ currentCall: this.currentCall });
    window.setTimeout(() => {
      this.currentCall = null;
      this.publish?.({ currentCall: null });
      this.scheduleIncomingDemoCall();
    }, 900);
  }

  private scheduleIncomingDemoCall(): void {
    if (this.incomingTimer) {
      window.clearTimeout(this.incomingTimer);
    }
    this.incomingTimer = window.setTimeout(() => {
      if (this.currentCall) {
        return;
      }
      this.currentCall = {
        id: crypto.randomUUID(),
        remoteIdentity: "+91 98406 05775",
        direction: "incoming",
        status: "ringing",
        muted: false,
        held: false
      };
      this.publish?.({ currentCall: this.currentCall });
    }, 12000);
  }
}
