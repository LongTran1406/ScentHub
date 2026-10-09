package com.ecommerce.inventory.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductResponse {

    private UUID productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity; // computed: stockQuantity - reservedQuantity

    public ProductResponse() {}

    public ProductResponse(UUID productId, String name, String description,
                           BigDecimal price, Integer stockQuantity,
                           Integer reservedQuantity) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = stockQuantity - reservedQuantity;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
}
