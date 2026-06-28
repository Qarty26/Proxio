package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.*;
import com.proxio.backend.models.enums.OrderStatus;
import com.proxio.backend.models.enums.RatingStatus;
import com.proxio.backend.models.security.SecurityUser;
import com.proxio.backend.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CustomerService customerService;
    @Mock private StockRepository stockRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserRatingRepository userRatingRepository;
    @Mock private PickupSlotRepository pickupSlotRepository;

    @InjectMocks
    private OrderService orderService;

    // ── Auth helpers ─────────────────────────────────────────────────────────

    private Authentication authAs(Long userId, String role) {
        Authentication auth = mock(Authentication.class);
        SecurityUser su = mock(SecurityUser.class);
        when(su.getId()).thenReturn(userId);
        when(auth.getPrincipal()).thenReturn(su);
        doReturn(List.of((GrantedAuthority) () -> role)).when(auth).getAuthorities();
        return auth;
    }

    // ── Basic CRUD ────────────────────────────────────────────────────────────

    @Test
    void createReturnsSavedEntity() {
        Order entity = new Order();
        entity.setStatus(OrderStatus.PENDING);
        when(orderRepository.save(entity)).thenReturn(entity);
        assertSame(entity, orderService.create(entity));
        verify(orderRepository).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Order entity = new Order();
        when(orderRepository.save(entity)).thenThrow(new RuntimeException());
        assertThrows(CreateOperationException.class, () -> orderService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        when(orderRepository.findAll()).thenReturn(List.of(new Order(), new Order()));
        assertEquals(2, orderService.getAll().size());
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Order entity = new Order();
        entity.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(entity));
        assertEquals(1L, orderService.getById(1L).getId());
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Order existing = new Order();
        existing.setId(1L);
        existing.setStatus(OrderStatus.PENDING);
        Order updated = new Order();
        updated.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(existing);
        assertEquals(OrderStatus.CONFIRMED, orderService.update(1L, updated).getStatus());
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.update(1L, new Order()));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Order entity = new Order();
        entity.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(entity));
        orderService.delete(1L);
        verify(orderRepository).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.delete(1L));
        verify(orderRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Order entity = new Order();
        entity.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(orderRepository).delete(entity);
        assertThrows(DeleteOperationException.class, () -> orderService.delete(1L));
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Test
    void placeOrderReducesStockAndCreatesOrder() {
        Product product = new Product(); product.setId(1L);
        Location location = new Location(); location.setId(2L);
        Stock stock = new Stock(); stock.setProduct(product); stock.setLocation(location); stock.setQuantity(10.0);
        Customer customer = new Customer();
        Order saved = new Order(); saved.setId(99L);

        when(stockRepository.findByProductIdAndLocationId(1L, 2L)).thenReturn(Optional.of(stock));
        when(customerService.findOrCreateForCurrentUser(any())).thenReturn(customer);
        when(orderRepository.save(any())).thenReturn(saved);

        Authentication auth = authAs(5L, "ROLE_CUSTOMER");
        Order result = orderService.placeOrder(new OrderService.PlaceOrderRequest(1L, 2L, 3.0, null), auth);

        assertEquals(99L, result.getId());
        assertEquals(7.0, stock.getQuantity());
        verify(stockRepository).save(stock);
    }

    @Test
    void placeOrderWhenInsufficientStockThrowsIllegalArgument() {
        Product product = new Product(); product.setId(1L);
        Location location = new Location(); location.setId(2L);
        Stock stock = new Stock(); stock.setProduct(product); stock.setLocation(location); stock.setQuantity(2.0);

        when(stockRepository.findByProductIdAndLocationId(1L, 2L)).thenReturn(Optional.of(stock));

        assertThrows(IllegalArgumentException.class,
            () -> orderService.placeOrder(new OrderService.PlaceOrderRequest(1L, 2L, 5.0, null), authAs(5L, "ROLE_CUSTOMER")));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrderWhenStockNotFoundThrowsResourceNotFoundException() {
        when(stockRepository.findByProductIdAndLocationId(any(), any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
            () -> orderService.placeOrder(new OrderService.PlaceOrderRequest(1L, 2L, 1.0, null), authAs(5L, "ROLE_CUSTOMER")));
    }

    @Test
    void placeOrderWithPickupSlotAttachesSlotToOrder() {
        Product product = new Product(); product.setId(1L);
        Location location = new Location(); location.setId(2L);
        Stock stock = new Stock(); stock.setProduct(product); stock.setLocation(location); stock.setQuantity(10.0);
        PickupSlot slot = new PickupSlot(); slot.setId(7L); slot.setLocation(location);
        Customer customer = new Customer();
        Order saved = new Order(); saved.setId(1L);

        when(stockRepository.findByProductIdAndLocationId(1L, 2L)).thenReturn(Optional.of(stock));
        when(pickupSlotRepository.findById(7L)).thenReturn(Optional.of(slot));
        when(customerService.findOrCreateForCurrentUser(any())).thenReturn(customer);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.placeOrder(new OrderService.PlaceOrderRequest(1L, 2L, 1.0, 7L), authAs(5L, "ROLE_CUSTOMER"));

        assertNotNull(result.getPickupSlot());
        assertEquals(7L, result.getPickupSlot().getId());
    }

    @Test
    void placeOrderWithPickupSlotFromWrongLocationThrowsIllegalArgument() {
        Product product = new Product(); product.setId(1L);
        Location location = new Location(); location.setId(2L);
        Location otherLocation = new Location(); otherLocation.setId(99L);
        Stock stock = new Stock(); stock.setProduct(product); stock.setLocation(location); stock.setQuantity(10.0);
        PickupSlot slot = new PickupSlot(); slot.setId(7L); slot.setLocation(otherLocation);

        when(stockRepository.findByProductIdAndLocationId(1L, 2L)).thenReturn(Optional.of(stock));
        when(pickupSlotRepository.findById(7L)).thenReturn(Optional.of(slot));

        assertThrows(IllegalArgumentException.class,
            () -> orderService.placeOrder(new OrderService.PlaceOrderRequest(1L, 2L, 1.0, 7L), authAs(5L, "ROLE_CUSTOMER")));
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Test
    void cancelOrderByCustomerRestoresStockAndSetsStatusCancelled() {
        User customerUser = new User(); customerUser.setId(5L);
        Customer customer = new Customer(); customer.setUser(customerUser);
        Product product = new Product(); product.setId(1L);
        Location location = new Location(); location.setId(2L);
        OrderItem item = new OrderItem(); item.setProduct(product); item.setQuantity(3.0);
        Order order = new Order();
        order.setId(1L); order.setStatus(OrderStatus.PENDING);
        order.setCustomer(customer); order.setLocation(location); order.setItems(List.of(item));
        Stock stock = new Stock(); stock.setQuantity(7.0);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(stockRepository.findByProductIdAndLocationId(1L, 2L)).thenReturn(Optional.of(stock));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.cancelOrder(1L, authAs(5L, "ROLE_CUSTOMER"));

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(10.0, stock.getQuantity());
    }

    @Test
    void cancelOrderWhenAlreadyPickedUpThrowsIllegalState() {
        User customerUser = new User(); customerUser.setId(5L);
        Customer customer = new Customer(); customer.setUser(customerUser);
        Order order = new Order();
        order.setId(1L); order.setStatus(OrderStatus.PICKED_UP);
        order.setCustomer(customer); order.setItems(List.of());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(1L, authAs(5L, "ROLE_CUSTOMER")));
    }

    @Test
    void cancelOrderWhenAlreadyCancelledThrowsIllegalState() {
        User customerUser = new User(); customerUser.setId(5L);
        Customer customer = new Customer(); customer.setUser(customerUser);
        Order order = new Order();
        order.setId(1L); order.setStatus(OrderStatus.CANCELLED);
        order.setCustomer(customer); order.setItems(List.of());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(1L, authAs(5L, "ROLE_CUSTOMER")));
    }

    @Test
    void cancelOrderByUnrelatedUserThrowsAccessDenied() {
        User customerUser = new User(); customerUser.setId(5L);
        Customer customer = new Customer(); customer.setUser(customerUser);
        User vendorUser = new User(); vendorUser.setId(3L);
        Vendor vendor = new Vendor(); vendor.setUser(vendorUser);
        Location location = new Location(); location.setVendor(vendor);
        Order order = new Order();
        order.setId(1L); order.setStatus(OrderStatus.PENDING);
        order.setCustomer(customer); order.setLocation(location); order.setItems(List.of());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> orderService.cancelOrder(1L, authAs(99L, "ROLE_CUSTOMER")));
    }

    // ── markDelivered ─────────────────────────────────────────────────────────

    @Test
    void markDeliveredByVendorSetsStatusPickedUp() {
        User vendorUser = new User(); vendorUser.setId(3L);
        Vendor vendor = new Vendor(); vendor.setUser(vendorUser);
        Location location = new Location(); location.setVendor(vendor);
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.PENDING); order.setLocation(location);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.markDelivered(1L, authAs(3L, "ROLE_VENDOR"));

        assertEquals(OrderStatus.PICKED_UP, result.getStatus());
    }

    @Test
    void markDeliveredWhenCancelledThrowsIllegalState() {
        User vendorUser = new User(); vendorUser.setId(3L);
        Vendor vendor = new Vendor(); vendor.setUser(vendorUser);
        Location location = new Location(); location.setVendor(vendor);
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.CANCELLED); order.setLocation(location);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.markDelivered(1L, authAs(3L, "ROLE_VENDOR")));
    }

    @Test
    void markDeliveredWhenAlreadyPickedUpThrowsIllegalState() {
        User vendorUser = new User(); vendorUser.setId(3L);
        Vendor vendor = new Vendor(); vendor.setUser(vendorUser);
        Location location = new Location(); location.setVendor(vendor);
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.PICKED_UP); order.setLocation(location);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.markDelivered(1L, authAs(3L, "ROLE_VENDOR")));
    }

    @Test
    void markDeliveredByNonVendorThrowsAccessDenied() {
        User vendorUser = new User(); vendorUser.setId(3L);
        Vendor vendor = new Vendor(); vendor.setUser(vendorUser);
        Location location = new Location(); location.setVendor(vendor);
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.PENDING); order.setLocation(location);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> orderService.markDelivered(1L, authAs(99L, "ROLE_VENDOR")));
    }

    @Test
    void markDeliveredByAdminSucceedsRegardlessOfLocation() {
        Location location = new Location();
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.PENDING); order.setLocation(location);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.markDelivered(1L, authAs(1L, "ROLE_ADMIN"));

        assertEquals(OrderStatus.PICKED_UP, result.getStatus());
    }

    // ── rateOrder ─────────────────────────────────────────────────────────────

    @Test
    void rateOrderByCustomerCreatesRatingForVendor() {
        User customerUser = new User(); customerUser.setId(5L);
        User vendorUser = new User(); vendorUser.setId(3L);
        Vendor vendor = new Vendor(); vendor.setUser(vendorUser);
        Location location = new Location(); location.setVendor(vendor);
        Customer customer = new Customer(); customer.setUser(customerUser);
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.PICKED_UP);
        order.setCustomer(customer); order.setLocation(location);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRatingRepository.existsByOrderIdAndRaterId(1L, 5L)).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(customerUser));
        when(userRatingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserRating result = orderService.rateOrder(1L, new OrderService.RateOrderRequest(5, "Great!"), authAs(5L, "ROLE_CUSTOMER"));

        assertNotNull(result);
        assertEquals(customerUser, result.getRater());
        assertEquals(vendorUser, result.getRated());
        assertEquals(5, result.getScore());
        assertEquals(RatingStatus.PENDING, result.getStatus());
    }

    @Test
    void rateOrderByVendorCreatesRatingForCustomer() {
        User customerUser = new User(); customerUser.setId(5L);
        User vendorUser = new User(); vendorUser.setId(3L);
        Vendor vendor = new Vendor(); vendor.setUser(vendorUser);
        Location location = new Location(); location.setVendor(vendor);
        Customer customer = new Customer(); customer.setUser(customerUser);
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.PICKED_UP);
        order.setCustomer(customer); order.setLocation(location);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRatingRepository.existsByOrderIdAndRaterId(1L, 3L)).thenReturn(false);
        when(userRepository.findById(3L)).thenReturn(Optional.of(vendorUser));
        when(userRatingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserRating result = orderService.rateOrder(1L, new OrderService.RateOrderRequest(4, null), authAs(3L, "ROLE_VENDOR"));

        assertEquals(vendorUser, result.getRater());
        assertEquals(customerUser, result.getRated());
    }

    @Test
    void rateOrderWithScoreBelowOneThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
            () -> orderService.rateOrder(1L, new OrderService.RateOrderRequest(0, null), authAs(5L, "ROLE_CUSTOMER")));
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void rateOrderWithScoreAboveFiveThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
            () -> orderService.rateOrder(1L, new OrderService.RateOrderRequest(6, null), authAs(5L, "ROLE_CUSTOMER")));
    }

    @Test
    void rateOrderWhenOrderNotPickedUpThrowsIllegalState() {
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
            () -> orderService.rateOrder(1L, new OrderService.RateOrderRequest(5, null), authAs(5L, "ROLE_CUSTOMER")));
    }

    @Test
    void rateOrderWhenAlreadyRatedThrowsIllegalState() {
        User customerUser = new User(); customerUser.setId(5L);
        User vendorUser = new User(); vendorUser.setId(3L);
        Vendor vendor = new Vendor(); vendor.setUser(vendorUser);
        Location location = new Location(); location.setVendor(vendor);
        Customer customer = new Customer(); customer.setUser(customerUser);
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.PICKED_UP);
        order.setCustomer(customer); order.setLocation(location);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findById(5L)).thenReturn(Optional.of(customerUser));
        when(userRatingRepository.existsByOrderIdAndRaterId(1L, 5L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
            () -> orderService.rateOrder(1L, new OrderService.RateOrderRequest(5, null), authAs(5L, "ROLE_CUSTOMER")));
        verify(userRatingRepository, never()).save(any());
    }

    @Test
    void rateOrderByUnrelatedUserThrowsAccessDenied() {
        User customerUser = new User(); customerUser.setId(5L);
        User vendorUser = new User(); vendorUser.setId(3L);
        Vendor vendor = new Vendor(); vendor.setUser(vendorUser);
        Location location = new Location(); location.setVendor(vendor);
        Customer customer = new Customer(); customer.setUser(customerUser);
        Order order = new Order(); order.setId(1L); order.setStatus(OrderStatus.PICKED_UP);
        order.setCustomer(customer); order.setLocation(location);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRatingRepository.existsByOrderIdAndRaterId(1L, 99L)).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.of(new User()));

        assertThrows(AccessDeniedException.class,
            () -> orderService.rateOrder(1L, new OrderService.RateOrderRequest(5, null), authAs(99L, "ROLE_CUSTOMER")));
    }

    // ── getMyOrders / getVendorOrders ─────────────────────────────────────────

    @Test
    void getMyOrdersWhenCustomerExistsReturnsOrders() {
        Customer customer = new Customer(); customer.setId(10L);
        Order order = new Order(); order.setId(1L);

        when(customerService.findByUserId(5L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByCustomerId(10L)).thenReturn(List.of(order));

        List<Order> result = orderService.getMyOrders(authAs(5L, "ROLE_CUSTOMER"));

        assertEquals(1, result.size());
    }

    @Test
    void getMyOrdersWhenNoCustomerProfileReturnsEmptyList() {
        when(customerService.findByUserId(5L)).thenReturn(Optional.empty());

        List<Order> result = orderService.getMyOrders(authAs(5L, "ROLE_CUSTOMER"));

        assertTrue(result.isEmpty());
    }

    @Test
    void getVendorOrdersReturnsOrdersForVendorsLocations() {
        Vendor vendor = new Vendor(); vendor.setId(20L);
        Order order = new Order(); order.setId(1L);

        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));
        when(orderRepository.findByLocationVendorId(20L)).thenReturn(List.of(order));

        List<Order> result = orderService.getVendorOrders(authAs(3L, "ROLE_VENDOR"));

        assertEquals(1, result.size());
    }
}
