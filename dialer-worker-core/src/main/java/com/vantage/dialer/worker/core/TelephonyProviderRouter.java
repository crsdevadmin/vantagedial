package com.vantage.dialer.worker.core;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TelephonyProviderRouter {

    private final List<TelephonyProvider> providers;

    public TelephonyProviderRouter(List<TelephonyProvider> providers) {
        this.providers = providers;
    }

    public TelephonyProvider resolve(String provider) {
        return providers.stream()
                .filter(candidate -> candidate.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + provider));
    }
}
