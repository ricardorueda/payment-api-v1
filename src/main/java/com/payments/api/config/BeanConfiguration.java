package com.payments.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TransactionLimitConfig.class)
public class BeanConfiguration {
}

