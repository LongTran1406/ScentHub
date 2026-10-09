package com.ecommerce.customer.domain.model;

import com.ecommerce.customer.domain.exception.InvalidCustomerDataException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Aggregate: a complaint raised by a customer. Always starts OPEN. */
@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Complaint() {
        // for JPA
    }

    public static Complaint raise(UUID customerId, String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidCustomerDataException("Complaint content is required");
        }
        Complaint complaint = new Complaint();
        complaint.customerId = customerId;
        complaint.content = content.trim();
        complaint.status = ComplaintStatus.OPEN;
        complaint.createdAt = Instant.now();
        return complaint;
    }

    public UUID getId() { return id; }

    public UUID getCustomerId() { return customerId; }

    public String getContent() { return content; }

    public ComplaintStatus getStatus() { return status; }

    public Instant getCreatedAt() { return createdAt; }
}
