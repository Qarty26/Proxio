package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.User;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.repositories.UserRepository;
import com.proxio.backend.repositories.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    public VendorService(VendorRepository vendorRepository, UserRepository userRepository) {
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
    }

    public Vendor create(Vendor vendor) {
        try {
            vendor.setUser(resolveUser(vendor));
            return vendorRepository.save(vendor);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Vendor. " + exception.getMessage());
        }
    }

    public List<Vendor> getAll() {
        return vendorRepository.findAll();
    }

    public Page<Vendor> getAllPaged(Pageable pageable) {
        return vendorRepository.findAll(pageable);
    }

    public Vendor getById(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor with id " + id + " was not found."));
    }

    public Vendor getByUserId(Long userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor for user id " + userId + " was not found."));
    }

    public Vendor update(Long id, Vendor vendor) {
        try {
            Vendor existing = getById(id);
            existing.setUser(resolveUser(vendor));
            existing.setFarmName(vendor.getFarmName());
            existing.setDescription(vendor.getDescription());
            existing.setProfileImageUrl(vendor.getProfileImageUrl());
            existing.setLocations(vendor.getLocations());
            existing.setProducts(vendor.getProducts());
            existing.setSubscriptions(vendor.getSubscriptions());
            existing.setFavoritedByCustomers(vendor.getFavoritedByCustomers());
            return vendorRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Vendor with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Vendor existing = getById(id);
            vendorRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Vendor with id " + id + ".");
        }
    }

    private User resolveUser(Vendor vendor) {
        if (vendor.getUser() == null || vendor.getUser().getId() == null) {
            throw new ResourceNotFoundException("User is required for Vendor.");
        }

        return userRepository.findById(vendor.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + vendor.getUser().getId() + " was not found."
                ));
    }
}
