package com.ecommerce.order.domain.model;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.event.EventLine;
import com.ecommerce.order.domain.event.OrderCancelled;
import com.ecommerce.order.domain.event.OrderConfirmed;
import com.ecommerce.order.domain.event.OrderPlaced;
import com.ecommerce.order.domain.event.OrderRejected;
import com.ecommerce.order.domain.exception.InvalidOrderException;
import com.ecommerce.order.domain.exception.OrderCancellationNotAllowedException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root of the Order bounded context. All changes to an order and its {@link OrderItem}s go through
 * this class, which enforces the invariants (valid lines, legal status transitions) and records the
 * {@link DomainEvent}s that the application service publishes after the change is saved.
 *
 * <p>Concurrency: {@code version} gives optimistic locking, so two simultaneous changes to the same order cannot
 * silently overwrite each other.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false))
    private Money totalAmount;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected Order() {
        // for JPA
    }

    // ---------------------------------------------------------------- factory

    /** Creates a new PENDING order from already-priced lines and records an {@link OrderPlaced} event. */
    public static Order place(UUID customerId, List<OrderLineDraft> lines) {
        if (customerId == null) {
            throw new InvalidOrderException("customerId is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOrderException("An order needs at least one item");
        }

        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.status = OrderStatus.PENDING;
        order.createdAt = Instant.now();
        order.updatedAt = order.createdAt;

        Money total = Money.ZERO;
        for (OrderLineDraft line : lines) {
            if (line.productId() == null) {
                throw new InvalidOrderException("Every item needs a productId");
            }
            if (line.quantity() <= 0) {
                throw new InvalidOrderException("Quantity must be positive for product " + line.productId());
            }
            OrderItem item = new OrderItem(order, line.productId(), line.quantity(), line.unitPrice());
            order.items.add(item);
            total = total.plus(item.lineTotal());
        }
        order.totalAmount = total;

        order.domainEvents.add(new OrderPlaced(order.id, customerId, order.eventLines(), total, order.createdAt));
        return order;
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * Inventory reserved the stock. Only a PENDING order can be confirmed; otherwise (for example it was
     * cancelled while waiting for inventory) the late reply is ignored.
     *
     * @return true if the order changed
     */
    public boolean confirm() {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        status = OrderStatus.CONFIRMED;
        touch();
        domainEvents.add(new OrderConfirmed(id, customerId, eventLines(), updatedAt));
        return true;
    }

    /**
     * Inventory could not reserve the stock. Only a PENDING order can be rejected.
     *
     * @return true if the order changed
     */
    public boolean reject(String reason) {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        status = OrderStatus.REJECTED;
        rejectionReason = reason;
        touch();
        domainEvents.add(new OrderRejected(id, reason, updatedAt));
        return true;
    }

    /** Orders that are PENDING or CONFIRMED can still be cancelled; everything else is final or in transit. */
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    /** @throws OrderCancellationNotAllowedException if the order is not in a cancellable status */
    public void cancel() {
        if (!isCancellable()) {
            throw new OrderCancellationNotAllowedException(
                    "Cannot cancel order " + id + ": already " + status);
        }
        OrderStatus previous = status;
        status = OrderStatus.CANCELLED;
        touch();
        domainEvents.add(new OrderCancelled(id, eventLines(), previous.name(), updatedAt));
    }

    // ---------------------------------------------------------------- events

    /** Returns the events raised since the last call and clears them. */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    // ---------------------------------------------------------------- accessors

    public UUID getId() { return id; }

    public UUID getCustomerId() { return customerId; }

    public OrderStatus getStatus() { return status; }

    public Money getTotalAmount() { return totalAmount; }

    public String getRejectionReason() { return rejectionReason; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }

    private void touch() {
        updatedAt = Instant.now();
    }

    private List<EventLine> eventLines() {
        return items.stream().map(i -> new EventLine(i.getProductId(), i.getQuantity())).toList();
    }
}
