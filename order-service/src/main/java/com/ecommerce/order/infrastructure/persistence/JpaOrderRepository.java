package com.ecommerce.order.infrastructure.persistence;

import com.ecommerce.order.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repository (technical detail; the domain only knows the OrderRepository port). */
interface JpaOrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerId(UUID customerId);
}
