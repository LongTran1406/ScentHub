package com.ecommerce.order.domain.event;

import com.ecommerce.order.domain.model.Money;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlaced(UUID orderId, UUID customerId, List<EventLine> lines, Money totalAmount, Instant occurredAt)
        implements DomainEvent {
}
