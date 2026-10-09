package com.ecommerce.customer.service;

import com.ecommerce.customer.domain.exception.CustomerNotFoundException;
import com.ecommerce.customer.domain.model.Complaint;
import com.ecommerce.customer.domain.model.Customer;
import com.ecommerce.customer.domain.model.Feedback;
import com.ecommerce.customer.domain.repository.ComplaintRepository;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import com.ecommerce.customer.domain.repository.FeedbackRepository;
import com.ecommerce.customer.service.dto.ComplaintResponse;
import com.ecommerce.customer.service.dto.CreateComplaintRequest;
import com.ecommerce.customer.service.dto.CreateCustomerRequest;
import com.ecommerce.customer.service.dto.CreateFeedbackRequest;
import com.ecommerce.customer.service.dto.CustomerResponse;
import com.ecommerce.customer.service.dto.FeedbackResponse;
import com.ecommerce.customer.service.dto.UpdateCustomerRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Application service for customer profiles, feedback and complaints. Rules live in the domain model. */
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

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        Customer customer = Customer.register(request.getName(), request.getEmail());
        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID customerId) {
        return toResponse(load(customerId));
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        Customer customer = load(customerId);
        customer.updateProfile(request.getName(), request.getEmail());
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public FeedbackResponse submitFeedback(UUID customerId, CreateFeedbackRequest request) {
        requireCustomer(customerId);
        Feedback saved = feedbackRepository.save(
                Feedback.submit(customerId, request.getContent(), request.getRating()));
        return new FeedbackResponse(saved.getId(), saved.getContent(), saved.getRating(),
                saved.getCreatedAt().toString());
    }

    @Transactional
    public ComplaintResponse submitComplaint(UUID customerId, CreateComplaintRequest request) {
        requireCustomer(customerId);
        return toResponse(complaintRepository.save(Complaint.raise(customerId, request.getContent())));
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByCustomer(UUID customerId) {
        return complaintRepository.findByCustomerId(customerId).stream().map(CustomerService::toResponse).toList();
    }

    private Customer load(UUID customerId) {
        return customerRepository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private void requireCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }
    }

    private static CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail().address());
    }

    private static ComplaintResponse toResponse(Complaint c) {
        return new ComplaintResponse(c.getId(), c.getContent(), c.getStatus().name(), c.getCreatedAt().toString());
    }
}
