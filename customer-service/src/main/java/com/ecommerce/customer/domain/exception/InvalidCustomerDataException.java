package com.ecommerce.customer.domain.exception;

/** Customer, feedback or complaint data breaks a business rule (blank name, malformed email, rating range, ...). */
public class InvalidCustomerDataException extends RuntimeException {
    public InvalidCustomerDataException(String message) {
        super(message);
    }
}
