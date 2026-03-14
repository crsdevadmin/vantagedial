package com.vantage.dialer.common.kafka;

public final class Topics {
  private Topics() {}

  public static final String RAW_EXOTEL = "provider.raw.exotel";
  public static final String RAW_TWILIO = "provider.raw.twilio";
  public static final String STANDARD_EVENTS = "dialer.standard.events";
  public static final String COMMANDS = "dialer.commands";
  public static final String EVENTS = "dialer.events";
}