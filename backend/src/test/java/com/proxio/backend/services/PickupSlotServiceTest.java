package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.PickupSlot;
import com.proxio.backend.repositories.PickupSlotRepository;

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
class PickupSlotServiceTest {

    @Mock
    private PickupSlotRepository pickupSlotRepository;

    @InjectMocks
    private PickupSlotService pickupSlotService;

    @Test
    void createReturnsSavedEntity() {
        PickupSlot entity = new PickupSlot();
        entity.setMaxOrders(5);

        when(pickupSlotRepository.save(entity)).thenReturn(entity);

        PickupSlot result = pickupSlotService.create(entity);

        assertSame(entity, result);
        verify(pickupSlotRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        PickupSlot entity = new PickupSlot();

        when(pickupSlotRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> pickupSlotService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        PickupSlot first = new PickupSlot();
        PickupSlot second = new PickupSlot();

        when(pickupSlotRepository.findAll()).thenReturn(List.of(first, second));

        List<PickupSlot> result = pickupSlotService.getAll();

        assertEquals(2, result.size());
        verify(pickupSlotRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        PickupSlot entity = new PickupSlot();
        entity.setId(1L);

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(entity));

        PickupSlot result = pickupSlotService.getById(1L);

        assertEquals(1L, result.getId());
        verify(pickupSlotRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pickupSlotService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        PickupSlot existing = new PickupSlot();
        existing.setId(1L);
        existing.setMaxOrders(5);

        PickupSlot updated = new PickupSlot();
        updated.setMaxOrders(10);

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(pickupSlotRepository.save(existing)).thenReturn(existing);

        PickupSlot result = pickupSlotService.update(1L, updated);

        assertEquals(10, result.getMaxOrders());
        verify(pickupSlotRepository, times(1)).findById(1L);
        verify(pickupSlotRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        PickupSlot updated = new PickupSlot();

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pickupSlotService.update(1L, updated));
        verify(pickupSlotRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        PickupSlot entity = new PickupSlot();
        entity.setId(1L);

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(entity));

        pickupSlotService.delete(1L);

        verify(pickupSlotRepository, times(1)).findById(1L);
        verify(pickupSlotRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pickupSlotService.delete(1L));
        verify(pickupSlotRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        PickupSlot entity = new PickupSlot();
        entity.setId(1L);

        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(pickupSlotRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> pickupSlotService.delete(1L));
    }
}
