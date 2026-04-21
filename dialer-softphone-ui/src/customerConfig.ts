export type CustomerConfigurationResponse = {
  customerId?: string;
  customerName?: string;
  sipDomain?: string;
  webSocketUrl?: string;
  apiBaseUrl?: string;
  defaultAgentUiMode?: string;
  brandDisplayName?: string;
};

export type SoftphoneCustomerSettings = {
  customerId: string;
  customerName?: string;
  sipServer?: string;
  websocketUrl?: string;
  apiBaseUrl?: string;
  agentUiMode?: string;
  brandDisplayName?: string;
};

function normalizeOptional(value?: string): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

export function normalizeApiBaseUrl(value: string): string {
  return value.trim().replace(/\/+$/, "");
}

export function customerConfigUrl(apiBaseUrl: string, customerId: string): string {
  return `${normalizeApiBaseUrl(apiBaseUrl)}/customers/${encodeURIComponent(customerId.trim())}`;
}

export function toSoftphoneCustomerSettings(
  response: CustomerConfigurationResponse,
  requestedCustomerId: string
): SoftphoneCustomerSettings {
  return {
    customerId: normalizeOptional(response.customerId) ?? requestedCustomerId.trim(),
    customerName: normalizeOptional(response.customerName),
    sipServer: normalizeOptional(response.sipDomain),
    websocketUrl: normalizeOptional(response.webSocketUrl),
    apiBaseUrl: normalizeOptional(response.apiBaseUrl),
    agentUiMode: normalizeOptional(response.defaultAgentUiMode),
    brandDisplayName: normalizeOptional(response.brandDisplayName)
  };
}

export async function fetchCustomerSettings(
  apiBaseUrl: string,
  customerId: string
): Promise<SoftphoneCustomerSettings> {
  const normalizedCustomerId = customerId.trim();
  if (!normalizedCustomerId) {
    throw new Error("Customer ID is required");
  }

  const response = await fetch(customerConfigUrl(apiBaseUrl, normalizedCustomerId));
  if (!response.ok) {
    throw new Error(`Customer config request failed with ${response.status}`);
  }

  return toSoftphoneCustomerSettings(
    (await response.json()) as CustomerConfigurationResponse,
    normalizedCustomerId
  );
}
