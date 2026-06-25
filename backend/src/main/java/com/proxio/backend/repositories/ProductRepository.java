package com.proxio.backend.repositories;

import com.proxio.backend.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByVendorUserEmailOrderByNameAsc(String email);

    boolean existsByVendorIdAndNameIgnoreCase(Long vendorId, String name);
}
