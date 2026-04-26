package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.repositories.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public Vendor create(Vendor vendor) {
        try {
            return vendorRepository.save(vendor);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Vendor.");
        }
    }

    public List<Vendor> getAll() {
        return vendorRepository.findAll();
    }

    public Vendor getById(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor with id " + id + " was not found."));
    }

    public Vendor update(Long id, Vendor vendor) {
        try {
            Vendor existing = getById(id);
        existing.setUser(vendor.getUser());
        existing.setFarmName(vendor.getFarmName());
        existing.setDescription(vendor.getDescription());
        existing.setProfileImageUrl(vendor.getProfileImageUrl());
        existing.setLocations(vendor.getLocations());
        existing.setProducts(vendor.getProducts());
        existing.setSubscriptions(vendor.getSubscriptions());
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
}
