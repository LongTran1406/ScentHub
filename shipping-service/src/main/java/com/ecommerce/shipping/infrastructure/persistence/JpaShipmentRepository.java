package com.ecommerce.shipping.infrastructure.persistence;

import com.ecommerce.shipping.domain.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository (technical detail; the domain only knows the ShipmentRepository port). */
interface JpaShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByOrderId(UUID orderId);
}
