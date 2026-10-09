package com.ecommerce.inventory.domain.event;

import java.util.UUID;

/** Value object: a quantity of one product, as requested by (or reserved for) an order. */
public record StockLine(UUID productId, int quantity) {
}
