package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.Stock;
import com.proxio.backend.repositories.StockRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public Stock create(Stock stock) {
        try {
            return stockRepository.save(stock);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Stock.");
        }
    }

    public List<Stock> getAll() {
        return stockRepository.findAll();
    }

    public List<Stock> getByProductId(Long productId) {
        return stockRepository.findByProductId(productId);
    }

    public Stock getById(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock with id " + id + " was not found."));
    }

    public Stock update(Long id, Stock stock) {
        try {
            Stock existing = getById(id);
        existing.setLocation(stock.getLocation());
        existing.setProduct(stock.getProduct());
        existing.setQuantity(stock.getQuantity());
            return stockRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Stock with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Stock existing = getById(id);
            stockRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Stock with id " + id + ".");
        }
    }
}
