package com.ecommerce.shipping.event.consumer;

import com.ecommerce.shipping.event.dto.OrderCancelledPayload;
import com.ecommerce.shipping.event.dto.OrderConfirmedPayload;
import com.ecommerce.shipping.service.ShipmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ShipmentService shipmentService, ObjectMapper objectMapper) {
        this.shipmentService = shipmentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "shipping-service")
    public void onMessage(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            String eventType = root.path("eventType").asText();
            JsonNode payloadNode = root.path("payload");

            switch (eventType) {
                case "OrderConfirmedEvent" -> {
                    OrderConfirmedPayload payload = objectMapper.treeToValue(payloadNode, OrderConfirmedPayload.class);
                    log.info("Processing OrderConfirmedEvent for orderId={}", payload.getOrderId());
                    shipmentService.createShipment(UUID.fromString(payload.getOrderId()));
                }
                case "OrderCancelledEvent" -> {
                    OrderCancelledPayload payload = objectMapper.treeToValue(payloadNode, OrderCancelledPayload.class);
                    log.info("Processing OrderCancelledEvent for orderId={}", payload.getOrderId());
                    shipmentService.cancelIfNotDispatched(UUID.fromString(payload.getOrderId()));
                }
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", rawMessage, e);
        }
    }
}
