package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.*;
import com.proxio.backend.models.enums.OrderStatus;
import com.proxio.backend.models.enums.RatingStatus;
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
    private final UserRatingRepository userRatingRepository;
    private final PickupSlotRepository pickupSlotRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerService customerService,
                        StockRepository stockRepository,
                        VendorRepository vendorRepository,
                        UserRepository userRepository,
                        UserRatingRepository userRatingRepository,
                        PickupSlotRepository pickupSlotRepository) {
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.stockRepository = stockRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.userRatingRepository = userRatingRepository;
        this.pickupSlotRepository = pickupSlotRepository;
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

        if (request.pickupSlotId() != null) {
            PickupSlot slot = pickupSlotRepository.findById(request.pickupSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pickup slot not found."));
            if (!slot.getLocation().getId().equals(request.locationId())) {
                throw new IllegalArgumentException("Pickup slot does not belong to the selected location.");
            }
            order.setPickupSlot(slot);
        }

        item.setOrder(order);
        order.setItems(List.of(item));

        Order saved = orderRepository.save(order);

        stock.setQuantity(stock.getQuantity() - request.quantity());
        stockRepository.save(stock);

        return saved;
    }

    public record PlaceOrderRequest(Long productId, Long locationId, Double quantity, Long pickupSlotId) {}

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

    // ── Mark as delivered ────────────────────────────────────────────────────

    public Order markDelivered(Long orderId, Authentication authentication) {
        Order order = getById(orderId);

        assertIsVendorOfOrder(order, authentication);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot deliver a cancelled order.");
        }
        if (order.getStatus() == OrderStatus.PICKED_UP) {
            throw new IllegalStateException("Order is already marked as delivered.");
        }

        order.setStatus(OrderStatus.PICKED_UP);
        return orderRepository.save(order);
    }

    // ── Rate order ───────────────────────────────────────────────────────────

    @Transactional
    public UserRating rateOrder(Long orderId, RateOrderRequest request, Authentication authentication) {
        if (request.score() < 1 || request.score() > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5.");
        }

        Order order = getById(orderId);

        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new IllegalStateException("You can only rate a delivered order.");
        }

        Long userId = resolveUserId(authentication);
        User rater = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (userRatingRepository.existsByOrderIdAndRaterId(orderId, userId)) {
            throw new IllegalStateException("You have already rated this order.");
        }

        // Determine who is being rated
        User rated;
        boolean isCustomer = order.getCustomer() != null
                && order.getCustomer().getUser() != null
                && order.getCustomer().getUser().getId().equals(userId);

        if (isCustomer) {
            if (order.getLocation() == null || order.getLocation().getVendor() == null
                    || order.getLocation().getVendor().getUser() == null) {
                throw new ResourceNotFoundException("Vendor user not found for this order.");
            }
            rated = order.getLocation().getVendor().getUser();
        } else {
            // Check rater is the vendor
            boolean isVendor = order.getLocation() != null
                    && order.getLocation().getVendor() != null
                    && order.getLocation().getVendor().getUser() != null
                    && order.getLocation().getVendor().getUser().getId().equals(userId);
            if (!isVendor) {
                throw new AccessDeniedException("Only the customer or vendor of this order can submit a rating.");
            }
            if (order.getCustomer() == null || order.getCustomer().getUser() == null) {
                throw new ResourceNotFoundException("Customer user not found for this order.");
            }
            rated = order.getCustomer().getUser();
        }

        UserRating rating = new UserRating();
        rating.setRater(rater);
        rating.setRated(rated);
        rating.setOrder(order);
        rating.setScore(request.score());
        rating.setComment(request.comment());
        rating.setStatus(RatingStatus.PENDING);

        return userRatingRepository.save(rating);
    }

    public record RateOrderRequest(Integer score, String comment) {}

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

    private void assertIsVendorOfOrder(Order order, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return;

        Long userId = resolveUserId(authentication);
        if (order.getLocation() == null
                || order.getLocation().getVendor() == null
                || order.getLocation().getVendor().getUser() == null
                || !order.getLocation().getVendor().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Only the vendor at this location can mark the order as delivered.");
        }
    }

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
