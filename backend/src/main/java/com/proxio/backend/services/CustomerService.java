package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.Customer;
import com.proxio.backend.models.User;
import com.proxio.backend.models.security.SecurityUser;
import com.proxio.backend.repositories.CustomerRepository;
import com.proxio.backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    public Customer create(Customer customer) {
        try {
            return customerRepository.save(customer);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Customer.");
        }
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public Page<Customer> getAllPaged(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " was not found."));
    }

    public Optional<Customer> findByUserId(Long userId) {
        return customerRepository.findByUserId(userId);
    }

    public Customer findOrCreateForCurrentUser(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return customerRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found."));
            Customer customer = new Customer();
            customer.setUser(user);
            return customerRepository.save(customer);
        });
    }

    public Customer update(Long id, Customer customer) {
        try {
            Customer existing = getById(id);
            existing.setUser(customer.getUser());
            existing.setPhone(customer.getPhone());
            existing.setAddress(customer.getAddress());
            existing.setSubscriptions(customer.getSubscriptions());
            existing.setOrders(customer.getOrders());
            existing.setFavoriteVendors(customer.getFavoriteVendors());
            return customerRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Customer with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Customer existing = getById(id);
            customerRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Customer with id " + id + ".");
        }
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getId();
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        return user.getId();
    }
}
