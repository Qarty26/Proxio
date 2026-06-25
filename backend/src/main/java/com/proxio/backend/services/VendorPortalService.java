package com.proxio.backend.services;

import com.proxio.backend.controllers.forms.VendorProductOfferForm;
import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Location;
import com.proxio.backend.models.PickupSlot;
import com.proxio.backend.models.Product;
import com.proxio.backend.models.Stock;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.WeeklyOffer;
import com.proxio.backend.repositories.LocationRepository;
import com.proxio.backend.repositories.PickupSlotRepository;
import com.proxio.backend.repositories.ProductRepository;
import com.proxio.backend.repositories.StockRepository;
import com.proxio.backend.repositories.VendorRepository;
import com.proxio.backend.repositories.WeeklyOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class VendorPortalService {

    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final StockRepository stockRepository;
    private final WeeklyOfferRepository weeklyOfferRepository;
    private final PickupSlotRepository pickupSlotRepository;

    public VendorPortalService(
            VendorRepository vendorRepository,
            ProductRepository productRepository,
            LocationRepository locationRepository,
            StockRepository stockRepository,
            WeeklyOfferRepository weeklyOfferRepository,
            PickupSlotRepository pickupSlotRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.stockRepository = stockRepository;
        this.weeklyOfferRepository = weeklyOfferRepository;
        this.pickupSlotRepository = pickupSlotRepository;
    }

    public Vendor vendorFor(String email) {
        return vendorRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile was not found for this account."));
    }

    public List<Product> productsFor(String email) {
        return productRepository.findByVendorUserEmailOrderByNameAsc(email);
    }

    public List<WeeklyOffer> offersFor(String email) {
        return weeklyOfferRepository.findByProductVendorUserEmailOrderByValidUntilDesc(email);
    }

    @Transactional
    public WeeklyOffer publishProductOffer(String email, VendorProductOfferForm form, MultipartFile image) {
        Vendor vendor = vendorFor(email);
        if (form.getOfferQuantity() > form.getStockQuantity()) {
            throw new CreateOperationException("Offer quantity cannot be greater than stock quantity.");
        }

        Location location = findOrCreateLocation(vendor, form);

        Product product = new Product();
        product.setVendor(vendor);
        product.setName(form.getName());
        product.setDescription(form.getDescription());
        product.setUnit(form.getUnit());
        product.setCategory(form.getCategory());
        product.setImageUrl(storeImage(image, form));
        product = productRepository.save(product);

        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setLocation(location);
        stock.setQuantity(form.getStockQuantity());
        stockRepository.save(stock);

        WeeklyOffer offer = new WeeklyOffer();
        offer.setProduct(product);
        offer.setLocation(location);
        offer.setPricePerUnit(form.getPricePerUnit());
        offer.setAvailableQuantity(form.getOfferQuantity());
        offer.setValidFrom(LocalDate.now());
        offer.setValidUntil(form.getValidUntil());
        offer.setNote(form.getNote());
        WeeklyOffer saved = weeklyOfferRepository.save(offer);

        ensurePickupSlots(location);
        return saved;
    }

    public WeeklyOffer publishProductOffer(String email, VendorProductOfferForm form) {
        return publishProductOffer(email, form, null);
    }

    private Location findOrCreateLocation(Vendor vendor, VendorProductOfferForm form) {
        if (vendor.getLocations() != null && !vendor.getLocations().isEmpty()) {
            return vendor.getLocations().get(0);
        }

        Location location = new Location();
        location.setVendor(vendor);
        location.setName(form.getLocationName());
        location.setCity(form.getCity());
        location.setAddress(form.getLocationName());
        return locationRepository.save(location);
    }

    private void ensurePickupSlots(Location location) {
        if (!pickupSlotRepository.findByLocationIdOrderByStartTimeAsc(location.getId()).isEmpty()) {
            return;
        }
        for (int day = 1; day <= 3; day++) {
            PickupSlot slot = new PickupSlot();
            slot.setLocation(location);
            slot.setStartTime(LocalDateTime.now().plusDays(day).withHour(10).withMinute(0).withSecond(0).withNano(0));
            slot.setEndTime(LocalDateTime.now().plusDays(day).withHour(12).withMinute(0).withSecond(0).withNano(0));
            slot.setMaxOrders(12);
            pickupSlotRepository.save(slot);
        }
    }

    private String imageFor(com.proxio.backend.models.enums.ProductCategory category) {
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

    private String storeImage(MultipartFile image, VendorProductOfferForm form) {
        if (image == null || image.isEmpty()) {
            return imageFor(form.getCategory());
        }
        String originalName = image.getOriginalFilename() == null ? "" : image.getOriginalFilename();
        String extension = extension(originalName);
        if (!Set.of(".jpg", ".jpeg", ".png", ".webp").contains(extension)) {
            throw new CreateOperationException("Product image must be JPG, PNG or WEBP.");
        }
        try {
            Path directory = Path.of("uploads", "products").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + extension;
            Path destination = directory.resolve(filename);
            Files.copy(image.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + filename;
        } catch (IOException exception) {
            throw new CreateOperationException("Could not store product image.");
        }
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }
}
