package com.ecommerce.shipping.service;

import com.ecommerce.shipping.domain.exception.InvalidShipmentStatusException;
import com.ecommerce.shipping.domain.exception.ShipmentNotFoundException;
import com.ecommerce.shipping.domain.model.Shipment;
import com.ecommerce.shipping.domain.model.ShipmentStatus;
import com.ecommerce.shipping.domain.repository.ShipmentRepository;
import com.ecommerce.shipping.service.dto.ShipmentResponse;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentServiceTest {

    static class InMemoryShipments implements ShipmentRepository {
        final Map<UUID, Shipment> byOrder = new HashMap<>();
        public Shipment save(Shipment s) { byOrder.put(s.getOrderId(), s); return s; }
        public Optional<Shipment> findByOrderId(UUID orderId) { return Optional.ofNullable(byOrder.get(orderId)); }
    }

    private final InMemoryShipments shipments = new InMemoryShipments();
    private final ShipmentService service = new ShipmentService(shipments);
    private final UUID orderId = UUID.randomUUID();

    @Test
    void createShipmentStartsAsCreated() {
        service.createShipment(orderId);

        assertThat(service.getShipmentByOrderId(orderId).getStatus()).isEqualTo("CREATED");
    }

    @Test
    void aDuplicateOrderConfirmedEventDoesNotCreateASecondShipment() {
        Shipment first = service.createShipment(orderId);
        Shipment second = service.createShipment(orderId);

        assertThat(second).isSameAs(first);
        assertThat(shipments.byOrder).hasSize(1);
    }

    @Test
    void cancellingAnOrderCancelsItsCreatedShipment() {
        service.createShipment(orderId);

        service.cancelIfNotDispatched(orderId);

        assertThat(service.getShipmentByOrderId(orderId).getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void aDispatchedShipmentIsLeftAloneWithoutAnError() {
        service.createShipment(orderId);
        service.updateStatus(orderId, "DISPATCHED");

        assertThatCode(() -> service.cancelIfNotDispatched(orderId)).doesNotThrowAnyException();

        assertThat(service.getShipmentByOrderId(orderId).getStatus()).isEqualTo("DISPATCHED");
    }

    @Test
    void cancellingAnOrderThatNeverHadAShipmentIsANoOp() {
        assertThatCode(() -> service.cancelIfNotDispatched(UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    void updateStatusValidatesTheValue() {
        service.createShipment(orderId);

        ShipmentResponse ok = service.updateStatus(orderId, "IN_TRANSIT");

        assertThat(ok.getStatus()).isEqualTo("IN_TRANSIT");
        assertThat(ok.getOrderId()).isEqualTo(orderId.toString());
        assertThatThrownBy(() -> service.updateStatus(orderId, "FLYING"))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(shipments.byOrder.get(orderId).getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void missingShipmentsAreReportedAsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.getShipmentByOrderId(unknown)).isInstanceOf(ShipmentNotFoundException.class);
        assertThatThrownBy(() -> service.updateStatus(unknown, "DISPATCHED"))
                .isInstanceOf(ShipmentNotFoundException.class);
    }
}
