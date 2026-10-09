package com.ecommerce.shipping.infrastructure;

import com.ecommerce.shipping.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Loads the whole Spring context (real controllers, services, H2, and the real Spring Cloud Stream binding
 * "orderEvents-in-0") but swaps Kafka for the in-memory test binder, so no broker is needed.
 */
@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
class OrderEventsBindingTest {

    private static final String TOPIC = "order-events";

    @Autowired
    private InputDestination input;

    @Autowired
    private ShipmentService shipmentService;

    private static String envelope(String eventType, UUID orderId) {
        return "{\"eventType\":\"" + eventType + "\",\"eventId\":\"" + UUID.randomUUID()
                + "\",\"timestamp\":\"2026-10-08T00:00:00Z\",\"payload\":{\"orderId\":\"" + orderId
                + "\",\"customerId\":\"" + UUID.randomUUID() + "\",\"items\":[],\"previousStatus\":\"CONFIRMED\"}}";
    }

    private void send(String eventType, UUID orderId) {
        input.send(new GenericMessage<>(envelope(eventType, orderId).getBytes(StandardCharsets.UTF_8)), TOPIC);
    }

    @Test
    void anOrderConfirmedEventCreatesAShipment() {
        UUID orderId = UUID.randomUUID();

        send("OrderConfirmedEvent", orderId);

        assertThat(shipmentService.getShipmentByOrderId(orderId).getStatus()).isEqualTo("CREATED");
    }

    @Test
    void aDuplicateOrderConfirmedEventIsHarmless() {
        UUID orderId = UUID.randomUUID();

        send("OrderConfirmedEvent", orderId);
        send("OrderConfirmedEvent", orderId);

        assertThat(shipmentService.getShipmentByOrderId(orderId).getStatus()).isEqualTo("CREATED");
    }

    @Test
    void anOrderCancelledEventCancelsAShipmentThatHasNotBeenDispatched() {
        UUID orderId = UUID.randomUUID();
        send("OrderConfirmedEvent", orderId);

        send("OrderCancelledEvent", orderId);

        assertThat(shipmentService.getShipmentByOrderId(orderId).getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void aDispatchedShipmentSurvivesAnOrderCancelledEvent() {
        UUID orderId = UUID.randomUUID();
        send("OrderConfirmedEvent", orderId);
        shipmentService.updateStatus(orderId, "DISPATCHED");

        send("OrderCancelledEvent", orderId);

        assertThat(shipmentService.getShipmentByOrderId(orderId).getStatus()).isEqualTo("DISPATCHED");
    }

    @Test
    void cancellingAnOrderWithoutAShipmentAndIgnoredEventTypesDoNotBreakTheConsumer() {
        UUID noShipment = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        send("OrderCancelledEvent", noShipment);
        send("OrderCreatedEvent", other);   // shipping does not act on this one
        send("SomethingElseEvent", other);

        assertThatThrownBy(() -> shipmentService.getShipmentByOrderId(noShipment)).isNotNull();
        assertThatThrownBy(() -> shipmentService.getShipmentByOrderId(other)).isNotNull();

        // and the consumer is still alive afterwards
        UUID later = UUID.randomUUID();
        send("OrderConfirmedEvent", later);
        assertThat(shipmentService.getShipmentByOrderId(later).getStatus()).isEqualTo("CREATED");
    }
}
