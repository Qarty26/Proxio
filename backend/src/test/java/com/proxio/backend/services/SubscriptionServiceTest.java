package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Subscription;
import com.proxio.backend.repositories.SubscriptionRepository;

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
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void createReturnsSavedEntity() {
        Subscription entity = new Subscription();
        entity.setNotificationsEnabled(true);

        when(subscriptionRepository.save(entity)).thenReturn(entity);

        Subscription result = subscriptionService.create(entity);

        assertSame(entity, result);
        verify(subscriptionRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Subscription entity = new Subscription();

        when(subscriptionRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> subscriptionService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        Subscription first = new Subscription();
        Subscription second = new Subscription();

        when(subscriptionRepository.findAll()).thenReturn(List.of(first, second));

        List<Subscription> result = subscriptionService.getAll();

        assertEquals(2, result.size());
        verify(subscriptionRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Subscription entity = new Subscription();
        entity.setId(1L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(entity));

        Subscription result = subscriptionService.getById(1L);

        assertEquals(1L, result.getId());
        verify(subscriptionRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subscriptionService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Subscription existing = new Subscription();
        existing.setId(1L);
        existing.setNotificationsEnabled(true);

        Subscription updated = new Subscription();
        updated.setNotificationsEnabled(false);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(existing)).thenReturn(existing);

        Subscription result = subscriptionService.update(1L, updated);

        assertEquals(false, result.getNotificationsEnabled());
        verify(subscriptionRepository, times(1)).findById(1L);
        verify(subscriptionRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        Subscription updated = new Subscription();

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subscriptionService.update(1L, updated));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Subscription entity = new Subscription();
        entity.setId(1L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(entity));

        subscriptionService.delete(1L);

        verify(subscriptionRepository, times(1)).findById(1L);
        verify(subscriptionRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subscriptionService.delete(1L));
        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Subscription entity = new Subscription();
        entity.setId(1L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(subscriptionRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> subscriptionService.delete(1L));
    }
}
