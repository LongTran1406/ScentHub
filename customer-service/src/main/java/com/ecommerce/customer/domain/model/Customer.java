package com.ecommerce.customer.domain.model;

import com.ecommerce.customer.domain.exception.InvalidCustomerDataException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root of the Customer bounded context. Feedback and complaints are separate aggregates that refer to
 * a customer only by id, so a customer profile can change without loading its (unbounded) history.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", nullable = false, unique = true))
    private Email email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Customer() {
        // for JPA
    }

    public static Customer register(String name, String email) {
        Customer customer = new Customer();
        customer.id = UUID.randomUUID();
        customer.name = requireName(name);
        customer.email = new Email(email);
        customer.createdAt = Instant.now();
        return customer;
    }

    /** Partial update: only non-null arguments are applied. */
    public void updateProfile(String name, String email) {
        if (name != null) {
            this.name = requireName(name);
        }
        if (email != null) {
            this.email = new Email(email);
        }
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public Email getEmail() { return email; }

    public Instant getCreatedAt() { return createdAt; }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidCustomerDataException("Customer name is required");
        }
        return name.trim();
    }
}
