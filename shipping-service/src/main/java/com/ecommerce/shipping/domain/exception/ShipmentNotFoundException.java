package com.ecommerce.shipping.domain.exception;

import java.util.UUID;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(UUID orderId) {
        super("No shipment found for order: " + orderId);
    }
}
