package com.ecommerce.shipping.infrastructure.messaging.dto;

public class OrderItemEventDto {
    private String productId;
    private Integer quantity;

    public OrderItemEventDto() {}

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
