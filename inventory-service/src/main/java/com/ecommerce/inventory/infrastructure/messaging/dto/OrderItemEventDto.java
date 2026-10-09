package com.ecommerce.inventory.infrastructure.messaging.dto;

public class OrderItemEventDto {

    private String productId;
    private Integer quantity;

    public OrderItemEventDto() {}

    public OrderItemEventDto(String productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
