package com.ecommerce.order.event.dto;

import java.util.List;

public class InventoryRejectedPayload {

    private String orderId;
    private String reason;
    private List<InventoryUnavailableItemDto> unavailableItems;

    public InventoryRejectedPayload() {}

    public InventoryRejectedPayload(String orderId, String reason, List<InventoryUnavailableItemDto> unavailableItems) {
        this.orderId = orderId;
        this.reason = reason;
        this.unavailableItems = unavailableItems;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<InventoryUnavailableItemDto> getUnavailableItems() { return unavailableItems; }
    public void setUnavailableItems(List<InventoryUnavailableItemDto> unavailableItems) {
        this.unavailableItems = unavailableItems;
    }
}
