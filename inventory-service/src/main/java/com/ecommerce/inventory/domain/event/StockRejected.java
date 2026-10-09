package com.ecommerce.inventory.domain.event;

import java.util.List;

/** Nothing was reserved for this order. {@code reason} is INSUFFICIENT_STOCK or PRODUCT_NOT_FOUND. */
public record StockRejected(String orderId, String reason, List<Shortage> shortages) implements InventoryEvent {
}
