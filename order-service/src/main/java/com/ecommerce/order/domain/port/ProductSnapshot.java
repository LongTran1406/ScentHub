package com.ecommerce.order.domain.port;

import com.ecommerce.order.domain.model.Money;

import java.util.UUID;

/** Read-only view of a product as the Inventory context currently reports it (we never own this data). */
public record ProductSnapshot(UUID productId, String name, Money price, int availableQuantity) {
}
