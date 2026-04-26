package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.Order;
import com.proxio.backend.repositories.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order create(Order order) {
        try {
            return orderRepository.save(order);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Order.");
        }
    }

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + id + " was not found."));
    }

    public Order update(Long id, Order order) {
        try {
            Order existing = getById(id);
        existing.setCustomer(order.getCustomer());
        existing.setLocation(order.getLocation());
        existing.setPickupSlot(order.getPickupSlot());
        existing.setStatus(order.getStatus());
        existing.setItems(order.getItems());
        existing.setRatings(order.getRatings());
            return orderRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Order with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Order existing = getById(id);
            orderRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Order with id " + id + ".");
        }
    }
}
