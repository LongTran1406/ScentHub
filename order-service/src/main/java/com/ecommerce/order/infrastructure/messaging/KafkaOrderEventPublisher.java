package com.ecommerce.order.infrastructure.messaging;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.event.EventLine;
import com.ecommerce.order.domain.event.OrderCancelled;
import com.ecommerce.order.domain.event.OrderConfirmed;
import com.ecommerce.order.domain.event.OrderPlaced;
import com.ecommerce.order.domain.event.OrderRejected;
import com.ecommerce.order.domain.port.OrderEventPublisher;
import com.ecommerce.order.infrastructure.messaging.dto.EventEnvelope;
import com.ecommerce.order.infrastructure.messaging.dto.OrderCancelledPayload;
import com.ecommerce.order.infrastructure.messaging.dto.OrderConfirmedPayload;
import com.ecommerce.order.infrastructure.messaging.dto.OrderCreatedPayload;
import com.ecommerce.order.infrastructure.messaging.dto.OrderItemEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Translates domain events into the shared JSON envelope and sends them to the "order-events" topic through
 * Spring Cloud Stream's StreamBridge (binding "orderEvents-out-0", mapped to the topic in application.yml).
 */
@Component
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);
    private static final String BINDING = "orderEvents-out-0";

    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    public KafkaOrderEventPublisher(StreamBridge streamBridge, ObjectMapper objectMapper) {
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            switch (event) {
                case OrderPlaced e -> send("OrderCreatedEvent", new OrderCreatedPayload(
                        e.orderId().toString(), e.customerId().toString(), items(e.lines()),
                        e.totalAmount().amount()));
                case OrderConfirmed e -> send("OrderConfirmedEvent", new OrderConfirmedPayload(
                        e.orderId().toString(), e.customerId().toString(), items(e.lines())));
                case OrderCancelled e -> send("OrderCancelledEvent", new OrderCancelledPayload(
                        e.orderId().toString(), items(e.lines()), e.previousStatus()));
                case OrderRejected e ->
                        log.debug("Order {} rejected ({}): not published, rejection is a dead end by design",
                                e.orderId(), e.reason());
            }
        }
    }

    private static List<OrderItemEventDto> items(List<EventLine> lines) {
        return lines.stream()
                .map(l -> new OrderItemEventDto(l.productId().toString(), l.quantity()))
                .toList();
    }

    private void send(String eventType, Object payload) {
        try {
            EventEnvelope envelope = new EventEnvelope(
                    eventType, UUID.randomUUID().toString(), Instant.now().toString(), payload);
            streamBridge.send(BINDING, objectMapper.writeValueAsString(envelope));
            log.debug("Published event [{}] via binding [{}]", eventType, BINDING);
        } catch (Exception e) {
            log.error("Failed to publish event [{}]: {}", eventType, e.getMessage(), e);
        }
    }
}
