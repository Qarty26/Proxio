package com.proxio.backend.config;

import com.proxio.backend.models.Customer;
import com.proxio.backend.models.Location;
import com.proxio.backend.models.Order;
import com.proxio.backend.models.OrderItem;
import com.proxio.backend.models.PickupSlot;
import com.proxio.backend.models.Product;
import com.proxio.backend.models.Stock;
import com.proxio.backend.models.Subscription;
import com.proxio.backend.models.User;
import com.proxio.backend.models.UserRating;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.WeeklyOffer;
import com.proxio.backend.models.enums.OrderStatus;
import com.proxio.backend.models.enums.ProductCategory;
import com.proxio.backend.models.enums.RatingStatus;
import com.proxio.backend.models.enums.Role;
import com.proxio.backend.repositories.CustomerRepository;
import com.proxio.backend.repositories.LocationRepository;
import com.proxio.backend.repositories.OrderRepository;
import com.proxio.backend.repositories.PickupSlotRepository;
import com.proxio.backend.repositories.ProductRepository;
import com.proxio.backend.repositories.StockRepository;
import com.proxio.backend.repositories.SubscriptionRepository;
import com.proxio.backend.repositories.UserRepository;
import com.proxio.backend.repositories.UserRatingRepository;
import com.proxio.backend.repositories.VendorRepository;
import com.proxio.backend.repositories.WeeklyOfferRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final VendorRepository vendorRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final WeeklyOfferRepository weeklyOfferRepository;
    private final PickupSlotRepository pickupSlotRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;
    private final UserRatingRepository userRatingRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            VendorRepository vendorRepository,
            LocationRepository locationRepository,
            ProductRepository productRepository,
            StockRepository stockRepository,
            WeeklyOfferRepository weeklyOfferRepository,
            PickupSlotRepository pickupSlotRepository,
            SubscriptionRepository subscriptionRepository,
            OrderRepository orderRepository,
            UserRatingRepository userRatingRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.vendorRepository = vendorRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.weeklyOfferRepository = weeklyOfferRepository;
        this.pickupSlotRepository = pickupSlotRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orderRepository = orderRepository;
        this.userRatingRepository = userRatingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        userRepository.findByEmail("admin@proxio.local")
                .orElseGet(() -> userRepository.save(user("admin@proxio.local", "Proxio Admin", "admin123", Role.ADMIN)));

        User shopper = userRepository.findByEmail("user@proxio.local")
                .orElseGet(() -> userRepository.save(user("user@proxio.local", "Local Shopper", "user123", Role.USER)));
        Customer customer = customerRepository.findByUserEmail(shopper.getEmail())
                .orElseGet(() -> createCustomer(shopper));

        SeedVendor greenFarm = vendor("vendor@proxio.local", "Green Farm Owner", "vendor123", "Green Farm",
                "Fresh vegetables, honey and orchard products from Brasov.", "Piata Centrala Pickup", "Piata Centrala 1", "Brasov");
        SeedVendor orchard = vendor("vendor2@proxio.local", "Orchard House Owner", "vendor123", "Orchard House",
                "Small orchard with seasonal fruits, juices and preserves.", "Orchard House Stand", "Strada Livezii 4", "Sibiu");
        SeedVendor dairy = vendor("vendor3@proxio.local", "Dairy Corner Owner", "vendor123", "Dairy Corner",
                "Local dairy products prepared in small batches.", "Dairy Corner Pickup", "Bulevardul Republicii 22", "Cluj-Napoca");
        SeedVendor meadow = vendor("vendor4@proxio.local", "Meadow Goods Owner", "vendor123", "Meadow Goods",
                "Meat, eggs and pantry staples from nearby farms.", "Meadow Goods Booth", "Piata Unirii 8", "Iasi");

        subscribe(customer, greenFarm.vendor());
        subscribe(customer, orchard.vendor());

        createOffer(greenFarm, "Tomatoes", ProductCategory.VEGETABLES, "kg", 8.5, 35.0, "Sweet greenhouse tomatoes");
        createOffer(greenFarm, "Cucumbers", ProductCategory.VEGETABLES, "kg", 6.5, 28.0, "Crunchy cucumbers for salads");
        createOffer(greenFarm, "Bell peppers", ProductCategory.VEGETABLES, "kg", 11.0, 22.0, "Mixed red and yellow peppers");
        createOffer(greenFarm, "Wildflower honey", ProductCategory.HONEY, "jar", 28.0, 18.0, "Raw wildflower honey");
        createOffer(greenFarm, "Carrots", ProductCategory.VEGETABLES, "kg", 4.8, 45.0, "Fresh bunch carrots");
        createOffer(greenFarm, "Herb bundles", ProductCategory.OTHERS, "bundle", 5.5, 30.0, "Parsley, dill and thyme bundles");
        createOffer(greenFarm, "Spinach leaves", ProductCategory.VEGETABLES, "bag", 7.0, 26.0, "Tender spinach washed and packed");
        createOffer(greenFarm, "Garden lettuce", ProductCategory.VEGETABLES, "head", 4.5, 40.0, "Crisp lettuce heads");
        createOffer(greenFarm, "Radish bunch", ProductCategory.VEGETABLES, "bunch", 4.0, 34.0, "Peppery spring radishes");

        createOffer(orchard, "Apples", ProductCategory.FRUITS, "kg", 5.0, 60.0, "Crisp orchard apples");
        createOffer(orchard, "Pears", ProductCategory.FRUITS, "kg", 7.2, 42.0, "Sweet pears picked this week");
        createOffer(orchard, "Plums", ProductCategory.FRUITS, "kg", 6.0, 38.0, "Ripe plums for dessert or jam");
        createOffer(orchard, "Apple juice", ProductCategory.FRUITS, "bottle", 12.0, 26.0, "Cold pressed apple juice");
        createOffer(orchard, "Berry jam", ProductCategory.OTHERS, "jar", 18.0, 20.0, "Small batch mixed berry jam");
        createOffer(orchard, "Quince box", ProductCategory.FRUITS, "box", 24.0, 12.0, "Aromatic quince selection");
        createOffer(orchard, "Cherry basket", ProductCategory.FRUITS, "basket", 22.0, 18.0, "Sweet cherries from the orchard");
        createOffer(orchard, "Apricot crate", ProductCategory.FRUITS, "crate", 30.0, 10.0, "Soft apricots for jam or snacks");

        createOffer(dairy, "Farm milk", ProductCategory.DAIRY, "bottle", 9.5, 35.0, "Fresh whole milk");
        createOffer(dairy, "Telemea cheese", ProductCategory.DAIRY, "kg", 36.0, 16.0, "Salted sheep cheese");
        createOffer(dairy, "Greek yogurt", ProductCategory.DAIRY, "jar", 10.0, 30.0, "Thick yogurt in glass jars");
        createOffer(dairy, "Butter", ProductCategory.DAIRY, "pack", 14.5, 25.0, "Creamy farm butter");
        createOffer(dairy, "Fresh eggs", ProductCategory.OTHERS, "tray", 19.0, 24.0, "Tray of 10 free-range eggs");
        createOffer(dairy, "Smoked cheese", ProductCategory.DAIRY, "piece", 21.0, 18.0, "Lightly smoked cheese wheel");
        createOffer(dairy, "Goat cheese", ProductCategory.DAIRY, "piece", 24.0, 16.0, "Soft goat cheese with herbs");
        createOffer(dairy, "Kefir", ProductCategory.DAIRY, "bottle", 8.0, 28.0, "Fresh probiotic kefir");

        createOffer(meadow, "Chicken breast", ProductCategory.MEAT, "kg", 33.0, 20.0, "Fresh free-range chicken");
        createOffer(meadow, "Pork sausages", ProductCategory.MEAT, "kg", 39.0, 14.0, "House recipe sausages");
        createOffer(meadow, "Beef stew pack", ProductCategory.MEAT, "kg", 45.0, 12.0, "Ready cut beef for stew");
        createOffer(meadow, "Sunflower oil", ProductCategory.OTHERS, "bottle", 16.0, 32.0, "Cold pressed sunflower oil");
        createOffer(meadow, "Potatoes", ProductCategory.VEGETABLES, "kg", 3.5, 80.0, "Storage potatoes");
        createOffer(meadow, "Onions", ProductCategory.VEGETABLES, "kg", 3.2, 70.0, "Yellow onions");
        createOffer(meadow, "Pumpkin", ProductCategory.VEGETABLES, "piece", 18.0, 15.0, "Sweet roasting pumpkin");
        createOffer(meadow, "Walnuts", ProductCategory.OTHERS, "bag", 20.0, 22.0, "Shelled walnuts from local trees");

        productRepository.findAll().forEach(product -> {
            if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
                product.setImageUrl(imageFor(product.getCategory()));
                productRepository.save(product);
            }
        });
        createDemoCompletedOrder(customer, greenFarm.vendor());
    }

    private Customer createCustomer(User shopper) {
        Customer customer = new Customer();
        customer.setUser(shopper);
        customer.setPhone("0712345678");
        customer.setAddress("Strada Exemplu 10");
        return customerRepository.save(customer);
    }

    private User user(String email, String fullName, String password, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return user;
    }

    private SeedVendor vendor(
            String email,
            String ownerName,
            String password,
            String farmName,
            String description,
            String locationName,
            String address,
            String city
    ) {
        User vendorUser = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(user(email, ownerName, password, Role.VENDOR)));
        Vendor vendor = vendorRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    Vendor newVendor = new Vendor();
                    newVendor.setUser(vendorUser);
                    newVendor.setFarmName(farmName);
                    newVendor.setDescription(description);
                    return vendorRepository.save(newVendor);
                });
        if (vendor.getDescription() == null || vendor.getDescription().isBlank()) {
            vendor.setDescription(description);
            vendor = vendorRepository.save(vendor);
        }

        Vendor savedVendor = vendor;
        Location location = locationRepository.findByVendorIdOrderByIdAsc(savedVendor.getId()).stream()
                .findFirst()
                .orElseGet(() -> createLocation(savedVendor, locationName, address, city));
        ensurePickupSlots(location);
        return new SeedVendor(savedVendor, location);
    }

    private Location createLocation(Vendor vendor, String locationName, String address, String city) {
        Location location = new Location();
        location.setVendor(vendor);
        location.setName(locationName);
        location.setAddress(address);
        location.setCity(city);
        return locationRepository.save(location);
    }

    private void createOffer(
            SeedVendor seedVendor,
            String name,
            ProductCategory category,
            String unit,
            Double price,
            Double quantity,
            String note
    ) {
        if (productRepository.existsByVendorIdAndNameIgnoreCase(seedVendor.vendor().getId(), name)) {
            return;
        }
        Product product = new Product();
        product.setVendor(seedVendor.vendor());
        product.setName(name);
        product.setCategory(category);
        product.setUnit(unit);
        product.setDescription(note);
        product.setImageUrl(imageFor(category));
        product = productRepository.save(product);

        Stock stock = new Stock();
        stock.setLocation(seedVendor.location());
        stock.setProduct(product);
        stock.setQuantity(quantity);
        stockRepository.save(stock);

        WeeklyOffer offer = new WeeklyOffer();
        offer.setProduct(product);
        offer.setLocation(seedVendor.location());
        offer.setPricePerUnit(price);
        offer.setAvailableQuantity(quantity);
        offer.setValidFrom(LocalDate.now().minusDays(1));
        offer.setValidUntil(LocalDate.now().plusDays(10));
        offer.setNote(note);
        weeklyOfferRepository.save(offer);
    }

    private String imageFor(ProductCategory category) {
        if (category == null) {
            return "/images/products/pantry.png";
        }
        return switch (category) {
            case FRUITS -> "/images/products/fruits.png";
            case DAIRY -> "/images/products/dairy.png";
            case HONEY, OTHERS, MEAT -> "/images/products/pantry.png";
            case VEGETABLES -> "/images/products/vegetables.png";
        };
    }

    private void subscribe(Customer customer, Vendor vendor) {
        if (subscriptionRepository.existsByCustomerUserEmailAndVendorId(customer.getUser().getEmail(), vendor.getId())) {
            return;
        }
        Subscription subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setVendor(vendor);
        subscription.setNotificationsEnabled(true);
        subscriptionRepository.save(subscription);
    }

    private void createDemoCompletedOrder(Customer customer, Vendor vendor) {
        if (userRatingRepository.countByRatedIdAndStatus(vendor.getUser().getId(), RatingStatus.APPROVED) > 0) {
            return;
        }
        WeeklyOffer offer = weeklyOfferRepository.findAll().stream()
                .filter(candidate -> candidate.getProduct().getVendor().getId().equals(vendor.getId()))
                .findFirst()
                .orElse(null);
        if (offer == null) {
            return;
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setLocation(offer.getLocation());
        order.setPickupSlot(pickupSlotRepository.findByLocationIdOrderByStartTimeAsc(offer.getLocation().getId()).stream()
                .findFirst()
                .orElse(null));
        order.setStatus(OrderStatus.PICKED_UP);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setWeeklyOffer(offer);
        item.setQuantity(1.0);
        item.setPriceAtOrder(offer.getPricePerUnit());
        order.setItems(java.util.List.of(item));
        order = orderRepository.save(order);

        UserRating rating = new UserRating();
        rating.setOrder(order);
        rating.setRater(customer.getUser());
        rating.setRated(vendor.getUser());
        rating.setScore(5);
        rating.setComment("Fresh products and easy pickup.");
        rating.setStatus(RatingStatus.APPROVED);
        userRatingRepository.save(rating);
    }

    private void ensurePickupSlots(Location location) {
        if (!pickupSlotRepository.findByLocationIdOrderByStartTimeAsc(location.getId()).isEmpty()) {
            return;
        }
        createPickupSlot(location, 1, 10, 12);
        createPickupSlot(location, 1, 16, 18);
        createPickupSlot(location, 2, 9, 11);
    }

    private void createPickupSlot(Location location, int daysFromNow, int startHour, int endHour) {
        PickupSlot slot = new PickupSlot();
        slot.setLocation(location);
        slot.setStartTime(LocalDateTime.now().plusDays(daysFromNow).withHour(startHour).withMinute(0).withSecond(0).withNano(0));
        slot.setEndTime(LocalDateTime.now().plusDays(daysFromNow).withHour(endHour).withMinute(0).withSecond(0).withNano(0));
        slot.setMaxOrders(12);
        pickupSlotRepository.save(slot);
    }

    private record SeedVendor(Vendor vendor, Location location) {
    }
}
