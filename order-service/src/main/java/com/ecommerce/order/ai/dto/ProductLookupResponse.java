package com.ecommerce.order.ai.dto;

import java.math.BigDecimal;

public class ProductLookupResponse {

    private String productId;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;

    public ProductLookupResponse() {}

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
}
