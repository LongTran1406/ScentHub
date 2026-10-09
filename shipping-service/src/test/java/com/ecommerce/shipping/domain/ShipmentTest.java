package com.ecommerce.shipping.domain;

import com.ecommerce.shipping.domain.exception.InvalidShipmentStatusException;
import com.ecommerce.shipping.domain.model.Shipment;
import com.ecommerce.shipping.domain.model.ShipmentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    @Test
    void aNewShipmentIsCreatedForItsOrder() {
        UUID orderId = UUID.randomUUID();

        Shipment shipment = Shipment.createFor(orderId);

        assertThat(shipment.getOrderId()).isEqualTo(orderId);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(shipment.getId()).isNotNull();
    }

    @Test
    void anOrderIdIsRequired() {
        assertThatThrownBy(() -> Shipment.createFor(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancellingOrdersCancelsOnlyShipmentsThatHaveNotBeenDispatched() {
        Shipment fresh = Shipment.createFor(UUID.randomUUID());
        assertThat(fresh.cancelIfNotDispatched()).isTrue();
        assertThat(fresh.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);

        for (ShipmentStatus status : new ShipmentStatus[]{
                ShipmentStatus.DISPATCHED, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED}) {
            Shipment moving = Shipment.createFor(UUID.randomUUID());
            moving.changeStatus(status);
            assertThat(moving.cancelIfNotDispatched()).as("status " + status).isFalse();
            assertThat(moving.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void anOperatorCanSetAnyStatus() {
        Shipment shipment = Shipment.createFor(UUID.randomUUID());

        shipment.changeStatus(ShipmentStatus.DELIVERED);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @Test
    void statusTextIsParsedStrictly() {
        assertThat(ShipmentStatus.parse("IN_TRANSIT")).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThatThrownBy(() -> ShipmentStatus.parse("FLYING"))
                .isInstanceOf(InvalidShipmentStatusException.class)
                .hasMessage("Unknown status: FLYING");
        assertThatThrownBy(() -> ShipmentStatus.parse("created")).isInstanceOf(InvalidShipmentStatusException.class);
        assertThatThrownBy(() -> ShipmentStatus.parse(null)).isInstanceOf(InvalidShipmentStatusException.class);
    }

    @Test
    void onlyDispatchedOnwardsHaveLeftTheWarehouse() {
        assertThat(ShipmentStatus.CREATED.hasLeftWarehouse()).isFalse();
        assertThat(ShipmentStatus.CANCELLED.hasLeftWarehouse()).isFalse();
        assertThat(ShipmentStatus.DISPATCHED.hasLeftWarehouse()).isTrue();
        assertThat(ShipmentStatus.IN_TRANSIT.hasLeftWarehouse()).isTrue();
        assertThat(ShipmentStatus.DELIVERED.hasLeftWarehouse()).isTrue();
    }
}
