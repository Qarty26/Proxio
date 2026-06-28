package com.proxio.discoveryservice.controller;

import com.proxio.discoveryservice.model.ServiceInstance;
import com.proxio.discoveryservice.model.ServiceRegistration;
import com.proxio.discoveryservice.service.RegistryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final RegistryService registryService;

    public DiscoveryController(RegistryService registryService) {
        this.registryService = registryService;
    }

    @GetMapping("/services")
    public ResponseEntity<Map<String, List<ServiceInstance>>> services() {
        return ResponseEntity.ok(registryService.all());
    }

    @GetMapping("/services/{serviceId}")
    public ResponseEntity<List<ServiceInstance>> service(@PathVariable String serviceId) {
        return ResponseEntity.ok(registryService.instances(serviceId));
    }

    @PostMapping("/register")
    public ResponseEntity<ServiceInstance> register(@Valid @RequestBody ServiceRegistration registration) {
        ServiceInstance instance = registryService.register(registration);
        return ResponseEntity.created(URI.create("/api/discovery/services/" + instance.serviceId()))
                .body(instance);
    }

    @PostMapping("/services/{serviceId}/{instanceId}/heartbeat")
    public ResponseEntity<ServiceInstance> heartbeat(@PathVariable String serviceId, @PathVariable String instanceId) {
        ServiceInstance instance = registryService.heartbeat(serviceId, instanceId);
        return instance == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(instance);
    }

    @DeleteMapping("/services/{serviceId}/{instanceId}")
    public ResponseEntity<Void> unregister(@PathVariable String serviceId, @PathVariable String instanceId) {
        return registryService.unregister(serviceId, instanceId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
