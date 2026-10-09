package com.ecommerce.shipping.domain.repository;

import com.ecommerce.shipping.domain.model.Shipment;

import java.util.Optional;
import java.util.UUID;

/** Port: how the domain loads and stores {@link Shipment} aggregates. Implemented in the infrastructure layer. */
public interface ShipmentRepository {

    Shipment save(Shipment shipment);

    Optional<Shipment> findByOrderId(UUID orderId);
}
