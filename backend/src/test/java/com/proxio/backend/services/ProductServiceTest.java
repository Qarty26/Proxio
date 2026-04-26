package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Product;
import com.proxio.backend.repositories.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createReturnsSavedEntity() {
        Product entity = new Product();
        entity.setName("Old Product");

        when(productRepository.save(entity)).thenReturn(entity);

        Product result = productService.create(entity);

        assertSame(entity, result);
        verify(productRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Product entity = new Product();

        when(productRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> productService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        Product first = new Product();
        Product second = new Product();

        when(productRepository.findAll()).thenReturn(List.of(first, second));

        List<Product> result = productService.getAll();

        assertEquals(2, result.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Product entity = new Product();
        entity.setId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(entity));

        Product result = productService.getById(1L);

        assertEquals(1L, result.getId());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setName("Old Product");

        Product updated = new Product();
        updated.setName("New Product");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        Product result = productService.update(1L, updated);

        assertEquals("New Product", result.getName());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        Product updated = new Product();

        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.update(1L, updated));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Product entity = new Product();
        entity.setId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(entity));

        productService.delete(1L);

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.delete(1L));
        verify(productRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Product entity = new Product();
        entity.setId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(productRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> productService.delete(1L));
    }
}
