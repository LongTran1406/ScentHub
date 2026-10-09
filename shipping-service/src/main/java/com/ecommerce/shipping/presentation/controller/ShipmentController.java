package com.ecommerce.shipping.presentation.controller;

import com.ecommerce.shipping.service.dto.ShipmentResponse;
import com.ecommerce.shipping.service.dto.UpdateShipmentStatusRequest;
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
}
