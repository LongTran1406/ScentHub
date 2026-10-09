package com.ecommerce.customer.domain.model;

import com.ecommerce.customer.domain.exception.InvalidCustomerDataException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Aggregate: one piece of feedback from a customer, with an optional 1-5 rating. Immutable once created. */
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String content;

    @Column
    private Integer rating;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Feedback() {
        // for JPA
    }

    public static Feedback submit(UUID customerId, String content, Integer rating) {
        if (content == null || content.isBlank()) {
            throw new InvalidCustomerDataException("Feedback content is required");
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new InvalidCustomerDataException("Rating must be between 1 and 5");
        }
        Feedback feedback = new Feedback();
        feedback.customerId = customerId;
        feedback.content = content.trim();
        feedback.rating = rating;
        feedback.createdAt = Instant.now();
        return feedback;
    }

    public UUID getId() { return id; }

    public UUID getCustomerId() { return customerId; }

    public String getContent() { return content; }

    public Integer getRating() { return rating; }

    public Instant getCreatedAt() { return createdAt; }
}
