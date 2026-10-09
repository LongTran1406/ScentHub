package com.ecommerce.order.domain.port;

import java.util.Optional;
import java.util.UUID;

/** Port to the Shipping context. */
public interface ShipmentGateway {

    /** @return the shipment status ("CREATED", "DISPATCHED", ...) or empty if no shipment exists yet */
    Optional<String> findShipmentStatus(UUID orderId);
}
