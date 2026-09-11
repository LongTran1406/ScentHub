package com.ecommerce.customer.repository;

import com.ecommerce.customer.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    List<Feedback> findByCustomerId(UUID customerId);
}
