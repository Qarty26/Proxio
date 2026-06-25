package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.User;
import com.proxio.backend.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(User user) {
        try {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new CreateOperationException("An account with this email already exists.");
            }

            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            return userRepository.save(user);

        } catch (CreateOperationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create User.");
        }
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " was not found."));
    }

    public User update(Long id, User user) {
        try {
            User existing = getById(id);

            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                existing.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            existing.setEmail(user.getEmail());
            existing.setFullName(user.getFullName());
            existing.setRole(user.getRole());
            existing.setVendor(user.getVendor());
            existing.setCustomer(user.getCustomer());
            existing.setRatingsGiven(user.getRatingsGiven());
            existing.setRatingsReceived(user.getRatingsReceived());
            return userRepository.save(existing);

        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update User with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            User existing = getById(id);
            userRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete User with id " + id + ".");
        }
    }
}
