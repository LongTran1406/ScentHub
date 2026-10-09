package com.ecommerce.customer.presentation.controller;

import com.ecommerce.customer.domain.exception.CustomerNotFoundException;
import com.ecommerce.customer.domain.exception.InvalidCustomerDataException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain/application exceptions to HTTP responses (plain-text message bodies, as before). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<String> handleNotFound(CustomerNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidCustomerDataException.class)
    public ResponseEntity<String> handleInvalidData(InvalidCustomerDataException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDuplicateEmail(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body("Duplicate value - likely email already exists: "
                + ex.getMostSpecificCause().getMessage());
    }
}
