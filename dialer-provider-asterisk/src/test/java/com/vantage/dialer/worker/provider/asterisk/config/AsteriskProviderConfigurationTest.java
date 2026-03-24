package com.vantage.dialer.worker.provider.asterisk.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsteriskProviderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AsteriskProviderConfiguration.class);

    @Test
    void exposesAsteriskPropertiesWithDefaults() {
        contextRunner.run(context -> {
            AsteriskProperties properties = context.getBean(AsteriskProperties.class);

            assertEquals("localhost", properties.getHost());
            assertEquals(5038, properties.getPort());
            assertEquals("admin", properties.getUsername());
            assertEquals("vivphone-endpoint", properties.getEndpoint());
            assertEquals("Vantage Dialer", properties.getCallerId());
            assertEquals("91", properties.getDialPrefix());
            assertEquals("from-internal", properties.getOriginateContext());
            assertEquals("s", properties.getOriginateExtension());
            assertEquals(1, properties.getOriginatePriority());
            assertEquals(30000L, properties.getOriginateTimeoutMs());
            assertEquals(10000L, properties.getReconnectDelayMs());
        });
    }

    @Test
    void bindsAsteriskPropertiesOverrides() {
        contextRunner
                .withPropertyValues(
                        "asterisk.ami.host=pbx.internal",
                        "asterisk.ami.port=5040",
                        "asterisk.ami.username=worker",
                        "asterisk.ami.password=super-secret",
                        "asterisk.ami.endpoint=softphone-endpoint",
                        "asterisk.ami.caller-id=Outbound Team",
                        "asterisk.ami.dial-prefix=001",
                        "asterisk.ami.originate-context=from-test",
                        "asterisk.ami.originate-extension=101",
                        "asterisk.ami.originate-priority=3",
                        "asterisk.ami.originate-timeout-ms=45000",
                        "asterisk.ami.reconnect-delay-ms=2500"
                )
                .run(context -> {
                    AsteriskProperties properties = context.getBean(AsteriskProperties.class);

                    assertEquals("pbx.internal", properties.getHost());
                    assertEquals(5040, properties.getPort());
                    assertEquals("worker", properties.getUsername());
                    assertEquals("super-secret", properties.getPassword());
                    assertEquals("softphone-endpoint", properties.getEndpoint());
                    assertEquals("Outbound Team", properties.getCallerId());
                    assertEquals("001", properties.getDialPrefix());
                    assertEquals("from-test", properties.getOriginateContext());
                    assertEquals("101", properties.getOriginateExtension());
                    assertEquals(3, properties.getOriginatePriority());
                    assertEquals(45000L, properties.getOriginateTimeoutMs());
                    assertEquals(2500L, properties.getReconnectDelayMs());
                });
    }
}
