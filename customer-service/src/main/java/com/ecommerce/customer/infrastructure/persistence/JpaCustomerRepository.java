package com.ecommerce.customer.infrastructure.persistence;

import com.ecommerce.customer.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data repository (technical detail; the domain only knows the CustomerRepository port). */
interface JpaCustomerRepository extends JpaRepository<Customer, UUID> {
}
