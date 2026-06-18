package com.proxio.discoveryservice.model;

import java.time.Instant;

public record ServiceInstance(
        String serviceId,
        String instanceId,
        String baseUrl,
        Instant registeredAt,
        Instant lastHeartbeat
) {

    public ServiceInstance heartbeat(Instant now) {
        return new ServiceInstance(serviceId, instanceId, baseUrl, registeredAt, now);
    }
}
