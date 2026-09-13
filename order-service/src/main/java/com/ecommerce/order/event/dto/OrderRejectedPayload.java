package com.ecommerce.order.event.dto;

public class OrderRejectedPayload {

    private String orderId;
    private String reason;

    public OrderRejectedPayload() {}

    public OrderRejectedPayload(String orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
