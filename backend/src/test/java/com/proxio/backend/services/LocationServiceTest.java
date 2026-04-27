package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Location;
import com.proxio.backend.repositories.LocationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationService locationService;

    @Test
    void createReturnsSavedEntity() {
        Location entity = new Location();
        entity.setName("Old Location");

        when(locationRepository.save(entity)).thenReturn(entity);

        Location result = locationService.create(entity);

        assertSame(entity, result);
        verify(locationRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Location entity = new Location();

        when(locationRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> locationService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        Location first = new Location();
        Location second = new Location();

        when(locationRepository.findAll()).thenReturn(List.of(first, second));

        List<Location> result = locationService.getAll();

        assertEquals(2, result.size());
        verify(locationRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Location entity = new Location();
        entity.setId(1L);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(entity));

        Location result = locationService.getById(1L);

        assertEquals(1L, result.getId());
        verify(locationRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(locationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> locationService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Location existing = new Location();
        existing.setId(1L);
        existing.setName("Old Location");

        Location updated = new Location();
        updated.setName("New Location");

        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(locationRepository.save(existing)).thenReturn(existing);

        Location result = locationService.update(1L, updated);

        assertEquals("New Location", result.getName());
        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        Location updated = new Location();

        when(locationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> locationService.update(1L, updated));
        verify(locationRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Location entity = new Location();
        entity.setId(1L);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(entity));

        locationService.delete(1L);

        verify(locationRepository, times(1)).findById(1L);
        verify(locationRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(locationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> locationService.delete(1L));
        verify(locationRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Location entity = new Location();
        entity.setId(1L);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(locationRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> locationService.delete(1L));
    }
}
