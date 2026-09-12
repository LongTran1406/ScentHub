package com.ecommerce.inventory.dto;

import java.util.UUID;

public class LowStockResponse {

    private UUID productId;
    private String name;
    private Integer availableQuantity;
    private Integer threshold;

    public LowStockResponse() {}

    public LowStockResponse(UUID productId, String name, Integer availableQuantity, Integer threshold) {
        this.productId = productId;
        this.name = name;
        this.availableQuantity = availableQuantity;
        this.threshold = threshold;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }
}
