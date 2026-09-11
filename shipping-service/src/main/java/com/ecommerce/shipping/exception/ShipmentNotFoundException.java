package com.ecommerce.shipping.exception;

import java.util.UUID;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(UUID orderId) {
        super("No shipment found for order: " + orderId);
    }
}
