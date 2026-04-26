package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.repositories.VendorRepository;

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
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private VendorService vendorService;

    @Test
    void createReturnsSavedEntity() {
        Vendor entity = new Vendor();
        entity.setFarmName("Old Farm");

        when(vendorRepository.save(entity)).thenReturn(entity);

        Vendor result = vendorService.create(entity);

        assertSame(entity, result);
        verify(vendorRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Vendor entity = new Vendor();

        when(vendorRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> vendorService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        Vendor first = new Vendor();
        Vendor second = new Vendor();

        when(vendorRepository.findAll()).thenReturn(List.of(first, second));

        List<Vendor> result = vendorService.getAll();

        assertEquals(2, result.size());
        verify(vendorRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Vendor entity = new Vendor();
        entity.setId(1L);

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(entity));

        Vendor result = vendorService.getById(1L);

        assertEquals(1L, result.getId());
        verify(vendorRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vendorService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Vendor existing = new Vendor();
        existing.setId(1L);
        existing.setFarmName("Old Farm");

        Vendor updated = new Vendor();
        updated.setFarmName("New Farm");

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vendorRepository.save(existing)).thenReturn(existing);

        Vendor result = vendorService.update(1L, updated);

        assertEquals("New Farm", result.getFarmName());
        verify(vendorRepository, times(1)).findById(1L);
        verify(vendorRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        Vendor updated = new Vendor();

        when(vendorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vendorService.update(1L, updated));
        verify(vendorRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Vendor entity = new Vendor();
        entity.setId(1L);

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(entity));

        vendorService.delete(1L);

        verify(vendorRepository, times(1)).findById(1L);
        verify(vendorRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vendorService.delete(1L));
        verify(vendorRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Vendor entity = new Vendor();
        entity.setId(1L);

        when(vendorRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(vendorRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> vendorService.delete(1L));
    }
}
