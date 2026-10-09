package com.ecommerce.order.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when inventory refuses an order. By design it is NOT sent to Kafka (rejection is a dead end that the
 * customer discovers on the next GET /orders/{id}), but it is still a real fact in the domain.
 */
public record OrderRejected(UUID orderId, String reason, Instant occurredAt) implements DomainEvent {
}
