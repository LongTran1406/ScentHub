package com.ecommerce.shipping.event.dto;

import java.util.List;

public class OrderConfirmedPayload {
    private String orderId;
    private String customerId;
    private List<OrderItemEventDto> items;

    public OrderConfirmedPayload() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<OrderItemEventDto> getItems() { return items; }
    public void setItems(List<OrderItemEventDto> items) { this.items = items; }
}
