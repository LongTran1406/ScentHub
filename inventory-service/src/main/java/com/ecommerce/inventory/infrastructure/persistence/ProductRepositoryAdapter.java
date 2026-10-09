package com.ecommerce.inventory.infrastructure.persistence;

import com.ecommerce.inventory.domain.model.Product;
import com.ecommerce.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter: implements the domain's ProductRepository port with Spring Data JPA. */
@Repository
class ProductRepositoryAdapter implements ProductRepository {

    private final JpaProductRepository jpa;

    ProductRepositoryAdapter(JpaProductRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Product save(Product product) {
        return jpa.save(product);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Product> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids);
    }

    @Override
    public List<Product> findAll() {
        return jpa.findAll();
    }

    @Override
    public List<Product> searchByName(String keyword) {
        return jpa.findByNameContainingIgnoreCase(keyword);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public List<Product> findLowStock(int threshold) {
        return jpa.findLowStock(threshold);
    }
}
