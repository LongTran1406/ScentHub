package com.ecommerce.shipping.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root of the Shipping bounded context: one shipment per order (orderId is unique).
 *
 * <p>The status can be set to any value by an operator (the project does not model a strict workflow), but a
 * shipment is only auto-cancelled by an order cancellation while it is still CREATED.
 */
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Shipment() {
        // for JPA
    }

    public static Shipment createFor(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        Shipment shipment = new Shipment();
        shipment.id = UUID.randomUUID();
        shipment.orderId = orderId;
        shipment.status = ShipmentStatus.CREATED;
        shipment.createdAt = Instant.now();
        shipment.updatedAt = shipment.createdAt;
        return shipment;
    }

    public void changeStatus(ShipmentStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Called when the order was cancelled. Only a shipment that has not been dispatched yet is cancelled.
     *
     * @return true if the shipment was cancelled, false if it was too late (nothing changes)
     */
    public boolean cancelIfNotDispatched() {
        if (status != ShipmentStatus.CREATED) {
            return false;
        }
        changeStatus(ShipmentStatus.CANCELLED);
        return true;
    }

    public UUID getId() { return id; }

    public UUID getOrderId() { return orderId; }

    public ShipmentStatus getStatus() { return status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
