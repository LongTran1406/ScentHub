package com.ecommerce.order.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Entity inside the {@link Order} aggregate. It has an identity but is only reachable (and only modifiable)
 * through its aggregate root.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price", nullable = false))
    private Money unitPrice;

    protected OrderItem() {
        // for JPA
    }

    OrderItem(Order order, UUID productId, int quantity, Money unitPrice) {
        this.order = order;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getProductId() { return productId; }

    public int getQuantity() { return quantity; }

    public Money getUnitPrice() { return unitPrice; }

    public Money lineTotal() { return unitPrice.times(quantity); }
}
