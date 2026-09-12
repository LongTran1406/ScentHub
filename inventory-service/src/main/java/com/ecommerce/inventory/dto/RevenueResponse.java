package com.ecommerce.inventory.dto;

public class RevenueResponse {

    private Double totalRevenue;

    public RevenueResponse() {}

    public RevenueResponse(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
}
