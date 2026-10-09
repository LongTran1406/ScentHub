package com.ecommerce.customer.infrastructure.persistence;

import com.ecommerce.customer.domain.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repository (technical detail; the domain only knows the ComplaintRepository port). */
interface JpaComplaintRepository extends JpaRepository<Complaint, UUID> {

    List<Complaint> findByCustomerId(UUID customerId);
}
