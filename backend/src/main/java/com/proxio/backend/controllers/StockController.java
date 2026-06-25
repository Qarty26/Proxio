package com.proxio.backend.controllers;

import com.proxio.backend.models.Stock;
import com.proxio.backend.services.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public ResponseEntity<Stock> create(@Valid @RequestBody Stock stock) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.create(stock));
    }

    @GetMapping
    public ResponseEntity<List<Stock>> getAll() {
        return ResponseEntity.ok(stockService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stock> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stockService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Stock> update(@PathVariable Long id, @Valid @RequestBody Stock stock) {
        return ResponseEntity.ok(stockService.update(id, stock));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
