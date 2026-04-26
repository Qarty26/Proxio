package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.Subscription;
import com.proxio.backend.repositories.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Subscription create(Subscription subscription) {
        try {
            return subscriptionRepository.save(subscription);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Subscription.");
        }
    }

    public List<Subscription> getAll() {
        return subscriptionRepository.findAll();
    }

    public Subscription getById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription with id " + id + " was not found."));
    }

    public Subscription update(Long id, Subscription subscription) {
        try {
            Subscription existing = getById(id);
        existing.setCustomer(subscription.getCustomer());
        existing.setVendor(subscription.getVendor());
        existing.setNotificationsEnabled(subscription.getNotificationsEnabled());
            return subscriptionRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Subscription with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Subscription existing = getById(id);
            subscriptionRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Subscription with id " + id + ".");
        }
    }
}
