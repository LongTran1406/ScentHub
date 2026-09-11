package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.dto.ShipmentResponse;
import com.ecommerce.shipping.dto.UpdateShipmentStatusRequest;
import com.ecommerce.shipping.exception.InvalidShipmentStatusException;
import com.ecommerce.shipping.exception.ShipmentNotFoundException;
import com.ecommerce.shipping.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ShipmentResponse> getShipment(@PathVariable UUID orderId) {
        return ResponseEntity.ok(shipmentService.getShipmentByOrderId(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ShipmentResponse> updateStatus(
            @PathVariable UUID orderId,
            @RequestBody UpdateShipmentStatusRequest request) {
        return ResponseEntity.ok(shipmentService.updateStatus(orderId, request.getStatus()));
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ShipmentNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidShipmentStatusException.class)
    public ResponseEntity<String> handleInvalidStatus(InvalidShipmentStatusException ex) {
        return ResponseEntity.status(400).body(ex.getMessage());
    }
}
