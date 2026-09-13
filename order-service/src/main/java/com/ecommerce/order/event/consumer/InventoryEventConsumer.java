package com.ecommerce.order.event.consumer;

import com.ecommerce.order.event.dto.InventoryRejectedPayload;
import com.ecommerce.order.event.dto.InventoryReservedPayload;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public InventoryEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-service")
    public void onMessage(String rawMessage) {
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
