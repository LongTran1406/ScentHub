package com.ecommerce.order.presentation.controller;

import com.ecommerce.order.domain.exception.ExternalServiceUnavailableException;
import com.ecommerce.order.domain.exception.InvalidOrderException;
import com.ecommerce.order.domain.exception.OrderCancellationNotAllowedException;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain/application exceptions to HTTP responses (plain-text message bodies, as before). */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<String> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(OrderCancellationNotAllowedException.class)
    public ResponseEntity<String> handleCancellationNotAllowed(OrderCancellationNotAllowedException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<String> handleInvalidOrder(InvalidOrderException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<String> handleDependencyDown(ExternalServiceUnavailableException ex) {
        log.warn("{}: {}", ex.getMessage(), ex.getCause() == null ? "" : ex.getCause().getMessage());
        return ResponseEntity.status(503).body(ex.getMessage() + ". Please try again later.");
    }
}
