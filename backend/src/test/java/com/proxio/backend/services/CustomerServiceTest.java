package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Customer;
import com.proxio.backend.models.User;
import com.proxio.backend.models.security.SecurityUser;
import com.proxio.backend.repositories.CustomerRepository;
import com.proxio.backend.repositories.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CustomerService customerService;

    // ── Auth helper ───────────────────────────────────────────────────────────

    private Authentication authAs(Long userId) {
        Authentication auth = mock(Authentication.class);
        SecurityUser su = mock(SecurityUser.class);
        when(su.getId()).thenReturn(userId);
        when(auth.getPrincipal()).thenReturn(su);
        return auth;
    }

    // ── Basic CRUD ────────────────────────────────────────────────────────────

    @Test
    void createReturnsSavedEntity() {
        Customer entity = new Customer();
        entity.setPhone("0700000000");
        when(customerRepository.save(entity)).thenReturn(entity);
        assertSame(entity, customerService.create(entity));
        verify(customerRepository).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Customer entity = new Customer();
        when(customerRepository.save(entity)).thenThrow(new RuntimeException());
        assertThrows(CreateOperationException.class, () -> customerService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        when(customerRepository.findAll()).thenReturn(List.of(new Customer(), new Customer()));
        assertEquals(2, customerService.getAll().size());
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Customer entity = new Customer();
        entity.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(entity));
        assertEquals(1L, customerService.getById(1L).getId());
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Customer existing = new Customer(); existing.setId(1L); existing.setPhone("0700000000");
        Customer updated = new Customer(); updated.setPhone("0711111111");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);
        assertEquals("0711111111", customerService.update(1L, updated).getPhone());
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerService.update(1L, new Customer()));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Customer entity = new Customer(); entity.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(entity));
        customerService.delete(1L);
        verify(customerRepository).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerService.delete(1L));
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Customer entity = new Customer(); entity.setId(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(customerRepository).delete(entity);
        assertThrows(DeleteOperationException.class, () -> customerService.delete(1L));
    }

    // ── findByUserId ──────────────────────────────────────────────────────────

    @Test
    void findByUserIdWhenCustomerExistsReturnsCustomer() {
        Customer customer = new Customer(); customer.setId(1L);
        when(customerRepository.findByUserId(5L)).thenReturn(Optional.of(customer));
        Optional<Customer> result = customerService.findByUserId(5L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void findByUserIdWhenNoCustomerReturnsEmpty() {
        when(customerRepository.findByUserId(5L)).thenReturn(Optional.empty());
        assertTrue(customerService.findByUserId(5L).isEmpty());
    }

    // ── findOrCreateForCurrentUser ────────────────────────────────────────────

    @Test
    void findOrCreateWhenCustomerExistsReturnsExisting() {
        Customer existing = new Customer(); existing.setId(42L);
        when(customerRepository.findByUserId(5L)).thenReturn(Optional.of(existing));

        Customer result = customerService.findOrCreateForCurrentUser(authAs(5L));

        assertEquals(42L, result.getId());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void findOrCreateWhenNoCustomerCreatesAndReturnsNew() {
        User user = new User(); user.setId(5L);
        Customer saved = new Customer(); saved.setId(1L);

        when(customerRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(customerRepository.save(any())).thenReturn(saved);

        Customer result = customerService.findOrCreateForCurrentUser(authAs(5L));

        assertEquals(1L, result.getId());
        verify(customerRepository).save(any(Customer.class));
    }
}
