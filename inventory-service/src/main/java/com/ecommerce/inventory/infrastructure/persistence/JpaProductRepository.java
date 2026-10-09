package com.ecommerce.inventory.infrastructure.persistence;

import com.ecommerce.inventory.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data repository (technical detail; the domain only knows the ProductRepository port). */
interface JpaProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT p FROM Product p WHERE (p.stockQuantity - p.reservedQuantity) < :threshold")
    List<Product> findLowStock(@Param("threshold") int threshold);
}
