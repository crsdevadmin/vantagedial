import { describe, expect, it } from "vitest";
import {
  customerConfigUrl,
  normalizeApiBaseUrl,
  toSoftphoneCustomerSettings
} from "./customerConfig";

describe("customerConfig", () => {
  it("normalizes API base URLs before building customer config URLs", () => {
    expect(normalizeApiBaseUrl(" http://localhost:8081/// ")).toBe("http://localhost:8081");
    expect(customerConfigUrl("http://localhost:8081/", "customer a")).toBe(
      "http://localhost:8081/customers/customer%20a"
    );
  });

  it("maps backend customer configuration into softphone settings", () => {
    expect(
      toSoftphoneCustomerSettings(
        {
          customerId: "customer-a",
          customerName: "Customer A",
          sipDomain: "asterisk.customer-a.example.com",
          webSocketUrl: "wss://asterisk.customer-a.example.com:8089/ws",
          apiBaseUrl: "http://app.customer-a.example.com:8081",
          defaultAgentUiMode: "jssip",
          brandDisplayName: "Customer A Contact Center"
        },
        "fallback-customer"
      )
    ).toEqual({
      customerId: "customer-a",
      customerName: "Customer A",
      sipServer: "asterisk.customer-a.example.com",
      websocketUrl: "wss://asterisk.customer-a.example.com:8089/ws",
      apiBaseUrl: "http://app.customer-a.example.com:8081",
      agentUiMode: "jssip",
      brandDisplayName: "Customer A Contact Center"
    });
  });

  it("falls back to the requested customer id when the response omits one", () => {
    expect(toSoftphoneCustomerSettings({}, "customer-a").customerId).toBe("customer-a");
  });
});
