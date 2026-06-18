package com.proxio.discoveryservice.service;

import com.proxio.discoveryservice.model.ServiceInstance;
import com.proxio.discoveryservice.model.ServiceRegistration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistryService {

    private final Map<String, Map<String, ServiceInstance>> services = new ConcurrentHashMap<>();
    private final Duration ttl;

    public RegistryService(@Value("${discovery.ttl-seconds:90}") long ttlSeconds) {
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public ServiceInstance register(ServiceRegistration registration) {
        Instant now = Instant.now();
        ServiceInstance instance = new ServiceInstance(
                registration.serviceId(),
                registration.instanceId(),
                normalizeBaseUrl(registration.baseUrl()),
                now,
                now
        );
        services.computeIfAbsent(instance.serviceId(), ignored -> new ConcurrentHashMap<>())
                .put(instance.instanceId(), instance);
        return instance;
    }

    public ServiceInstance heartbeat(String serviceId, String instanceId) {
        Map<String, ServiceInstance> instances = services.get(serviceId);
        if (instances == null || !instances.containsKey(instanceId)) {
            return null;
        }
        ServiceInstance refreshed = instances.get(instanceId).heartbeat(Instant.now());
        instances.put(instanceId, refreshed);
        return refreshed;
    }

    public List<ServiceInstance> instances(String serviceId) {
        purgeExpired();
        return services.getOrDefault(serviceId, Map.of()).values().stream()
                .sorted(Comparator.comparing(ServiceInstance::instanceId))
                .toList();
    }

    public Map<String, List<ServiceInstance>> all() {
        purgeExpired();
        Map<String, List<ServiceInstance>> snapshot = new ConcurrentHashMap<>();
        services.keySet().forEach(serviceId -> snapshot.put(serviceId, instances(serviceId)));
        return snapshot;
    }

    public boolean unregister(String serviceId, String instanceId) {
        Map<String, ServiceInstance> instances = services.get(serviceId);
        return instances != null && instances.remove(instanceId) != null;
    }

    private void purgeExpired() {
        Instant cutoff = Instant.now().minus(ttl);
        services.values().forEach(instances ->
                instances.values().removeIf(instance -> instance.lastHeartbeat().isBefore(cutoff)));
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
