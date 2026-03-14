package com.vantage.dialer.worker.provider.asterisk.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AsteriskProperties.class)
public class AsteriskProviderConfiguration {
}
