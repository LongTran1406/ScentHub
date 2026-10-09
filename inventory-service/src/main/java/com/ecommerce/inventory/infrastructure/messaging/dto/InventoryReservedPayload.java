package com.ecommerce.inventory.infrastructure.messaging.dto;

import java.util.List;

public class InventoryReservedPayload {

    private String orderId;
    private List<OrderItemEventDto> items;

    public InventoryReservedPayload() {}

    public InventoryReservedPayload(String orderId, List<OrderItemEventDto> items) {
        this.orderId = orderId;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public List<OrderItemEventDto> getItems() { return items; }
    public void setItems(List<OrderItemEventDto> items) { this.items = items; }
}
