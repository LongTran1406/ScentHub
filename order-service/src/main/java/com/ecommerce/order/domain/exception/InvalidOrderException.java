package com.ecommerce.order.domain.exception;

/** The order request breaks a business rule (empty order, bad quantity, unknown customer or product, ...). */
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
