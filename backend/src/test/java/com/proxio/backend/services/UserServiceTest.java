package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.User;
import com.proxio.backend.repositories.UserRepository;
import com.proxio.backend.models.enums.Role;

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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createReturnsSavedEntity() {
        User entity = new User();
        entity.setEmail("old@mail.com");

        when(userRepository.save(entity)).thenReturn(entity);

        User result = userService.create(entity);

        assertSame(entity, result);
        verify(userRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        User entity = new User();

        when(userRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> userService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        User first = new User();
        User second = new User();

        when(userRepository.findAll()).thenReturn(List.of(first, second));

        List<User> result = userService.getAll();

        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        User entity = new User();
        entity.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        User result = userService.getById(1L);

        assertEquals(1L, result.getId());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        User existing = new User();
        existing.setId(1L);
        existing.setEmail("old@mail.com");

        User updated = new User();
        updated.setEmail("new@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = userService.update(1L, updated);

        assertEquals("new@mail.com", result.getEmail());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        User updated = new User();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.update(1L, updated));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        User entity = new User();
        entity.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

        userService.delete(1L);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.delete(1L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        User entity = new User();
        entity.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(userRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> userService.delete(1L));
    }
}
