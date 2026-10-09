package com.ecommerce.order.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that happened to an {@code Order} that other parts of the system may care about. Raised by the
 * aggregate; translated to an integration event (Kafka message) by the infrastructure layer.
 */
public sealed interface DomainEvent
        permits OrderPlaced, OrderConfirmed, OrderCancelled, OrderRejected {

    UUID orderId();

    Instant occurredAt();
}
