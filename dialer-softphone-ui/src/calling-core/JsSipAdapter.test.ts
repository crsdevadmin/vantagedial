import { beforeEach, describe, expect, it, vi } from "vitest";
import { JsSipAdapter } from "./JsSipAdapter";
import type { SoftphoneSnapshot } from "./types";

type Handler = (...args: any[]) => void;
const mockState = vi.hoisted(() => ({
  uaInstances: [] as FakeUAShape[]
}));

type FakeUAShape = {
  handlers: Map<string, Handler[]>;
  nextCallSession: FakeSession | null;
  on(event: string, handler: Handler): void;
  emit(event: string, payload?: unknown): void;
  start(): void;
  stop(): void;
  call(): FakeSession;
};

class FakeSession {
  id: string;
  remote_identity: { display_name: string };
  private handlers = new Map<string, Handler[]>();

  constructor(id: string, remoteIdentity: string) {
    this.id = id;
    this.remote_identity = { display_name: remoteIdentity };
  }

  on(event: string, handler: Handler): void {
    const nextHandlers = this.handlers.get(event) ?? [];
    nextHandlers.push(handler);
    this.handlers.set(event, nextHandlers);
  }

  emit(event: string, payload?: unknown): void {
    for (const handler of this.handlers.get(event) ?? []) {
      handler(payload);
    }
  }

  answer(): void {}

  terminate(): void {}

  mute(): void {}

  unmute(): void {}

  hold(): void {}

  unhold(): void {}
}

vi.mock("jssip", () => {
  class FakeUA implements FakeUAShape {
    handlers = new Map<string, Handler[]>();
    nextCallSession: FakeSession | null = null;

    on(event: string, handler: Handler): void {
      const nextHandlers = this.handlers.get(event) ?? [];
      nextHandlers.push(handler);
      this.handlers.set(event, nextHandlers);
    }

    emit(event: string, payload?: unknown): void {
      for (const handler of this.handlers.get(event) ?? []) {
        handler(payload);
      }
    }

    start(): void {}

    stop(): void {}

    call(): FakeSession {
      if (!this.nextCallSession) {
        throw new Error("No fake session configured");
      }
      return this.nextCallSession;
    }
  }

  return {
    default: {
      WebSocketInterface: class FakeWebSocketInterface {
        constructor(_url: string) {}
      },
      UA: class FakeJsSipUA extends FakeUA {
        constructor(_config: unknown) {
          super();
          mockState.uaInstances.push(this);
        }
      }
    }
  };
});

describe("JsSipAdapter", () => {
  beforeEach(() => {
    mockState.uaInstances.length = 0;
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-03-27T10:00:00Z"));
  });

  it("keeps a newer call active when the previous call's delayed cleanup fires", async () => {
    const adapter = new JsSipAdapter();
    const patches: Array<Partial<SoftphoneSnapshot>> = [];

    await adapter.connect(
      {
        sipServer: "asterisk.example.com",
        websocketUrl: "wss://asterisk.example.com/ws",
        username: "1001",
        password: "secret"
      },
      (patch) => patches.push(patch)
    );

    const ua = mockState.uaInstances[mockState.uaInstances.length - 1];
    expect(ua).toBeDefined();

    const firstSession = new FakeSession("call-1", "First Contact");
    ua!.nextCallSession = firstSession;
    await adapter.dial("1002");

    firstSession.emit("accepted");
    firstSession.emit("ended");

    const secondSession = new FakeSession("call-2", "Second Contact");
    ua!.emit("newRTCSession", {
      originator: "remote",
      session: secondSession
    });

    vi.advanceTimersByTime(500);

    expect(patches[patches.length - 1]?.currentCall).toMatchObject({
      id: "call-2",
      remoteIdentity: "Second Contact",
      status: "ringing"
    });
  });

  it("ignores duplicate attachment attempts for the same session", async () => {
    const adapter = new JsSipAdapter();
    const patches: Array<Partial<SoftphoneSnapshot>> = [];

    await adapter.connect(
      {
        sipServer: "asterisk.example.com",
        websocketUrl: "wss://asterisk.example.com/ws",
        username: "1001",
        password: "secret"
      },
      (patch) => patches.push(patch)
    );

    const ua = mockState.uaInstances[mockState.uaInstances.length - 1];
    expect(ua).toBeDefined();

    const session = new FakeSession("call-3", "Repeat Contact");
    ua!.nextCallSession = session;
    await adapter.dial("1003");

    const publishCountBeforeDuplicate = patches.length;
    ua!.emit("newRTCSession", {
      originator: "local",
      session
    });

    expect(patches).toHaveLength(publishCountBeforeDuplicate);
  });
});
