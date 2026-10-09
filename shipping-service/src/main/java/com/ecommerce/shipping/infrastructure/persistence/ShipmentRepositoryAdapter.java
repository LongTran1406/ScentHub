package com.ecommerce.shipping.infrastructure.persistence;

import com.ecommerce.shipping.domain.model.Shipment;
import com.ecommerce.shipping.domain.repository.ShipmentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adapter: implements the domain's ShipmentRepository port with Spring Data JPA. */
@Repository
class ShipmentRepositoryAdapter implements ShipmentRepository {

    private final JpaShipmentRepository jpa;

    ShipmentRepositoryAdapter(JpaShipmentRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Shipment save(Shipment shipment) {
        return jpa.save(shipment);
    }

    @Override
    public Optional<Shipment> findByOrderId(UUID orderId) {
        return jpa.findByOrderId(orderId);
    }
}
