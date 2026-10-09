package com.ecommerce.inventory.domain.exception;

/** A product operation breaks a business rule (blank name, negative price/stock, stock below reserved, ...). */
public class InvalidProductException extends RuntimeException {
    public InvalidProductException(String message) {
        super(message);
    }
}
