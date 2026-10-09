package com.ecommerce.inventory.infrastructure.messaging;

import com.ecommerce.inventory.domain.event.InventoryEvent;
import com.ecommerce.inventory.domain.event.StockLine;
import com.ecommerce.inventory.domain.event.StockRejected;
import com.ecommerce.inventory.domain.event.StockReserved;
import com.ecommerce.inventory.domain.port.InventoryEventPublisher;
import com.ecommerce.inventory.infrastructure.messaging.dto.EventEnvelope;
import com.ecommerce.inventory.infrastructure.messaging.dto.InventoryRejectedPayload;
import com.ecommerce.inventory.infrastructure.messaging.dto.InventoryReservedPayload;
import com.ecommerce.inventory.infrastructure.messaging.dto.InventoryUnavailableItemDto;
import com.ecommerce.inventory.infrastructure.messaging.dto.OrderItemEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Translates stock outcomes into the shared JSON envelope and sends them to the "inventory-events" topic through
 * Spring Cloud Stream's StreamBridge (binding "inventoryEvents-out-0", mapped to the topic in application.yml).
 */
@Component
public class KafkaInventoryEventPublisher implements InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaInventoryEventPublisher.class);
    private static final String BINDING = "inventoryEvents-out-0";

    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    public KafkaInventoryEventPublisher(StreamBridge streamBridge, ObjectMapper objectMapper) {
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(InventoryEvent event) {
        switch (event) {
            case StockReserved e -> send("InventoryReservedEvent",
                    new InventoryReservedPayload(e.orderId(), items(e.lines())));
            case StockRejected e -> send("InventoryRejectedEvent",
                    new InventoryRejectedPayload(e.orderId(), e.reason(), e.shortages().stream()
                            .map(s -> new InventoryUnavailableItemDto(
                                    s.productId().toString(), s.requested(), s.available()))
                            .toList()));
        }
    }

    private static List<OrderItemEventDto> items(List<StockLine> lines) {
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
