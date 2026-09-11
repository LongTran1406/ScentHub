package com.ecommerce.shipping.service;

import com.ecommerce.shipping.dto.ShipmentResponse;
import com.ecommerce.shipping.entity.Shipment;
import com.ecommerce.shipping.exception.InvalidShipmentStatusException;
import com.ecommerce.shipping.exception.ShipmentNotFoundException;
import com.ecommerce.shipping.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private static final List<String> VALID_STATUSES =
        List.of("CREATED", "DISPATCHED", "IN_TRANSIT", "DELIVERED", "CANCELLED");

    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    public ShipmentResponse getShipmentByOrderId(UUID orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ShipmentNotFoundException(orderId));
        return mapToResponse(shipment);
    }

    public ShipmentResponse updateStatus(UUID orderId, String newStatus) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ShipmentNotFoundException(orderId));

        if (!VALID_STATUSES.contains(newStatus)) {
            throw new InvalidShipmentStatusException("Unknown status: " + newStatus);
        }

        shipment.setStatus(newStatus);
        shipment.setUpdatedAt(Instant.now());
        Shipment saved = shipmentRepository.save(shipment);
        return mapToResponse(saved);
    }

    public Shipment createShipment(UUID orderId) {
        // Check for duplicate - if shipment already exists, log and return existing
        // to handle duplicate OrderConfirmedEvent gracefully
        Optional<Shipment> existing = shipmentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.info("Shipment already exists for orderId={}, ignoring duplicate event", orderId);
            return existing.get();
        }
        Instant now = Instant.now();
        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setStatus("CREATED");
        shipment.setCreatedAt(now);
        shipment.setUpdatedAt(now);
        return shipmentRepository.save(shipment);
    }

    public void cancelIfNotDispatched(UUID orderId) {
        Optional<Shipment> optional = shipmentRepository.findByOrderId(orderId);
        if (optional.isEmpty()) {
            log.info("No shipment found for orderId={} during cancellation — order was cancelled before any shipment existed", orderId);
            return;
        }
        Shipment shipment = optional.get();
        if ("CREATED".equals(shipment.getStatus())) {
            shipment.setStatus("CANCELLED");
            shipment.setUpdatedAt(Instant.now());
            shipmentRepository.save(shipment);
            log.info("Shipment for orderId={} cancelled successfully", orderId);
        } else {
            log.warn("Shipment for orderId={} has status={} — cannot cancel; skipping", orderId, shipment.getStatus());
        }
    }

    private ShipmentResponse mapToResponse(Shipment shipment) {
        return new ShipmentResponse(shipment.getOrderId().toString(), shipment.getStatus());
    }
}
