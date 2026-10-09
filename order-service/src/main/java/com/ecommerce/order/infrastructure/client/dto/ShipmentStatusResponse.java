package com.ecommerce.order.infrastructure.client.dto;

public class ShipmentStatusResponse {

    private String orderId;
    private String status;

    public ShipmentStatusResponse() {}

    public ShipmentStatusResponse(String orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
