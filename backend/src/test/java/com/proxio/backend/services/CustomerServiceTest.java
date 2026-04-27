package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Customer;
import com.proxio.backend.repositories.CustomerRepository;

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
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createReturnsSavedEntity() {
        Customer entity = new Customer();
        entity.setPhone("0700000000");

        when(customerRepository.save(entity)).thenReturn(entity);

        Customer result = customerService.create(entity);

        assertSame(entity, result);
        verify(customerRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Customer entity = new Customer();

        when(customerRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> customerService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        Customer first = new Customer();
        Customer second = new Customer();

        when(customerRepository.findAll()).thenReturn(List.of(first, second));

        List<Customer> result = customerService.getAll();

        assertEquals(2, result.size());
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Customer entity = new Customer();
        entity.setId(1L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(entity));

        Customer result = customerService.getById(1L);

        assertEquals(1L, result.getId());
        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Customer existing = new Customer();
        existing.setId(1L);
        existing.setPhone("0700000000");

        Customer updated = new Customer();
        updated.setPhone("0711111111");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);

        Customer result = customerService.update(1L, updated);

        assertEquals("0711111111", result.getPhone());
        verify(customerRepository, times(1)).findById(1L);
        verify(customerRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        Customer updated = new Customer();

        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.update(1L, updated));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Customer entity = new Customer();
        entity.setId(1L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(entity));

        customerService.delete(1L);

        verify(customerRepository, times(1)).findById(1L);
        verify(customerRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.delete(1L));
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Customer entity = new Customer();
        entity.setId(1L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(customerRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> customerService.delete(1L));
    }
}
