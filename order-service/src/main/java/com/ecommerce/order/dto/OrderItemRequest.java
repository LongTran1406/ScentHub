package com.ecommerce.order.dto;

import java.util.UUID;

public class OrderItemRequest {

    private UUID productId;
    private Integer quantity;

    public OrderItemRequest() {}

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
