package com.vantage.dialer.worker.core;

public interface TelephonyProvider {

    boolean supports(String provider);

    void startCustomerLeg(CallSession session);

    void startAgentLeg(CallSession session);

    void bridge(CallSession session);
}
