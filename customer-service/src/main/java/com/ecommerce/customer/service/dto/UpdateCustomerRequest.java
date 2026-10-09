package com.ecommerce.customer.service.dto;

public class UpdateCustomerRequest {
    private String name;
    private String email;

    public UpdateCustomerRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
