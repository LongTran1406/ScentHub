package com.ecommerce.order.domain.exception;

/** A service this one depends on (inventory, customer, shipping) could not be reached or answered with an error. */
public class ExternalServiceUnavailableException extends RuntimeException {
    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
