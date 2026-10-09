package com.ecommerce.inventory.service.dto;

public class TrendingProductResponse {

    private String productId;
    private String name;
    private Long unitsOrdered;

    public TrendingProductResponse() {}

    public TrendingProductResponse(String productId, String name, Long unitsOrdered) {
        this.productId = productId;
        this.name = name;
        this.unitsOrdered = unitsOrdered;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getUnitsOrdered() { return unitsOrdered; }
    public void setUnitsOrdered(Long unitsOrdered) { this.unitsOrdered = unitsOrdered; }
}
