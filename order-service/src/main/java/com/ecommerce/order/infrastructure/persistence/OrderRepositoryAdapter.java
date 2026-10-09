package com.ecommerce.order.infrastructure.persistence;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter: implements the domain's OrderRepository port with Spring Data JPA. */
@Repository
class OrderRepositoryAdapter implements OrderRepository {

    private final JpaOrderRepository jpa;

    OrderRepositoryAdapter(JpaOrderRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        return jpa.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return jpa.findByCustomerId(customerId);
    }
}
