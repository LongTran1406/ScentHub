package com.ecommerce.order.domain.repository;

import com.ecommerce.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port: how the domain loads and stores {@link Order} aggregates. Implemented in the infrastructure layer. */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    List<Order> findByCustomerId(UUID customerId);
}
