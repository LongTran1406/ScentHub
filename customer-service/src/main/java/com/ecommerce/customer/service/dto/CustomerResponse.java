package com.ecommerce.customer.service.dto;

import java.util.UUID;

public class CustomerResponse {
    private UUID customerId;
    private String name;
    private String email;

    public CustomerResponse() {}

    public CustomerResponse(UUID customerId, String name, String email) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
    }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
