package com.vantage.dialer.worker.core.speech;

public interface SpeechToTextProvider {
    String providerName();

    String transcribe(byte[] audioBytes, String languageCode);
}
