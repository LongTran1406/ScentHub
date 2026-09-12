package com.ecommerce.inventory.event.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderCreatedPayload {

    private String orderId;
    private String customerId;
    private List<OrderItemEventDto> items;
    private BigDecimal totalAmount;

    public OrderCreatedPayload() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public List<OrderItemEventDto> getItems() { return items; }
    public void setItems(List<OrderItemEventDto> items) { this.items = items; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
