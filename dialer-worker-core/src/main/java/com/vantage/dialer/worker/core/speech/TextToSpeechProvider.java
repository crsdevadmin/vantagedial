package com.vantage.dialer.worker.core.speech;

public interface TextToSpeechProvider {
    String providerName();

    byte[] synthesize(String text, String voice, String languageCode);
}
