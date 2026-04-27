package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.models.Stock;
import com.proxio.backend.repositories.StockRepository;

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
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void createReturnsSavedEntity() {
        Stock entity = new Stock();
        entity.setQuantity(10.0);

        when(stockRepository.save(entity)).thenReturn(entity);

        Stock result = stockService.create(entity);

        assertSame(entity, result);
        verify(stockRepository, times(1)).save(entity);
    }

    @Test
    void createWhenRepositoryThrowsExceptionThrowsCreateOperationException() {
        Stock entity = new Stock();

        when(stockRepository.save(entity)).thenThrow(new RuntimeException());

        assertThrows(CreateOperationException.class, () -> stockService.create(entity));
    }

    @Test
    void getAllReturnsAllEntities() {
        Stock first = new Stock();
        Stock second = new Stock();

        when(stockRepository.findAll()).thenReturn(List.of(first, second));

        List<Stock> result = stockService.getAll();

        assertEquals(2, result.size());
        verify(stockRepository, times(1)).findAll();
    }

    @Test
    void getByIdWhenEntityExistsReturnsEntity() {
        Stock entity = new Stock();
        entity.setId(1L);

        when(stockRepository.findById(1L)).thenReturn(Optional.of(entity));

        Stock result = stockService.getById(1L);

        assertEquals(1L, result.getId());
        verify(stockRepository, times(1)).findById(1L);
    }

    @Test
    void getByIdWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(stockRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stockService.getById(1L));
    }

    @Test
    void updateWhenEntityExistsUpdatesAndSavesEntity() {
        Stock existing = new Stock();
        existing.setId(1L);
        existing.setQuantity(10.0);

        Stock updated = new Stock();
        updated.setQuantity(20.0);

        when(stockRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(stockRepository.save(existing)).thenReturn(existing);

        Stock result = stockService.update(1L, updated);

        assertEquals(20.0, result.getQuantity());
        verify(stockRepository, times(1)).findById(1L);
        verify(stockRepository, times(1)).save(existing);
    }

    @Test
    void updateWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        Stock updated = new Stock();

        when(stockRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stockService.update(1L, updated));
        verify(stockRepository, never()).save(any());
    }

    @Test
    void deleteWhenEntityExistsDeletesEntity() {
        Stock entity = new Stock();
        entity.setId(1L);

        when(stockRepository.findById(1L)).thenReturn(Optional.of(entity));

        stockService.delete(1L);

        verify(stockRepository, times(1)).findById(1L);
        verify(stockRepository, times(1)).delete(entity);
    }

    @Test
    void deleteWhenEntityDoesNotExistThrowsResourceNotFoundException() {
        when(stockRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stockService.delete(1L));
        verify(stockRepository, never()).delete(any());
    }

    @Test
    void deleteWhenRepositoryThrowsExceptionThrowsDeleteOperationException() {
        Stock entity = new Stock();
        entity.setId(1L);

        when(stockRepository.findById(1L)).thenReturn(Optional.of(entity));
        doThrow(new RuntimeException()).when(stockRepository).delete(entity);

        assertThrows(DeleteOperationException.class, () -> stockService.delete(1L));
    }
}
