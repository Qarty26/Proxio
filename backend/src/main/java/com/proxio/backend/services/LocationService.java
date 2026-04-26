package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.Location;
import com.proxio.backend.repositories.LocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Location create(Location location) {
        try {
            return locationRepository.save(location);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Location.");
        }
    }

    public List<Location> getAll() {
        return locationRepository.findAll();
    }

    public Location getById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location with id " + id + " was not found."));
    }

    public Location update(Long id, Location location) {
        try {
            Location existing = getById(id);
        existing.setVendor(location.getVendor());
        existing.setName(location.getName());
        existing.setAddress(location.getAddress());
        existing.setCity(location.getCity());
        existing.setLatitude(location.getLatitude());
        existing.setLongitude(location.getLongitude());
        existing.setStocks(location.getStocks());
        existing.setPickupSlots(location.getPickupSlots());
        existing.setOrders(location.getOrders());
        existing.setWeeklyOffers(location.getWeeklyOffers());
            return locationRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Location with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Location existing = getById(id);
            locationRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Location with id " + id + ".");
        }
    }
}
