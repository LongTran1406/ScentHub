package com.ecommerce.customer.service.dto;

public class CreateCustomerRequest {
    private String name;
    private String email;

    public CreateCustomerRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
