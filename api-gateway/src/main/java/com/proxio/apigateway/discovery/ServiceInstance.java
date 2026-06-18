package com.proxio.apigateway.discovery;

public record ServiceInstance(
        String serviceId,
        String instanceId,
        String baseUrl
) {
}
