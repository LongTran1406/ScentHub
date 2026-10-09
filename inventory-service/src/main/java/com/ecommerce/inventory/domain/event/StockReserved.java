package com.ecommerce.inventory.domain.event;

import java.util.List;

public record StockReserved(String orderId, List<StockLine> lines) implements InventoryEvent {
}
