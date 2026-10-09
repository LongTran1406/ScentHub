package com.ecommerce.shipping.service.dto;

public class UpdateShipmentStatusRequest {
    private String status;

    public UpdateShipmentStatusRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
