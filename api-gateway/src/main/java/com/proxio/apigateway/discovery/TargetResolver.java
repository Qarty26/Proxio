package com.proxio.apigateway.discovery;

import com.proxio.apigateway.config.ProxyProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TargetResolver {

    private final DiscoveryClient discoveryClient;
    private final ProxyProperties proxyProperties;
    private final Map<String, AtomicInteger> cursors = new ConcurrentHashMap<>();

    public TargetResolver(DiscoveryClient discoveryClient, ProxyProperties proxyProperties) {
        this.discoveryClient = discoveryClient;
        this.proxyProperties = proxyProperties;
    }

    public List<String> candidates(String serviceId) {
        List<String> discovered = discoveryClient.instances(serviceId).stream()
                .map(ServiceInstance::baseUrl)
                .distinct()
                .toList();
        if (!discovered.isEmpty()) {
            return rotate(serviceId, discovered);
        }
        return staticFallback(serviceId);
    }

    private List<String> rotate(String serviceId, List<String> targets) {
        int cursor = Math.floorMod(cursors.computeIfAbsent(serviceId, ignored -> new AtomicInteger()).getAndIncrement(),
                targets.size());
        return java.util.stream.IntStream.range(0, targets.size())
                .mapToObj(index -> targets.get((cursor + index) % targets.size()))
                .toList();
    }

    private List<String> staticFallback(String serviceId) {
        String target = switch (serviceId) {
            case "notification-service" -> proxyProperties.notifications();
            case "discovery-service" -> proxyProperties.discovery();
            case "config-service" -> proxyProperties.config();
            default -> proxyProperties.backend();
        };
        return List.of(target);
    }
}
