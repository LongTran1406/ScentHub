package com.ecommerce.inventory.event.consumer;

import com.ecommerce.inventory.event.dto.OrderCancelledPayload;
import com.ecommerce.inventory.event.dto.OrderCreatedPayload;
import com.ecommerce.inventory.event.producer.InventoryEventProducer;
import com.ecommerce.inventory.service.ProductService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ProductService productService;
    private final InventoryEventProducer inventoryEventProducer;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ProductService productService,
                              InventoryEventProducer inventoryEventProducer,
                              ObjectMapper objectMapper) {
        this.productService = productService;
        this.inventoryEventProducer = inventoryEventProducer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void onMessage(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            String eventType = root.path("eventType").asText();
            JsonNode payloadNode = root.path("payload");

            switch (eventType) {
                case "OrderCreatedEvent" -> {
                    OrderCreatedPayload payload = objectMapper.treeToValue(payloadNode, OrderCreatedPayload.class);
                    log.debug("Received OrderCreatedEvent for orderId [{}]", payload.getOrderId());
                    productService.reserveStock(payload.getOrderId(), payload.getItems());
                }
                case "OrderCancelledEvent" -> {
                    OrderCancelledPayload payload = objectMapper.treeToValue(payloadNode, OrderCancelledPayload.class);
                    log.debug("Received OrderCancelledEvent for orderId [{}]", payload.getOrderId());
                    productService.releaseStock(payload.getItems());
                }
                case "OrderConfirmedEvent", "OrderRejectedEvent" -> {
                    // This service does not act on these events
                    log.debug("Ignoring event type [{}]", eventType);
                }
                default -> log.warn("Unknown event type received: [{}]", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", e.getMessage(), e);
        }
    }
}
