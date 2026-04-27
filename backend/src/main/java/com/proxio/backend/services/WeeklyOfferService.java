package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.WeeklyOffer;
import com.proxio.backend.repositories.WeeklyOfferRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WeeklyOfferService {

    private final WeeklyOfferRepository weeklyOfferRepository;

    public WeeklyOfferService(WeeklyOfferRepository weeklyOfferRepository) {
        this.weeklyOfferRepository = weeklyOfferRepository;
    }

    public WeeklyOffer create(WeeklyOffer weeklyOffer) {
        try {
            return weeklyOfferRepository.save(weeklyOffer);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create WeeklyOffer.");
        }
    }

    public List<WeeklyOffer> getAll() {
        return weeklyOfferRepository.findAll();
    }

    public WeeklyOffer getById(Long id) {
        return weeklyOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WeeklyOffer with id " + id + " was not found."));
    }

    public WeeklyOffer update(Long id, WeeklyOffer weeklyOffer) {
        try {
            WeeklyOffer existing = getById(id);
        existing.setProduct(weeklyOffer.getProduct());
        existing.setLocation(weeklyOffer.getLocation());
        existing.setPricePerUnit(weeklyOffer.getPricePerUnit());
        existing.setAvailableQuantity(weeklyOffer.getAvailableQuantity());
        existing.setValidFrom(weeklyOffer.getValidFrom());
        existing.setValidUntil(weeklyOffer.getValidUntil());
        existing.setNote(weeklyOffer.getNote());
        existing.setOrderItems(weeklyOffer.getOrderItems());
            return weeklyOfferRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update WeeklyOffer with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            WeeklyOffer existing = getById(id);
            weeklyOfferRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete WeeklyOffer with id " + id + ".");
        }
    }
}
