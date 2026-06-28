package com.proxio.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.targets")
public record ProxyProperties(String backend, String notifications, String discovery, String config) {
}
