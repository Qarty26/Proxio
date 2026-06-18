package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.Product;
import com.proxio.backend.models.User;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.enums.ProductCategory;
import com.proxio.backend.models.security.SecurityUser;
import com.proxio.backend.repositories.ProductRepository;
import com.proxio.backend.repositories.UserRepository;
import com.proxio.backend.repositories.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final ProductCacheService productCacheService;

    public ProductService(ProductRepository productRepository,
                          VendorRepository vendorRepository,
                          UserRepository userRepository,
                          ProductCacheService productCacheService) {
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.productCacheService = productCacheService;
    }

    public Product create(Product product) {
        try {
            product.setVendor(resolveVendor(product));
            Product saved = productRepository.save(product);
            productCacheService.evictProductsSummary();
            return saved;
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Product." + exception.getMessage() );
        }
    }

    public Product create(Product product, Authentication authentication) {
        try {
            Vendor vendor = resolveVendor(product);
            assertCanWriteVendor(vendor.getId(), authentication);
            product.setVendor(vendor);
            Product saved = productRepository.save(product);
            productCacheService.evictProductsSummary();
            return saved;
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create Product." + exception.getMessage());
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
            existing.setVendor(resolveVendor(product));
            existing.setName(product.getName());
            existing.setDescription(product.getDescription());
            existing.setUnit(product.getUnit());
            existing.setImageUrl(product.getImageUrl());
            existing.setCategory(product.getCategory());
            existing.setStocks(product.getStocks());
            existing.setWeeklyOffers(product.getWeeklyOffers());
            Product saved = productRepository.save(existing);
            productCacheService.evictProductsSummary();
            return saved;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Product with id " + id + ".");
        }
    }

    public Product update(Long id, Product product, Authentication authentication) {
        try {
            Product existing = getById(id);
            assertCanWriteVendor(existing.getVendor().getId(), authentication);
            existing.setName(product.getName());
            existing.setDescription(product.getDescription());
            existing.setUnit(product.getUnit());
            existing.setImageUrl(product.getImageUrl());
            existing.setCategory(product.getCategory());
            Product saved = productRepository.save(existing);
            productCacheService.evictProductsSummary();
            return saved;
        } catch (ResourceNotFoundException | AccessDeniedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update Product with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            Product existing = getById(id);
            productRepository.delete(existing);
            productCacheService.evictProductsSummary();
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Product with id " + id + ".");
        }
    }

    public void delete(Long id, Authentication authentication) {
        try {
            Product existing = getById(id);
            assertCanWriteVendor(existing.getVendor().getId(), authentication);
            productRepository.delete(existing);
            productCacheService.evictProductsSummary();
        } catch (ResourceNotFoundException | AccessDeniedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete Product with id " + id + ".");
        }
    }

    private com.proxio.backend.models.Vendor resolveVendor(Product product) {
        if (product.getVendor() == null || product.getVendor().getId() == null) {
            throw new ResourceNotFoundException("Vendor is required for Product.");
        }

        return vendorRepository.findById(product.getVendor().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vendor with id " + product.getVendor().getId() + " was not found."
                ));
    }

    private void assertCanWriteVendor(Long vendorId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required.");
        }

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            return;
        }

        boolean vendorRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_VENDOR".equals(authority.getAuthority()));
        if (!vendorRole) {
            throw new AccessDeniedException("Only vendors can manage products.");
        }

        Long userId = resolveAuthenticatedUserId(authentication);
        Vendor currentVendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Vendor profile was not found for current user."));

        if (!currentVendor.getId().equals(vendorId)) {
            throw new AccessDeniedException("Vendors can manage only their own products.");
        }
    }

    private Long resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getId();
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user was not found."));
        return user.getId();
    }
}
