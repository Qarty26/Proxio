package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.OrderItem;
import com.proxio.backend.repositories.OrderItemRepository;

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
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    void createReturnsSavedEntity() {
        OrderItem entity = new OrderItem();
        entity.setQuantity(1.0);

        when(orderItemRepository.save(entity)).thenReturn(entity);

        OrderItem result = orderItemService.create(entity);

        assertSame(entity, result);
        verify(orderItemRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        OrderItem entity = new OrderItem();

        when(orderItemRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> orderItemService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        OrderItem first = new OrderItem();
        OrderItem second = new OrderItem();

        when(orderItemRepository.findAll()).thenReturn(List.of(first, second));

        List<OrderItem> result = orderItemService.getAll();

        assertEquals(2, result.size());
        verify(orderItemRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        OrderItem entity = new OrderItem();
        entity.setId(1L);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(entity));

        OrderItem result = orderItemService.getById(1L);

        assertEquals(1L, result.getId());
        verify(orderItemRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        OrderItem existing = new OrderItem();
        existing.setId(1L);
        existing.setQuantity(1.0);

        OrderItem updated = new OrderItem();
        updated.setQuantity(2.0);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderItemRepository.save(existing)).thenReturn(existing);

        OrderItem result = orderItemService.update(1L, updated);

        assertEquals(2.0, result.getQuantity());
        verify(orderItemRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        OrderItem updated = new OrderItem();

        when(orderItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.update(1L, updated));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        OrderItem entity = new OrderItem();
        entity.setId(1L);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(entity));

        orderItemService.delete(1L);

        verify(orderItemRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.delete(1L));
        verify(orderItemRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        OrderItem entity = new OrderItem();
        entity.setId(1L);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(orderItemRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> orderItemService.delete(1L));
    }
}
