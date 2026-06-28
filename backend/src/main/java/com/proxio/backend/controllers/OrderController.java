package com.proxio.backend.controllers;

import com.proxio.backend.models.Order;
import com.proxio.backend.models.UserRating;
import com.proxio.backend.services.OrderService;
import com.proxio.backend.services.OrderService.PlaceOrderRequest;
import com.proxio.backend.services.OrderService.RateOrderRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── Specific routes first (before /{id}) ────────────────────────────────

    @PostMapping("/place")
    public ResponseEntity<Order> place(@RequestBody PlaceOrderRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request, authentication));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getMyOrders(authentication));
    }

    @GetMapping("/vendor")
    public ResponseEntity<List<Order>> getVendorOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getVendorOrders(authentication));
    }

    // ── Generic CRUD ─────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(order));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancel(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(orderService.cancelOrder(id, authentication));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<Order> deliver(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(orderService.markDelivered(id, authentication));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<UserRating> rate(@PathVariable Long id,
                                           @RequestBody RateOrderRequest request,
                                           Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.rateOrder(id, request, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable Long id, @RequestBody Order order) {
        return ResponseEntity.ok(orderService.update(id, order));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
