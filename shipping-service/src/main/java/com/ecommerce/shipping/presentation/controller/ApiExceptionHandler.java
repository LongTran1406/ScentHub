package com.ecommerce.shipping.presentation.controller;

import com.ecommerce.shipping.domain.exception.InvalidShipmentStatusException;
import com.ecommerce.shipping.domain.exception.ShipmentNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain/application exceptions to HTTP responses (plain-text message bodies, as before). */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 404 is the signal Order Service reads as "no shipment yet", so it must stay a 404. */
    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ShipmentNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidShipmentStatusException.class)
    public ResponseEntity<String> handleInvalidStatus(InvalidShipmentStatusException ex) {
        return ResponseEntity.status(400).body(ex.getMessage());
    }
}
