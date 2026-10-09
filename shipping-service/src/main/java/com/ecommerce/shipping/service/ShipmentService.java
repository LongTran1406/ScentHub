package com.ecommerce.shipping.service;

import com.ecommerce.shipping.domain.exception.ShipmentNotFoundException;
import com.ecommerce.shipping.domain.model.Shipment;
import com.ecommerce.shipping.domain.model.ShipmentStatus;
import com.ecommerce.shipping.domain.repository.ShipmentRepository;
import com.ecommerce.shipping.service.dto.ShipmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Application service for shipments. Rules live in {@link Shipment}; this class loads, calls and saves. */
@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByOrderId(UUID orderId) {
        return toResponse(load(orderId));
    }

    @Transactional
    public ShipmentResponse updateStatus(UUID orderId, String newStatus) {
        Shipment shipment = load(orderId);
        shipment.changeStatus(ShipmentStatus.parse(newStatus));
        return toResponse(shipmentRepository.save(shipment));
    }

    /** Creates the shipment for a confirmed order. A duplicate OrderConfirmedEvent is ignored (idempotent). */
    @Transactional
    public Shipment createShipment(UUID orderId) {
        Optional<Shipment> existing = shipmentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.info("Shipment already exists for orderId={}, ignoring duplicate event", orderId);
            return existing.get();
        }
        return shipmentRepository.save(Shipment.createFor(orderId));
    }

    /** Cancels the shipment of a cancelled order if it has not left the warehouse. Never throws. */
    @Transactional
    public void cancelIfNotDispatched(UUID orderId) {
        Optional<Shipment> optional = shipmentRepository.findByOrderId(orderId);
        if (optional.isEmpty()) {
            log.info("No shipment found for orderId={} during cancellation - order was cancelled before any shipment existed", orderId);
            return;
        }
        Shipment shipment = optional.get();
        if (shipment.cancelIfNotDispatched()) {
            shipmentRepository.save(shipment);
            log.info("Shipment for orderId={} cancelled successfully", orderId);
        } else {
            log.warn("Shipment for orderId={} has status={} - cannot cancel; skipping", orderId, shipment.getStatus());
        }
    }

    private Shipment load(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId).orElseThrow(() -> new ShipmentNotFoundException(orderId));
    }

    private static ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(shipment.getOrderId().toString(), shipment.getStatus().name());
    }
}
