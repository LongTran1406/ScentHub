package com.ecommerce.inventory.domain.model;

import com.ecommerce.inventory.domain.exception.InvalidProductException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root of the Inventory bounded context: a product together with its stock counters.
 *
 * <p>Invariants enforced here: a product has a name; price and stock are never negative; reserved stock is never
 * negative and never exceeds the stock on hand. "Available" is always derived (stock - reserved), never stored.
 *
 * <p>Concurrency: {@code version} gives optimistic locking, so two orders racing for the last unit cannot both
 * succeed — the loser fails at commit and the message is retried against the up-to-date stock.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false))
    private Money price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
        // for JPA
    }

    // ---------------------------------------------------------------- factory

    public static Product create(String name, String description, Money price, Integer stockQuantity) {
        Product product = new Product();
        product.id = UUID.randomUUID();
        product.name = requireName(name);
        product.description = description;
        product.price = requirePrice(price);
        product.stockQuantity = requireStock(stockQuantity);
        product.reservedQuantity = 0;
        product.createdAt = Instant.now();
        product.updatedAt = product.createdAt;
        return product;
    }

    // ---------------------------------------------------------------- behaviour

    /** Partial update: only non-null arguments are applied. */
    public void update(String name, String description, Money price, Integer stockQuantity) {
        if (name != null) {
            this.name = requireName(name);
        }
        if (description != null) {
            this.description = description;
        }
        if (price != null) {
            this.price = price;
        }
        if (stockQuantity != null) {
            int newStock = requireStock(stockQuantity);
            if (newStock < reservedQuantity) {
                throw new InvalidProductException("Stock cannot be lowered to " + newStock
                        + ": " + reservedQuantity + " unit(s) are reserved for orders");
            }
            this.stockQuantity = newStock;
        }
        touch();
    }

    public int availableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    public boolean canReserve(int quantity) {
        return quantity > 0 && availableQuantity() >= quantity;
    }

    /** Sets aside stock for an order. Callers check {@link #canReserve(int)} first (see StockReservationService). */
    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new InvalidProductException("Cannot reserve " + quantity + " of product " + id
                    + ": only " + availableQuantity() + " available");
        }
        reservedQuantity += quantity;
        touch();
    }

    /**
     * Gives reserved stock back (the order was cancelled). Never goes below zero.
     *
     * @return true if the counter had to be clamped to zero, which signals inconsistent data upstream
     */
    public boolean release(int quantity) {
        boolean clamped = quantity > reservedQuantity;
        reservedQuantity = Math.max(0, reservedQuantity - Math.max(0, quantity));
        touch();
        return clamped;
    }

    // ---------------------------------------------------------------- accessors

    public UUID getId() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public Money getPrice() { return price; }

    public int getStockQuantity() { return stockQuantity; }

    public int getReservedQuantity() { return reservedQuantity; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidProductException("Product name is required");
        }
        return name.trim();
    }

    private static Money requirePrice(Money price) {
        if (price == null) {
            throw new InvalidProductException("Product price is required");
        }
        return price;
    }

    private static int requireStock(Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new InvalidProductException("Stock quantity must be zero or positive");
        }
        return stockQuantity;
    }
}
