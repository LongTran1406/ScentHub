package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.entity.Product;
import com.ecommerce.inventory.event.dto.*;
import com.ecommerce.inventory.event.producer.InventoryEventProducer;
import com.ecommerce.inventory.exception.ProductNotFoundException;
import com.ecommerce.inventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final InventoryEventProducer inventoryEventProducer;

    public ProductService(ProductRepository productRepository,
                          InventoryEventProducer inventoryEventProducer) {
        this.productRepository = productRepository;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    // =========================================================================
    // Task 2.1 — CRUD
    // =========================================================================

    /**
     * Create a new product and return its ProductResponse.
     * reservedQuantity starts at 0, so availableQuantity == stockQuantity.
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setReservedQuantity(0);
        Instant now = Instant.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    /**
     * Update only the non-null fields of an existing product.
     */
    @Transactional
    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());

        product.setUpdatedAt(Instant.now());
        return toResponse(productRepository.save(product));
    }

    /**
     * Delete a product by ID.
     */
    @Transactional
    public void deleteProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        productRepository.deleteById(productId);
    }

    /**
     * List all products, or filter by name if searchKeyword is provided.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String searchKeyword) {
        List<Product> products = (searchKeyword == null || searchKeyword.isBlank())
                ? productRepository.findAll()
                : productRepository.findByNameContainingIgnoreCase(searchKeyword);

        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single product by ID.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return toResponse(product);
    }

    // =========================================================================
    // Task 2.2 — Stock Reservation & Release
    // =========================================================================

    /**
     * All-or-nothing stock reservation.
     * If every item has sufficient available stock, reserve all and publish InventoryReservedEvent.
     * If any item is insufficient, reserve nothing and publish InventoryRejectedEvent.
     */
    @Transactional
    public void reserveStock(String orderId, List<OrderItemEventDto> items) {
        List<Product> products = new ArrayList<>();
        List<InventoryUnavailableItemDto> unavailableItems = new ArrayList<>();

        // First pass — validate all items
        for (OrderItemEventDto item : items) {
            UUID productId = UUID.fromString(item.getProductId());
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            products.add(product);

            int available = product.getStockQuantity() - product.getReservedQuantity();
            if (available < item.getQuantity()) {
                unavailableItems.add(new InventoryUnavailableItemDto(
                        item.getProductId(), item.getQuantity(), available));
            }
        }

        if (!unavailableItems.isEmpty()) {
            // Reject — do NOT reserve any items
            log.warn("Insufficient stock for order [{}]. Unavailable items: {}", orderId, unavailableItems.size());
            InventoryRejectedPayload rejected = new InventoryRejectedPayload(
                    orderId, "INSUFFICIENT_STOCK", unavailableItems);
            inventoryEventProducer.publish("InventoryRejectedEvent", rejected);
            return;
        }

        // Second pass — reserve all items
        for (int i = 0; i < items.size(); i++) {
            Product product = products.get(i);
            product.setReservedQuantity(product.getReservedQuantity() + items.get(i).getQuantity());
            product.setUpdatedAt(Instant.now());
            productRepository.save(product);
        }

        InventoryReservedPayload reserved = new InventoryReservedPayload(orderId, items);
        inventoryEventProducer.publish("InventoryReservedEvent", reserved);
        log.debug("Stock reserved for order [{}]", orderId);
    }

    /**
     * Release previously reserved stock on order cancellation.
     * Clamps reservedQuantity to 0 if it would go negative (logs a warning).
     * No event is published after release.
     */
    @Transactional
    public void releaseStock(List<OrderItemEventDto> items) {
        for (OrderItemEventDto item : items) {
            UUID productId = UUID.fromString(item.getProductId());
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            int newReserved = product.getReservedQuantity() - item.getQuantity();
            if (newReserved < 0) {
                log.warn("reservedQuantity would go negative for product [{}]; clamping to 0 — data inconsistency detected.",
                        productId);
                newReserved = 0;
            }
            product.setReservedQuantity(newReserved);
            product.setUpdatedAt(Instant.now());
            productRepository.save(product);
        }
        log.debug("Stock released for {} item(s)", items.size());
    }

    // =========================================================================
    // Task 2.4 — Analytics
    // =========================================================================

    /**
     * Find all products where availableQuantity (stock - reserved) is below the threshold.
     */
    @Transactional(readOnly = true)
    public List<LowStockResponse> findLowStock(int threshold) {
        return productRepository.findLowStock(threshold).stream()
                .map(p -> new LowStockResponse(
                        p.getId(),
                        p.getName(),
                        p.getStockQuantity() - p.getReservedQuantity(),
                        threshold))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStockQuantity(),
                p.getReservedQuantity()
        );
    }
}
