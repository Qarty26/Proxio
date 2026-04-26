package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Order;
import com.proxio.backend.repositories.OrderRepository;
import com.proxio.backend.models.enums.OrderStatus;

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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createReturnsSavedEntity() {
        Order entity = new Order();
        entity.setStatus(OrderStatus.PENDING);

        when(orderRepository.save(entity)).thenReturn(entity);

        Order result = orderService.create(entity);

        assertSame(entity, result);
        verify(orderRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Order entity = new Order();

        when(orderRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> orderService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        Order first = new Order();
        Order second = new Order();

        when(orderRepository.findAll()).thenReturn(List.of(first, second));

        List<Order> result = orderService.getAll();

        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Order entity = new Order();
        entity.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(entity));

        Order result = orderService.getById(1L);

        assertEquals(1L, result.getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Order existing = new Order();
        existing.setId(1L);
        existing.setStatus(OrderStatus.PENDING);

        Order updated = new Order();
        updated.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(existing);

        Order result = orderService.update(1L, updated);

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        Order updated = new Order();

        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.update(1L, updated));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Order entity = new Order();
        entity.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(entity));

        orderService.delete(1L);

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.delete(1L));
        verify(orderRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Order entity = new Order();
        entity.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(orderRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> orderService.delete(1L));
    }
}
