package com.ecommerce.customer.infrastructure.persistence;

import com.ecommerce.customer.domain.model.Complaint;
import com.ecommerce.customer.domain.model.Customer;
import com.ecommerce.customer.domain.model.Feedback;
import com.ecommerce.customer.domain.repository.ComplaintRepository;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import com.ecommerce.customer.domain.repository.FeedbackRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapters: implement the domain's repository ports with Spring Data JPA. */
final class RepositoryAdapters {

    private RepositoryAdapters() {
    }

    @Repository
    static class CustomerRepositoryAdapter implements CustomerRepository {
        private final JpaCustomerRepository jpa;

        CustomerRepositoryAdapter(JpaCustomerRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Customer save(Customer customer) {
            return jpa.save(customer);
        }

        @Override
        public Optional<Customer> findById(UUID id) {
            return jpa.findById(id);
        }

        @Override
        public boolean existsById(UUID id) {
            return jpa.existsById(id);
        }
    }

    @Repository
    static class FeedbackRepositoryAdapter implements FeedbackRepository {
        private final JpaFeedbackRepository jpa;

        FeedbackRepositoryAdapter(JpaFeedbackRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Feedback save(Feedback feedback) {
            return jpa.save(feedback);
        }
    }

    @Repository
    static class ComplaintRepositoryAdapter implements ComplaintRepository {
        private final JpaComplaintRepository jpa;

        ComplaintRepositoryAdapter(JpaComplaintRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Complaint save(Complaint complaint) {
            return jpa.save(complaint);
        }

        @Override
        public List<Complaint> findByCustomerId(UUID customerId) {
            return jpa.findByCustomerId(customerId);
        }
    }
}
