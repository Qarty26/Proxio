package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.Location;
import com.proxio.backend.models.User;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.security.SecurityUser;
import com.proxio.backend.repositories.LocationRepository;
import com.proxio.backend.repositories.UserRepository;
import com.proxio.backend.repositories.VendorRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    public LocationService(LocationRepository locationRepository,
                           VendorRepository vendorRepository,
                           UserRepository userRepository) {
        this.locationRepository = locationRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
    }

    public Location create(Location location) {
        try {
            return locationRepository.save(location);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Location.");
        }
    }

    public Location create(Location location, Authentication authentication) {
        try {
            Vendor vendor = resolveCurrentVendor(authentication);
            location.setVendor(vendor);
            return locationRepository.save(location);
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Location.");
        }
    }

    public List<Location> getAll() {
        return locationRepository.findAll();
    }

    public List<Location> getAll(Authentication authentication) {
        Long vendorId = resolveVendorIdFilter(authentication);
        if (vendorId != null) {
            return locationRepository.findByVendorId(vendorId);
        }
        return locationRepository.findAll();
    }

    public Location getById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location with id " + id + " was not found."));
    }

    public Location update(Long id, Location location) {
        try {
            Location existing = getById(id);
            existing.setVendor(location.getVendor());
            existing.setName(location.getName());
            existing.setAddress(location.getAddress());
            existing.setCity(location.getCity());
            existing.setLatitude(location.getLatitude());
            existing.setLongitude(location.getLongitude());
            existing.setStocks(location.getStocks());
            existing.setPickupSlots(location.getPickupSlots());
            existing.setOrders(location.getOrders());
            existing.setWeeklyOffers(location.getWeeklyOffers());
            return locationRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Location with id " + id + ".");
        }
    }

    public Location update(Long id, Location location, Authentication authentication) {
        try {
            Location existing = getById(id);
            assertOwnsLocation(existing, authentication);
            existing.setName(location.getName());
            existing.setAddress(location.getAddress());
            existing.setCity(location.getCity());
            existing.setLatitude(location.getLatitude());
            existing.setLongitude(location.getLongitude());
            return locationRepository.save(existing);
        } catch (ResourceNotFoundException | AccessDeniedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Location with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Location existing = getById(id);
            locationRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Location with id " + id + ".");
        }
    }

    public void delete(Long id, Authentication authentication) {
        try {
            Location existing = getById(id);
            assertOwnsLocation(existing, authentication);
            locationRepository.delete(existing);
        } catch (ResourceNotFoundException | AccessDeniedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Location with id " + id + ".");
        }
    }

    private void assertOwnsLocation(Location location, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return;

        Vendor currentVendor = resolveCurrentVendor(authentication);
        if (!currentVendor.getId().equals(location.getVendor().getId())) {
            throw new AccessDeniedException("Vendors can only modify their own locations.");
        }
    }

    private Vendor resolveCurrentVendor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required.");
        }
        Long userId = resolveUserId(authentication);
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Vendor profile not found for current user."));
    }

    private Long resolveVendorIdFilter(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        boolean isVendor = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_VENDOR".equals(a.getAuthority()));
        if (!isVendor) return null;
        Long userId = resolveUserId(authentication);
        return vendorRepository.findByUserId(userId).map(Vendor::getId).orElse(null);
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getId();
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        return user.getId();
    }
}
