package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.*;
import com.proxio.backend.models.security.SecurityUser;
import com.proxio.backend.repositories.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PickupSlotService {

    private final PickupSlotRepository pickupSlotRepository;
    private final LocationRepository locationRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    public PickupSlotService(PickupSlotRepository pickupSlotRepository,
                             LocationRepository locationRepository,
                             VendorRepository vendorRepository,
                             UserRepository userRepository) {
        this.pickupSlotRepository = pickupSlotRepository;
        this.locationRepository = locationRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
    }

    public List<PickupSlot> getAll(Authentication authentication) {
        Long vendorId = resolveVendorIdFilter(authentication);
        if (vendorId != null) {
            return pickupSlotRepository.findByLocationVendorId(vendorId);
        }
        return pickupSlotRepository.findAll();
    }

    public List<PickupSlot> getByLocation(Long locationId) {
        return pickupSlotRepository.findByLocationId(locationId);
    }

    public PickupSlot getById(Long id) {
        return pickupSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PickupSlot with id " + id + " was not found."));
    }

    public PickupSlot create(PickupSlot pickupSlot, Authentication authentication) {
        if (pickupSlot.getLocation() == null || pickupSlot.getLocation().getId() == null) {
            throw new IllegalArgumentException("A location is required.");
        }
        Location location = locationRepository.findById(pickupSlot.getLocation().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found."));
        assertOwnsLocation(location, authentication);
        pickupSlot.setLocation(location);
        try {
            return pickupSlotRepository.save(pickupSlot);
        } catch (Exception e) {
            throw new CreateOperationException("Could not create PickupSlot.");
        }
    }

    public PickupSlot update(Long id, PickupSlot incoming, Authentication authentication) {
        try {
            PickupSlot existing = getById(id);
            assertOwnsLocation(existing.getLocation(), authentication);
            existing.setStartTime(incoming.getStartTime());
            existing.setEndTime(incoming.getEndTime());
            existing.setMaxOrders(incoming.getMaxOrders());
            return pickupSlotRepository.save(existing);
        } catch (ResourceNotFoundException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new UpdateOperationException("Could not update PickupSlot with id " + id + ".");
        }
    }

    public void delete(Long id, Authentication authentication) {
        try {
            PickupSlot existing = getById(id);
            assertOwnsLocation(existing.getLocation(), authentication);
            pickupSlotRepository.delete(existing);
        } catch (ResourceNotFoundException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new DeleteOperationException("Could not delete PickupSlot with id " + id + ".");
        }
    }

    private void assertOwnsLocation(Location location, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return;
        Vendor vendor = resolveCurrentVendor(authentication);
        if (location.getVendor() == null || !location.getVendor().getId().equals(vendor.getId())) {
            throw new AccessDeniedException("You do not own this location.");
        }
    }

    private Vendor resolveCurrentVendor(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Vendor profile not found."));
    }

    private Long resolveVendorIdFilter(Authentication authentication) {
        boolean isVendor = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_VENDOR".equals(a.getAuthority()));
        if (!isVendor) return null;
        Long userId = resolveUserId(authentication);
        return vendorRepository.findByUserId(userId)
                .map(Vendor::getId)
                .orElse(null);
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
