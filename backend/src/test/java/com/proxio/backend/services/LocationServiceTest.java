package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Location;
import com.proxio.backend.models.User;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.security.SecurityUser;
import com.proxio.backend.repositories.LocationRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocationServiceTest {

    @Mock private LocationRepository locationRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private LocationService locationService;

    // ── Auth helper ───────────────────────────────────────────────────────────

    private Authentication authAs(Long userId, String role) {
        Authentication auth = mock(Authentication.class);
        SecurityUser su = mock(SecurityUser.class);
        when(su.getId()).thenReturn(userId);
        when(auth.getPrincipal()).thenReturn(su);
        when(auth.isAuthenticated()).thenReturn(true);
        doReturn(List.of((GrantedAuthority) () -> role)).when(auth).getAuthorities();
        return auth;
    }

    // ── Basic CRUD (no-auth overloads) ────────────────────────────────────────

    @Test
    void createReturnsSavedEntity() {
        Location entity = new Location(); entity.setName("Old Location");
        when(locationRepository.save(entity)).thenReturn(entity);
        assertSame(entity, locationService.create(entity));
        verify(locationRepository).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Location entity = new Location();
        when(locationRepository.save(entity)).thenThrow(new RuntimeException());
        assertThrows(CreateOperationException.class, () -> locationService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        when(locationRepository.findAll()).thenReturn(List.of(new Location(), new Location()));
        assertEquals(2, locationService.getAll().size());
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Location entity = new Location(); entity.setId(1L);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(entity));
        assertEquals(1L, locationService.getById(1L).getId());
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(locationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> locationService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Location existing = new Location(); existing.setId(1L); existing.setName("Old Location");
        Location updated = new Location(); updated.setName("New Location");
        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(locationRepository.save(existing)).thenReturn(existing);
        assertEquals("New Location", locationService.update(1L, updated).getName());
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(locationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> locationService.update(1L, new Location()));
        verify(locationRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Location entity = new Location(); entity.setId(1L);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(entity));
        locationService.delete(1L);
        verify(locationRepository).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(locationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> locationService.delete(1L));
        verify(locationRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Location entity = new Location(); entity.setId(1L);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(locationRepository).delete(entity);
        assertThrows(DeleteOperationException.class, () -> locationService.delete(1L));
    }

    // ── getAll(Authentication) ────────────────────────────────────────────────

    @Test
    void getAllWithVendorAuthReturnsOnlyVendorLocations() {
        Vendor vendor = new Vendor(); vendor.setId(10L);
        Location loc = new Location(); loc.setId(1L);

        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));
        when(locationRepository.findByVendorId(10L)).thenReturn(List.of(loc));

        List<Location> result = locationService.getAll(authAs(3L, "ROLE_VENDOR"));

        assertEquals(1, result.size());
        verify(locationRepository, never()).findAll();
    }

    @Test
    void getAllWithAdminAuthReturnsAllLocations() {
        when(locationRepository.findAll()).thenReturn(List.of(new Location(), new Location()));

        List<Location> result = locationService.getAll(authAs(1L, "ROLE_ADMIN"));

        assertEquals(2, result.size());
        verify(locationRepository).findAll();
    }

    // ── create(Location, Authentication) ─────────────────────────────────────

    @Test
    void createWithAuthSetsVendorAndSaves() {
        User user = new User(); user.setId(3L);
        Vendor vendor = new Vendor(); vendor.setId(10L); vendor.setUser(user);
        Location location = new Location(); location.setName("Farm Stand");
        Location saved = new Location(); saved.setId(1L);

        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));
        when(locationRepository.save(any())).thenReturn(saved);

        Location result = locationService.create(location, authAs(3L, "ROLE_VENDOR"));

        assertEquals(1L, result.getId());
        assertEquals(vendor, location.getVendor());
        verify(locationRepository).save(location);
    }

    // ── update(Long, Location, Authentication) ────────────────────────────────

    @Test
    void updateWithAuthWhenVendorOwnsLocationUpdatesAndSaves() {
        User user = new User(); user.setId(3L);
        Vendor vendor = new Vendor(); vendor.setId(10L); vendor.setUser(user);
        Location existing = new Location(); existing.setId(1L); existing.setName("Old"); existing.setVendor(vendor);
        Location updated = new Location(); updated.setName("New");

        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));
        when(locationRepository.save(existing)).thenReturn(existing);

        Location result = locationService.update(1L, updated, authAs(3L, "ROLE_VENDOR"));

        assertEquals("New", result.getName());
    }

    @Test
    void updateWithAuthWhenVendorDoesNotOwnLocationThrowsAccessDenied() {
        Vendor ownerVendor = new Vendor(); ownerVendor.setId(10L);
        User ownerUser = new User(); ownerUser.setId(3L); ownerVendor.setUser(ownerUser);
        Location existing = new Location(); existing.setId(1L); existing.setVendor(ownerVendor);

        Vendor otherVendor = new Vendor(); otherVendor.setId(99L);
        User otherUser = new User(); otherUser.setId(7L); otherVendor.setUser(otherUser);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vendorRepository.findByUserId(7L)).thenReturn(Optional.of(otherVendor));

        assertThrows(AccessDeniedException.class,
            () -> locationService.update(1L, new Location(), authAs(7L, "ROLE_VENDOR")));
        verify(locationRepository, never()).save(any());
    }

    // ── delete(Long, Authentication) ──────────────────────────────────────────

    @Test
    void deleteWithAuthWhenVendorOwnsLocationDeletes() {
        User user = new User(); user.setId(3L);
        Vendor vendor = new Vendor(); vendor.setId(10L); vendor.setUser(user);
        Location existing = new Location(); existing.setId(1L); existing.setVendor(vendor);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vendorRepository.findByUserId(3L)).thenReturn(Optional.of(vendor));

        locationService.delete(1L, authAs(3L, "ROLE_VENDOR"));

        verify(locationRepository).delete(existing);
    }

    @Test
    void deleteWithAuthWhenVendorDoesNotOwnLocationThrowsAccessDenied() {
        Vendor ownerVendor = new Vendor(); ownerVendor.setId(10L);
        User ownerUser = new User(); ownerUser.setId(3L); ownerVendor.setUser(ownerUser);
        Location existing = new Location(); existing.setId(1L); existing.setVendor(ownerVendor);

        Vendor otherVendor = new Vendor(); otherVendor.setId(99L);
        User otherUser = new User(); otherUser.setId(7L); otherVendor.setUser(otherUser);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vendorRepository.findByUserId(7L)).thenReturn(Optional.of(otherVendor));

        assertThrows(AccessDeniedException.class,
            () -> locationService.delete(1L, authAs(7L, "ROLE_VENDOR")));
        verify(locationRepository, never()).delete(any());
    }

    @Test
    void deleteWithAdminAuthDeletesRegardlessOfOwnership() {
        Vendor ownerVendor = new Vendor(); ownerVendor.setId(10L);
        Location existing = new Location(); existing.setId(1L); existing.setVendor(ownerVendor);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));

        locationService.delete(1L, authAs(1L, "ROLE_ADMIN"));

        verify(locationRepository).delete(existing);
    }
}
