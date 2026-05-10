package com.proxio.backend.repositories;

import com.proxio.backend.models.Product;
import com.proxio.backend.models.enums.ProductCategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategory(ProductCategory category, Pageable pageable);
}
