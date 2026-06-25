package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.controllers.forms.RatingForm;
import com.proxio.backend.models.Location;
import com.proxio.backend.models.Customer;
import com.proxio.backend.models.Order;
import com.proxio.backend.models.OrderItem;
import com.proxio.backend.models.PickupSlot;
import com.proxio.backend.models.Subscription;
import com.proxio.backend.models.UserRating;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.WeeklyOffer;
import com.proxio.backend.models.enums.OrderStatus;
import com.proxio.backend.models.enums.RatingStatus;
import com.proxio.backend.repositories.CustomerRepository;
import com.proxio.backend.repositories.LocationRepository;
import com.proxio.backend.repositories.OrderRepository;
import com.proxio.backend.repositories.PickupSlotRepository;
import com.proxio.backend.repositories.SubscriptionRepository;
import com.proxio.backend.repositories.UserRatingRepository;
import com.proxio.backend.repositories.VendorRepository;
import com.proxio.backend.repositories.WeeklyOfferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MarketplaceService {

    private final WeeklyOfferRepository weeklyOfferRepository;
    private final CustomerRepository customerRepository;
    private final PickupSlotRepository pickupSlotRepository;
    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LocationRepository locationRepository;
    private final UserRatingRepository userRatingRepository;

    public MarketplaceService(
            WeeklyOfferRepository weeklyOfferRepository,
            CustomerRepository customerRepository,
            PickupSlotRepository pickupSlotRepository,
            OrderRepository orderRepository,
            VendorRepository vendorRepository,
            SubscriptionRepository subscriptionRepository,
            LocationRepository locationRepository,
            UserRatingRepository userRatingRepository
    ) {
        this.weeklyOfferRepository = weeklyOfferRepository;
        this.customerRepository = customerRepository;
        this.pickupSlotRepository = pickupSlotRepository;
        this.orderRepository = orderRepository;
        this.vendorRepository = vendorRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.locationRepository = locationRepository;
        this.userRatingRepository = userRatingRepository;
    }

    public Page<WeeklyOffer> activeOffers(Pageable pageable) {
        return activeOffers(null, null, pageable);
    }

    public Page<WeeklyOffer> activeOffers(Long locationId, Pageable pageable) {
        return activeOffers(locationId, null, pageable);
    }

    public Page<WeeklyOffer> activeOffers(Long locationId, Long vendorId, Pageable pageable) {
        LocalDate today = LocalDate.now();
        return weeklyOfferRepository.searchActiveOffers(
                today,
                0.0,
                locationId,
                vendorId,
                pageable
        );
    }

    public List<Location> locations() {
        return locationRepository.findAllByOrderByCityAscNameAsc();
    }

    public WeeklyOffer offer(Long id) {
        return weeklyOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer with id " + id + " was not found."));
    }

    public List<PickupSlot> pickupSlotsForOffer(Long offerId) {
        WeeklyOffer offer = offer(offerId);
        return pickupSlotRepository.findByLocationIdOrderByStartTimeAsc(offer.getLocation().getId());
    }

    public List<Order> ordersFor(String email) {
        return orderRepository.findByCustomerUserEmailOrderByCreatedAtDesc(email);
    }

    public UserRating ratingFor(String email, Long orderId) {
        return userRatingRepository.findByOrderIdAndRaterEmail(orderId, email).orElse(null);
    }

    public String ratingSummaryForVendor(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor with id " + vendorId + " was not found."));
        Double average = userRatingRepository.averageScoreForRatedId(vendor.getUser().getId());
        long count = userRatingRepository.countByRatedIdAndStatus(vendor.getUser().getId(), RatingStatus.APPROVED);
        if (average == null || count == 0) {
            return "No ratings yet";
        }
        return String.format(java.util.Locale.US, "%.1f / 5 (%d)", average, count);
    }

    public int roundedRatingForVendor(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor with id " + vendorId + " was not found."));
        Double average = userRatingRepository.averageScoreForRatedId(vendor.getUser().getId());
        if (average == null) {
            return 0;
        }
        return Math.max(1, Math.min(5, (int) Math.round(average)));
    }

    @Transactional
    public Order markDone(String email, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " was not found."));
        if (!order.getCustomer().getUser().getEmail().equals(email)) {
            throw new CreateOperationException("You can mark only your own orders as done.");
        }
        order.setStatus(OrderStatus.PICKED_UP);
        return orderRepository.save(order);
    }

    @Transactional
    public UserRating rateOrder(String email, Long orderId, RatingForm form) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " was not found."));
        if (!order.getCustomer().getUser().getEmail().equals(email)) {
            throw new CreateOperationException("You can rate only your own orders.");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new CreateOperationException("This order has no vendor to rate.");
        }
        if (userRatingRepository.existsByOrderIdAndRaterEmail(orderId, email)) {
            throw new CreateOperationException("You already rated this order.");
        }
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new CreateOperationException("Mark the order as done before rating.");
        }

        UserRating rating = new UserRating();
        rating.setOrder(order);
        rating.setRater(order.getCustomer().getUser());
        rating.setRated(order.getItems().get(0).getWeeklyOffer().getProduct().getVendor().getUser());
        rating.setScore(form.getScore());
        rating.setComment(form.getComment());
        rating.setStatus(RatingStatus.APPROVED);
        return userRatingRepository.save(rating);
    }

    public List<Subscription> subscriptionsFor(String email) {
        return subscriptionRepository.findByCustomerUserEmailOrderBySubscribedAtDesc(email);
    }

    public boolean subscribedTo(String email, Long vendorId) {
        return subscriptionRepository.existsByCustomerUserEmailAndVendorId(email, vendorId);
    }

    @Transactional
    public void subscribe(String email, Long vendorId) {
        if (subscribedTo(email, vendorId)) {
            return;
        }
        Customer customer = customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new CreateOperationException("Your account does not have a customer profile."));
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor with id " + vendorId + " was not found."));
        Subscription subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setVendor(vendor);
        subscription.setNotificationsEnabled(true);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String email, Long vendorId) {
        subscriptionRepository.findByCustomerUserEmailAndVendorId(email, vendorId)
                .ifPresent(subscriptionRepository::delete);
    }

    @Transactional
    public Order placeOrder(String email, Long offerId, Double quantity, Long pickupSlotId) {
        Customer customer = customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new CreateOperationException("Your account does not have a customer profile."));
        WeeklyOffer offer = offer(offerId);

        if (quantity == null || quantity <= 0) {
            throw new CreateOperationException("Quantity must be positive.");
        }
        if (offer.getAvailableQuantity() < quantity) {
            throw new CreateOperationException("Only " + offer.getAvailableQuantity() + " " + offer.getProduct().getUnit() + " available.");
        }

        PickupSlot pickupSlot = null;
        if (pickupSlotId != null) {
            pickupSlot = pickupSlotRepository.findById(pickupSlotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pickup slot with id " + pickupSlotId + " was not found."));
            if (!pickupSlot.getLocation().getId().equals(offer.getLocation().getId())) {
                throw new CreateOperationException("Pickup slot does not belong to the offer location.");
            }
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setLocation(offer.getLocation());
        order.setPickupSlot(pickupSlot);
        order.setStatus(OrderStatus.PENDING);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setWeeklyOffer(offer);
        item.setQuantity(quantity);
        item.setPriceAtOrder(offer.getPricePerUnit());
        order.setItems(List.of(item));

        offer.setAvailableQuantity(offer.getAvailableQuantity() - quantity);
        weeklyOfferRepository.save(offer);
        return orderRepository.save(order);
    }
}
