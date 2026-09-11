package com.ecommerce.customer.service;

import com.ecommerce.customer.dto.*;
import com.ecommerce.customer.entity.Complaint;
import com.ecommerce.customer.entity.Customer;
import com.ecommerce.customer.entity.Feedback;
import com.ecommerce.customer.exception.CustomerNotFoundException;
import com.ecommerce.customer.repository.ComplaintRepository;
import com.ecommerce.customer.repository.CustomerRepository;
import com.ecommerce.customer.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final FeedbackRepository feedbackRepository;
    private final ComplaintRepository complaintRepository;

    public CustomerService(CustomerRepository customerRepository,
                           FeedbackRepository feedbackRepository,
                           ComplaintRepository complaintRepository) {
        this.customerRepository = customerRepository;
        this.feedbackRepository = feedbackRepository;
        this.complaintRepository = complaintRepository;
    }

    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setCreatedAt(Instant.now());
        Customer saved = customerRepository.save(customer);
        return mapToCustomerResponse(saved);
    }

    public CustomerResponse getCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        return mapToCustomerResponse(customer);
    }

    public CustomerResponse updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        if (request.getName() != null) customer.setName(request.getName());
        if (request.getEmail() != null) customer.setEmail(request.getEmail());
        Customer saved = customerRepository.save(customer);
        return mapToCustomerResponse(saved);
    }

    public FeedbackResponse submitFeedback(UUID customerId, CreateFeedbackRequest request) {
        customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        Feedback feedback = new Feedback();
        feedback.setCustomerId(customerId);
        feedback.setContent(request.getContent());
        feedback.setRating(request.getRating());
        feedback.setCreatedAt(Instant.now());
        Feedback saved = feedbackRepository.save(feedback);
        return mapToFeedbackResponse(saved);
    }

    public ComplaintResponse submitComplaint(UUID customerId, CreateComplaintRequest request) {
        customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        Complaint complaint = new Complaint();
        complaint.setCustomerId(customerId);
        complaint.setContent(request.getContent());
        complaint.setStatus("OPEN");
        complaint.setCreatedAt(Instant.now());
        Complaint saved = complaintRepository.save(complaint);
        return mapToComplaintResponse(saved);
    }

    public List<ComplaintResponse> getComplaintsByCustomer(UUID customerId) {
        return complaintRepository.findByCustomerId(customerId)
            .stream()
            .map(this::mapToComplaintResponse)
            .collect(Collectors.toList());
    }

    private CustomerResponse mapToCustomerResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail());
    }

    private FeedbackResponse mapToFeedbackResponse(Feedback f) {
        return new FeedbackResponse(f.getId(), f.getContent(), f.getRating(), f.getCreatedAt().toString());
    }

    private ComplaintResponse mapToComplaintResponse(Complaint c) {
        return new ComplaintResponse(c.getId(), c.getContent(), c.getStatus(), c.getCreatedAt().toString());
    }
}
