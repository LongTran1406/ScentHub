package com.ecommerce.order.domain.event;

import java.util.UUID;

/** Immutable (productId, quantity) pair carried by order domain events. */
public record EventLine(UUID productId, int quantity) {
}
