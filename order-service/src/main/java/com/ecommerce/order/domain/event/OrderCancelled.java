package com.ecommerce.order.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCancelled(UUID orderId, List<EventLine> lines, String previousStatus, Instant occurredAt)
        implements DomainEvent {
}
