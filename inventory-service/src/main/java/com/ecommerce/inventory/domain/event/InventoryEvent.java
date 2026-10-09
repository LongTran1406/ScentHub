package com.ecommerce.inventory.domain.event;

/** Something that happened to stock that the Order context needs to hear about. */
public sealed interface InventoryEvent permits StockReserved, StockRejected {

    String orderId();
}
