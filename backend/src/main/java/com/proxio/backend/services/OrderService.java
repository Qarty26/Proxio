package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.*;
import com.proxio.backend.models.enums.OrderStatus;
import com.proxio.backend.models.security.SecurityUser;
import com.proxio.backend.repositories.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final StockRepository stockRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerService customerService,
                        StockRepository stockRepository,
                        VendorRepository vendorRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.stockRepository = stockRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
    }

    // ── Existing generic CRUD (kept for backward compatibility) ──────────────

    public Order create(Order order) {
        try {
            return orderRepository.save(order);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Order.");
        }
    }

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + id + " was not found."));
    }

    public Order update(Long id, Order order) {
        try {
            Order existing = getById(id);
            existing.setCustomer(order.getCustomer());
            existing.setLocation(order.getLocation());
            existing.setPickupSlot(order.getPickupSlot());
            existing.setStatus(order.getStatus());
            existing.setItems(order.getItems());
            existing.setRatings(order.getRatings());
            return orderRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Order with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Order existing = getById(id);
            orderRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Order with id " + id + ".");
        }
    }

    // ── Place order ──────────────────────────────────────────────────────────

    @Transactional
    public Order placeOrder(PlaceOrderRequest request, Authentication authentication) {
        // Resolve stock — also gives us product and location in one query
        Stock stock = stockRepository
                .findByProductIdAndLocationId(request.productId(), request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No stock found for this product at the selected location."));

        if (stock.getQuantity() < request.quantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock: " + stock.getQuantity() + " available.");
        }

        Customer customer = customerService.findOrCreateForCurrentUser(authentication);

        OrderItem item = new OrderItem();
        item.setProduct(stock.getProduct());
        item.setQuantity(request.quantity());
        item.setPriceAtOrder(0.0);

        Order order = new Order();
        order.setCustomer(customer);
        order.setLocation(stock.getLocation());
        order.setStatus(OrderStatus.PENDING);
        item.setOrder(order);
        order.setItems(List.of(item));

        Order saved = orderRepository.save(order);

        stock.setQuantity(stock.getQuantity() - request.quantity());
        stockRepository.save(stock);

        return saved;
    }

    public record PlaceOrderRequest(Long productId, Long locationId, Double quantity) {}

    // ── Cancel order ─────────────────────────────────────────────────────────

    @Transactional
    public Order cancelOrder(Long orderId, Authentication authentication) {
        Order order = getById(orderId);

        assertCanCancelOrder(order, authentication);

        if (order.getStatus() == OrderStatus.PICKED_UP) {
            throw new IllegalStateException("Cannot cancel an order that has already been picked up.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("This order is already cancelled.");
        }

        // Restore stock for every item
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null && order.getLocation() != null) {
                    stockRepository.findByProductIdAndLocationId(
                            item.getProduct().getId(), order.getLocation().getId()
                    ).ifPresent(stock -> {
                        stock.setQuantity(stock.getQuantity() + item.getQuantity());
                        stockRepository.save(stock);
                    });
                }
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    // ── Customer: my orders ──────────────────────────────────────────────────

    public List<Order> getMyOrders(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return customerService.findByUserId(userId)
                .map(c -> orderRepository.findByCustomerId(c.getId()))
                .orElse(List.of());
    }

    // ── Vendor: incoming orders ──────────────────────────────────────────────

    public List<Order> getVendorOrders(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Vendor profile not found."));
        return orderRepository.findByLocationVendorId(vendor.getId());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertCanCancelOrder(Order order, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return;

        Long userId = resolveUserId(authentication);

        if (order.getCustomer() != null
                && order.getCustomer().getUser() != null
                && order.getCustomer().getUser().getId().equals(userId)) {
            return;
        }

        if (order.getLocation() != null
                && order.getLocation().getVendor() != null
                && order.getLocation().getVendor().getUser() != null
                && order.getLocation().getVendor().getUser().getId().equals(userId)) {
            return;
        }

        throw new AccessDeniedException("You are not authorised to cancel this order.");
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof SecurityUser su) {
            return su.getId();
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        return user.getId();
    }
}
