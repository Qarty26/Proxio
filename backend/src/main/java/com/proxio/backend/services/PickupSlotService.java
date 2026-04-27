package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.PickupSlot;
import com.proxio.backend.repositories.PickupSlotRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PickupSlotService {

    private final PickupSlotRepository pickupSlotRepository;

    public PickupSlotService(PickupSlotRepository pickupSlotRepository) {
        this.pickupSlotRepository = pickupSlotRepository;
    }

    public PickupSlot create(PickupSlot pickupSlot) {
        try {
            return pickupSlotRepository.save(pickupSlot);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create PickupSlot.");
        }
    }

    public List<PickupSlot> getAll() {
        return pickupSlotRepository.findAll();
    }

    public PickupSlot getById(Long id) {
        return pickupSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PickupSlot with id " + id + " was not found."));
    }

    public PickupSlot update(Long id, PickupSlot pickupSlot) {
        try {
            PickupSlot existing = getById(id);
        existing.setLocation(pickupSlot.getLocation());
        existing.setStartTime(pickupSlot.getStartTime());
        existing.setEndTime(pickupSlot.getEndTime());
        existing.setMaxOrders(pickupSlot.getMaxOrders());
            return pickupSlotRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update PickupSlot with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            PickupSlot existing = getById(id);
            pickupSlotRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete PickupSlot with id " + id + ".");
        }
    }
}
