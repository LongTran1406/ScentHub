package com.ecommerce.order.infrastructure.messaging;

import com.ecommerce.order.infrastructure.messaging.dto.InventoryRejectedPayload;
import com.ecommerce.order.infrastructure.messaging.dto.InventoryReservedPayload;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Spring Cloud Stream consumer. The bean name ("inventoryEvents") is what the binding
 * "inventoryEvents-in-0" in application.yml refers to; topic and group are configured there.
 */
@Component("inventoryEvents")
public class InventoryEventConsumer implements Consumer<String> {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public InventoryEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void accept(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            String eventType = root.path("eventType").asText();
            JsonNode payloadNode = root.path("payload");

            switch (eventType) {
                case "InventoryReservedEvent" -> {
                    InventoryReservedPayload payload = objectMapper.treeToValue(payloadNode, InventoryReservedPayload.class);
                    log.debug("Received InventoryReservedEvent for orderId [{}]", payload.getOrderId());
                    orderService.confirmOrder(UUID.fromString(payload.getOrderId()));
                }
                case "InventoryRejectedEvent" -> {
                    InventoryRejectedPayload payload = objectMapper.treeToValue(payloadNode, InventoryRejectedPayload.class);
                    log.debug("Received InventoryRejectedEvent for orderId [{}]", payload.getOrderId());
                    orderService.rejectOrder(UUID.fromString(payload.getOrderId()), payload.getReason());
                }
                default -> log.debug("Ignoring event type [{}]", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", e.getMessage(), e);
        }
    }
}
