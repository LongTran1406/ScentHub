package com.ecommerce.customer.domain.repository;

import com.ecommerce.customer.domain.model.Customer;

import java.util.Optional;
import java.util.UUID;

/** Port: how the domain loads and stores {@link Customer} aggregates. */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    boolean existsById(UUID id);
}
