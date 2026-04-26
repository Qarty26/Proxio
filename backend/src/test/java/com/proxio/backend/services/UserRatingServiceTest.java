package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.UserRating;
import com.proxio.backend.repositories.UserRatingRepository;

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
class UserRatingServiceTest {

    @Mock
    private UserRatingRepository userRatingRepository;

    @InjectMocks
    private UserRatingService userRatingService;

    @Test
    void createReturnsSavedEntity() {
        UserRating entity = new UserRating();
        entity.setScore(1);

        when(userRatingRepository.save(entity)).thenReturn(entity);

        UserRating result = userRatingService.create(entity);

        assertSame(entity, result);
        verify(userRatingRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        UserRating entity = new UserRating();

        when(userRatingRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> userRatingService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        UserRating first = new UserRating();
        UserRating second = new UserRating();

        when(userRatingRepository.findAll()).thenReturn(List.of(first, second));

        List<UserRating> result = userRatingService.getAll();

        assertEquals(2, result.size());
        verify(userRatingRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        UserRating entity = new UserRating();
        entity.setId(1L);

        when(userRatingRepository.findById(1L)).thenReturn(Optional.of(entity));

        UserRating result = userRatingService.getById(1L);

        assertEquals(1L, result.getId());
        verify(userRatingRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(userRatingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userRatingService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        UserRating existing = new UserRating();
        existing.setId(1L);
        existing.setScore(1);

        UserRating updated = new UserRating();
        updated.setScore(5);

        when(userRatingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRatingRepository.save(existing)).thenReturn(existing);

        UserRating result = userRatingService.update(1L, updated);

        assertEquals(5, result.getScore());
        verify(userRatingRepository, times(1)).findById(1L);
        verify(userRatingRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        UserRating updated = new UserRating();

        when(userRatingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userRatingService.update(1L, updated));
        verify(userRatingRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        UserRating entity = new UserRating();
        entity.setId(1L);

        when(userRatingRepository.findById(1L)).thenReturn(Optional.of(entity));

        userRatingService.delete(1L);

        verify(userRatingRepository, times(1)).findById(1L);
        verify(userRatingRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(userRatingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userRatingService.delete(1L));
        verify(userRatingRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        UserRating entity = new UserRating();
        entity.setId(1L);

        when(userRatingRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(userRatingRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> userRatingService.delete(1L));
    }
}
