package com.ecommerce.order.infrastructure.client.dto;

import java.math.BigDecimal;

/** JSON shape of Inventory Service's GET /products responses (only the fields this service needs). */
public class InventoryProductResponse {

    private String productId;
    private String name;
    private BigDecimal price;
    private Integer availableQuantity;

    public InventoryProductResponse() {}

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
}
