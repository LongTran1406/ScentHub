package com.ecommerce.order.event.dto;

import java.util.List;

public class OrderCancelledPayload {

    private String orderId;
    private List<OrderItemEventDto> items;
    private String previousStatus;

    public OrderCancelledPayload() {}

    public OrderCancelledPayload(String orderId, List<OrderItemEventDto> items, String previousStatus) {
        this.orderId = orderId;
        this.items = items;
        this.previousStatus = previousStatus;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public List<OrderItemEventDto> getItems() { return items; }
    public void setItems(List<OrderItemEventDto> items) { this.items = items; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
}
