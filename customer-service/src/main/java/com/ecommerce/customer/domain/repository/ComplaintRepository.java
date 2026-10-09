package com.ecommerce.customer.domain.repository;

import com.ecommerce.customer.domain.model.Complaint;

import java.util.List;
import java.util.UUID;

/** Port: storage of {@link Complaint} aggregates. */
public interface ComplaintRepository {

    Complaint save(Complaint complaint);

    List<Complaint> findByCustomerId(UUID customerId);
}
