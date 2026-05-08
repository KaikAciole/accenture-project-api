package br.com.accenture.inventory.domain.repository;

import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    PageResult<Product> findByNameContainingIgnoreCase(String name, PageRequest pageRequest);

    PageResult<Product> findAll(PageRequest pageRequest);

    void deleteById(UUID id);
}