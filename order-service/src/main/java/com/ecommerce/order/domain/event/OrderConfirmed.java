package com.ecommerce.order.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderConfirmed(UUID orderId, UUID customerId, List<EventLine> lines, Instant occurredAt)
        implements DomainEvent {
}
