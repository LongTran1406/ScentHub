package com.ecommerce.shipping.domain.exception;

public class InvalidShipmentStatusException extends RuntimeException {
    public InvalidShipmentStatusException(String message) {
        super(message);
    }
}
