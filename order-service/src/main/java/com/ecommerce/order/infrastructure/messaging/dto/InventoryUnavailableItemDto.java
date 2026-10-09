package com.ecommerce.order.infrastructure.messaging.dto;

public class InventoryUnavailableItemDto {

    private String productId;
    private Integer requested;
    private Integer available;

    public InventoryUnavailableItemDto() {}

    public InventoryUnavailableItemDto(String productId, Integer requested, Integer available) {
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Integer getRequested() { return requested; }
    public void setRequested(Integer requested) { this.requested = requested; }

    public Integer getAvailable() { return available; }
    public void setAvailable(Integer available) { this.available = available; }
}
