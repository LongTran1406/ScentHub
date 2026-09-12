package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT p FROM Product p WHERE (p.stockQuantity - p.reservedQuantity) < :threshold")
    List<Product> findLowStock(@Param("threshold") int threshold);
}
