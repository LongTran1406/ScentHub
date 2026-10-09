package com.ecommerce.inventory.domain.repository;

import com.ecommerce.inventory.domain.model.Product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port: how the domain loads and stores {@link Product} aggregates. Implemented in the infrastructure layer. */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAllById(Collection<UUID> ids);

    List<Product> findAll();

    List<Product> searchByName(String keyword);

    boolean existsById(UUID id);

    void deleteById(UUID id);

    /** Products whose available quantity (stock - reserved) is below {@code threshold}. */
    List<Product> findLowStock(int threshold);
}
