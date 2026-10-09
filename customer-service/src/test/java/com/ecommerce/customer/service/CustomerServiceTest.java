package com.ecommerce.customer.service;

import com.ecommerce.customer.domain.exception.CustomerNotFoundException;
import com.ecommerce.customer.domain.exception.InvalidCustomerDataException;
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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerServiceTest {

    static class InMemoryCustomers implements CustomerRepository {
        final Map<UUID, Customer> store = new HashMap<>();
        public Customer save(Customer c) { store.put(c.getId(), c); return c; }
        public Optional<Customer> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        public boolean existsById(UUID id) { return store.containsKey(id); }
    }

    static class InMemoryFeedback implements FeedbackRepository {
        final List<Feedback> store = new ArrayList<>();
        public Feedback save(Feedback f) { store.add(f); return f; }
    }

    static class InMemoryComplaints implements ComplaintRepository {
        final List<Complaint> store = new ArrayList<>();
        public Complaint save(Complaint c) { store.add(c); return c; }
        public List<Complaint> findByCustomerId(UUID id) {
            return store.stream().filter(c -> c.getCustomerId().equals(id)).toList();
        }
    }

    private final InMemoryFeedback feedback = new InMemoryFeedback();
    private final InMemoryComplaints complaints = new InMemoryComplaints();
    private final CustomerService service =
            new CustomerService(new InMemoryCustomers(), feedback, complaints);

    private CustomerResponse register(String name, String email) {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName(name);
        request.setEmail(email);
        return service.createCustomer(request);
    }

    @Test
    void createAndGetCustomer() {
        CustomerResponse created = register("Jane", "Jane@X.com");

        CustomerResponse fetched = service.getCustomer(created.getCustomerId());

        assertThat(fetched.getName()).isEqualTo("Jane");
        assertThat(fetched.getEmail()).isEqualTo("jane@x.com");
    }

    @Test
    void invalidRegistrationIsRefused() {
        assertThatThrownBy(() -> register("Jane", "nope")).isInstanceOf(InvalidCustomerDataException.class);
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        UUID id = register("Jane", "jane@x.com").getCustomerId();
        UpdateCustomerRequest update = new UpdateCustomerRequest();
        update.setName("Janet");

        CustomerResponse updated = service.updateCustomer(id, update);

        assertThat(updated.getName()).isEqualTo("Janet");
        assertThat(updated.getEmail()).isEqualTo("jane@x.com");
    }

    @Test
    void unknownCustomerIsNotFoundEverywhere() {
        UUID unknown = UUID.randomUUID();
        CreateFeedbackRequest f = new CreateFeedbackRequest();
        f.setContent("x");
        CreateComplaintRequest c = new CreateComplaintRequest();
        c.setContent("x");

        assertThatThrownBy(() -> service.getCustomer(unknown)).isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> service.updateCustomer(unknown, new UpdateCustomerRequest()))
                .isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> service.submitFeedback(unknown, f)).isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> service.submitComplaint(unknown, c)).isInstanceOf(CustomerNotFoundException.class);
        assertThat(feedback.store).isEmpty();
        assertThat(complaints.store).isEmpty();
    }

    @Test
    void feedbackIsStoredWithItsRating() {
        UUID id = register("Jane", "jane@x.com").getCustomerId();
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setContent("Lovely");
        request.setRating(4);

        FeedbackResponse response = service.submitFeedback(id, request);

        assertThat(response.getRating()).isEqualTo(4);
        assertThat(feedback.store).hasSize(1);
    }

    @Test
    void complaintsStartOpenAndAreListedPerCustomer() {
        UUID jane = register("Jane", "jane@x.com").getCustomerId();
        UUID bob = register("Bob", "bob@x.com").getCustomerId();
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setContent("Late parcel");
        service.submitComplaint(jane, request);

        List<ComplaintResponse> janes = service.getComplaintsByCustomer(jane);

        assertThat(janes).singleElement().satisfies(c -> assertThat(c.getStatus()).isEqualTo("OPEN"));
        assertThat(service.getComplaintsByCustomer(bob)).isEmpty();
    }
}
