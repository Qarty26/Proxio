package com.proxio.backend.services;

import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Location;
import com.proxio.backend.models.PickupSlot;
import com.proxio.backend.models.User;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.security.SecurityUser;
import com.proxio.backend.repositories.LocationRepository;
import com.proxio.backend.repositories.PickupSlotRepository;
import com.proxio.backend.repositories.UserRepository;
import com.proxio.backend.repositories.VendorRepository;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PickupSlotServiceTest {

    @Mock private PickupSlotRepository pickupSlotRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private PickupSlotService pickupSlotService;

    // ── Auth helper ───────────────────────────────────────────────────────────

    private Authentication authAs(Long userId, String role) {
        Authentication auth = mock(Authentication.class);
        SecurityUser su = mock(SecurityUser.class);
        when(su.getId()).thenReturn(userId);
        when(auth.getPrincipal()).thenReturn(su);
        doReturn(List.of((GrantedAuthority) () -> role)).when(auth).getAuthorities();
        return auth;
    }

    private Vendor vendorOwnedBy(Long vendorId, Long userId) {
        User user = new User(); user.setId(userId);
        Vendor vendor = new Vendor(); vendor.setId(vendorId); vendor.setUser(user);
        return vendor;
    }

    // ── getAll(Authentication) ────────────────────────────────────────────────

    @Test
    void getAllWithVendorAuthReturnsOnlyVendorSlots() {
        Vendor vendor = vendorOwnedBy(10L, 3L);
        PickupSlot slot = new PickupSlot(); slot.setId(1L);

        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));
        when(pickupSlotRepository.findByLocationVendorId(10L)).thenReturn(List.of(slot));

        List<PickupSlot> result = pickupSlotService.getAll(authAs(3L, "ROLE_VENDOR"));

        assertEquals(1, result.size());
        verify(pickupSlotRepository, never()).findAll();
    }

    @Test
    void getAllWithAdminAuthReturnsAllSlots() {
        when(pickupSlotRepository.findAll()).thenReturn(List.of(new PickupSlot(), new PickupSlot()));

        List<PickupSlot> result = pickupSlotService.getAll(authAs(1L, "ROLE_ADMIN"));

        assertEquals(2, result.size());
        verify(pickupSlotRepository).findAll();
    }

    // ── getByLocation ─────────────────────────────────────────────────────────

    @Test
    void getByLocationReturnsSlotsForThatLocation() {
        PickupSlot slot = new PickupSlot(); slot.setId(1L);
        when(pickupSlotRepository.findByLocationId(5L)).thenReturn(List.of(slot));

        List<PickupSlot> result = pickupSlotService.getByLocation(5L);

        assertEquals(1, result.size());
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getByIdWhenExistsReturnsSlot() {
        PickupSlot slot = new PickupSlot(); slot.setId(1L);
        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(slot));

        assertEquals(1L, pickupSlotService.getById(1L).getId());
    }

    @Test
    void getByIdWhenNotFoundThrowsResourceNotFoundException() {
        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> pickupSlotService.getById(1L));
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void createWhenVendorOwnsLocationSavesSlot() {
        Vendor vendor = vendorOwnedBy(10L, 3L);
        Location location = new Location(); location.setId(5L); location.setVendor(vendor);
        PickupSlot slot = new PickupSlot();
        slot.setStartTime(LocalDateTime.now().plusDays(1));
        slot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        slot.setLocation(new Location() {{ setId(5L); }});

        PickupSlot saved = new PickupSlot(); saved.setId(1L);

        when(locationRepository.findById(5L)).thenReturn(Optional.of(location));
        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));
        when(pickupSlotRepository.save(slot)).thenReturn(saved);

        PickupSlot result = pickupSlotService.create(slot, authAs(3L, "ROLE_VENDOR"));

        assertEquals(1L, result.getId());
        assertEquals(location, slot.getLocation());
    }

    @Test
    void createWhenVendorDoesNotOwnLocationThrowsAccessDenied() {
        Vendor ownerVendor = vendorOwnedBy(10L, 3L);
        Vendor otherVendor = vendorOwnedBy(99L, 7L);
        Location location = new Location(); location.setId(5L); location.setVendor(ownerVendor);
        PickupSlot slot = new PickupSlot(); slot.setLocation(new Location() {{ setId(5L); }});

        when(locationRepository.findById(5L)).thenReturn(Optional.of(location));
        when(vendorRepository.findByUserId(7L)).thenReturn(Optional.of(otherVendor));

        assertThrows(AccessDeniedException.class,
            () -> pickupSlotService.create(slot, authAs(7L, "ROLE_VENDOR")));
        verify(pickupSlotRepository, never()).save(any());
    }

    @Test
    void createWhenLocationNotFoundThrowsResourceNotFoundException() {
        PickupSlot slot = new PickupSlot(); slot.setLocation(new Location() {{ setId(5L); }});
        when(locationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> pickupSlotService.create(slot, authAs(3L, "ROLE_VENDOR")));
    }

    @Test
    void createWithNoLocationThrowsIllegalArgument() {
        PickupSlot slot = new PickupSlot();
        assertThrows(IllegalArgumentException.class,
            () -> pickupSlotService.create(slot, authAs(3L, "ROLE_VENDOR")));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void updateWhenVendorOwnsSlotUpdatesFields() {
        Vendor vendor = vendorOwnedBy(10L, 3L);
        Location location = new Location(); location.setId(5L); location.setVendor(vendor);
        PickupSlot existing = new PickupSlot(); existing.setId(1L); existing.setMaxOrders(5); existing.setLocation(location);
        PickupSlot incoming = new PickupSlot(); incoming.setMaxOrders(10);
        incoming.setStartTime(LocalDateTime.now().plusDays(1));
        incoming.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));
        when(pickupSlotRepository.save(existing)).thenReturn(existing);

        PickupSlot result = pickupSlotService.update(1L, incoming, authAs(3L, "ROLE_VENDOR"));

        assertEquals(10, result.getMaxOrders());
    }

    @Test
    void updateWhenVendorDoesNotOwnSlotThrowsAccessDenied() {
        Vendor ownerVendor = vendorOwnedBy(10L, 3L);
        Vendor otherVendor = vendorOwnedBy(99L, 7L);
        Location location = new Location(); location.setVendor(ownerVendor);
        PickupSlot existing = new PickupSlot(); existing.setId(1L); existing.setLocation(location);

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vendorRepository.findByUserId(7L)).thenReturn(Optional.of(otherVendor));

        assertThrows(AccessDeniedException.class,
            () -> pickupSlotService.update(1L, new PickupSlot(), authAs(7L, "ROLE_VENDOR")));
        verify(pickupSlotRepository, never()).save(any());
    }

    @Test
    void updateWhenSlotNotFoundThrowsResourceNotFoundException() {
        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
            () -> pickupSlotService.update(1L, new PickupSlot(), authAs(3L, "ROLE_VENDOR")));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteWhenVendorOwnsSlotDeletes() {
        Vendor vendor = vendorOwnedBy(10L, 3L);
        Location location = new Location(); location.setVendor(vendor);
        PickupSlot slot = new PickupSlot(); slot.setId(1L); slot.setLocation(location);

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(slot));
        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));

        pickupSlotService.delete(1L, authAs(3L, "ROLE_VENDOR"));

        verify(pickupSlotRepository).delete(slot);
    }

    @Test
    void deleteWhenVendorDoesNotOwnSlotThrowsAccessDenied() {
        Vendor ownerVendor = vendorOwnedBy(10L, 3L);
        Vendor otherVendor = vendorOwnedBy(99L, 7L);
        Location location = new Location(); location.setVendor(ownerVendor);
        PickupSlot slot = new PickupSlot(); slot.setId(1L); slot.setLocation(location);

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(slot));
        when(vendorRepository.findByUserId(7L)).thenReturn(Optional.of(otherVendor));

        assertThrows(AccessDeniedException.class,
            () -> pickupSlotService.delete(1L, authAs(7L, "ROLE_VENDOR")));
        verify(pickupSlotRepository, never()).delete(any());
    }

    @Test
    void deleteWhenSlotNotFoundThrowsResourceNotFoundException() {
        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
            () -> pickupSlotService.delete(1L, authAs(3L, "ROLE_VENDOR")));
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Vendor vendor = vendorOwnedBy(10L, 3L);
        Location location = new Location(); location.setVendor(vendor);
        PickupSlot slot = new PickupSlot(); slot.setId(1L); slot.setLocation(location);

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(slot));
        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));
        doThrow(new RuntimeException()).when(pickupSlotRepository).delete(slot);

        assertThrows(DeleteOperationException.class,
            () -> pickupSlotService.delete(1L, authAs(3L, "ROLE_VENDOR")));
    }

    @Test
    void deleteWithAdminAuthDeletesRegardlessOfOwnership() {
        Vendor ownerVendor = vendorOwnedBy(10L, 3L);
        Location location = new Location(); location.setVendor(ownerVendor);
        PickupSlot slot = new PickupSlot(); slot.setId(1L); slot.setLocation(location);

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(slot));

        pickupSlotService.delete(1L, authAs(1L, "ROLE_ADMIN"));

        verify(pickupSlotRepository).delete(slot);
    }
}
