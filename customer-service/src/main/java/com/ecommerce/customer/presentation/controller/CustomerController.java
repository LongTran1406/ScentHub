package com.ecommerce.customer.presentation.controller;

import com.ecommerce.customer.service.dto.*;
import com.ecommerce.customer.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(201).body(customerService.createCustomer(request));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.getCustomer(customerId));
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID customerId,
            @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
    }

    @PostMapping("/{customerId}/feedback")
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @PathVariable UUID customerId,
            @RequestBody CreateFeedbackRequest request) {
        return ResponseEntity.status(201).body(customerService.submitFeedback(customerId, request));
    }

    @PostMapping("/{customerId}/complaints")
    public ResponseEntity<ComplaintResponse> submitComplaint(
            @PathVariable UUID customerId,
            @RequestBody CreateComplaintRequest request) {
        return ResponseEntity.status(201).body(customerService.submitComplaint(customerId, request));
    }

    @GetMapping("/{customerId}/complaints")
    public ResponseEntity<List<ComplaintResponse>> getComplaints(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.getComplaintsByCustomer(customerId));
    }
}
