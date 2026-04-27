package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.OrderItem;
import com.proxio.backend.repositories.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    public OrderItem create(OrderItem orderItem) {
        try {
            return orderItemRepository.save(orderItem);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create OrderItem.");
        }
    }

    public List<OrderItem> getAll() {
        return orderItemRepository.findAll();
    }

    public OrderItem getById(Long id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem with id " + id + " was not found."));
    }

    public OrderItem update(Long id, OrderItem orderItem) {
        try {
            OrderItem existing = getById(id);
        existing.setOrder(orderItem.getOrder());
        existing.setWeeklyOffer(orderItem.getWeeklyOffer());
        existing.setQuantity(orderItem.getQuantity());
        existing.setPriceAtOrder(orderItem.getPriceAtOrder());
            return orderItemRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update OrderItem with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            OrderItem existing = getById(id);
            orderItemRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete OrderItem with id " + id + ".");
        }
    }
}
