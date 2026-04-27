package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.WeeklyOffer;
import com.proxio.backend.repositories.WeeklyOfferRepository;

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
class WeeklyOfferServiceTest {

    @Mock
    private WeeklyOfferRepository weeklyOfferRepository;

    @InjectMocks
    private WeeklyOfferService weeklyOfferService;

    @Test
    void createReturnsSavedEntity() {
        WeeklyOffer entity = new WeeklyOffer();
        entity.setNote("Old Note");

        when(weeklyOfferRepository.save(entity)).thenReturn(entity);

        WeeklyOffer result = weeklyOfferService.create(entity);

        assertSame(entity, result);
        verify(weeklyOfferRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        WeeklyOffer entity = new WeeklyOffer();

        when(weeklyOfferRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> weeklyOfferService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        WeeklyOffer first = new WeeklyOffer();
        WeeklyOffer second = new WeeklyOffer();

        when(weeklyOfferRepository.findAll()).thenReturn(List.of(first, second));

        List<WeeklyOffer> result = weeklyOfferService.getAll();

        assertEquals(2, result.size());
        verify(weeklyOfferRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        WeeklyOffer entity = new WeeklyOffer();
        entity.setId(1L);

        when(weeklyOfferRepository.findById(1L)).thenReturn(Optional.of(entity));

        WeeklyOffer result = weeklyOfferService.getById(1L);

        assertEquals(1L, result.getId());
        verify(weeklyOfferRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(weeklyOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> weeklyOfferService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        WeeklyOffer existing = new WeeklyOffer();
        existing.setId(1L);
        existing.setNote("Old Note");

        WeeklyOffer updated = new WeeklyOffer();
        updated.setNote("New Note");

        when(weeklyOfferRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(weeklyOfferRepository.save(existing)).thenReturn(existing);

        WeeklyOffer result = weeklyOfferService.update(1L, updated);

        assertEquals("New Note", result.getNote());
        verify(weeklyOfferRepository, times(1)).findById(1L);
        verify(weeklyOfferRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        WeeklyOffer updated = new WeeklyOffer();

        when(weeklyOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> weeklyOfferService.update(1L, updated));
        verify(weeklyOfferRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        WeeklyOffer entity = new WeeklyOffer();
        entity.setId(1L);

        when(weeklyOfferRepository.findById(1L)).thenReturn(Optional.of(entity));

        weeklyOfferService.delete(1L);

        verify(weeklyOfferRepository, times(1)).findById(1L);
        verify(weeklyOfferRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(weeklyOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> weeklyOfferService.delete(1L));
        verify(weeklyOfferRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        WeeklyOffer entity = new WeeklyOffer();
        entity.setId(1L);

        when(weeklyOfferRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(weeklyOfferRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> weeklyOfferService.delete(1L));
    }
}
