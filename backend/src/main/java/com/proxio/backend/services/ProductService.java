package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.Product;
import com.proxio.backend.models.enums.ProductCategory;
import com.proxio.backend.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product create(Product product) {
        try {
            return productRepository.save(product);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Product." + exception.getMessage() );
        }
    }
    public Page<Product> getAllPaged(Pageable pageable) {
            return productRepository.findAll(pageable);
    }
    public Page<Product> getByCategoryPaged(ProductCategory category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable);
    }
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " was not found."));
    }

    public Product update(Long id, Product product) {
        try {
            Product existing = getById(id);
        existing.setVendor(product.getVendor());
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setUnit(product.getUnit());
        existing.setImageUrl(product.getImageUrl());
        existing.setCategory(product.getCategory());
        existing.setStocks(product.getStocks());
        existing.setWeeklyOffers(product.getWeeklyOffers());
            return productRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Product with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Product existing = getById(id);
            productRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Product with id " + id + ".");
        }
    }
}
