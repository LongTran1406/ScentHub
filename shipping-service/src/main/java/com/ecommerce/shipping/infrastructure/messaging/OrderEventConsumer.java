package com.ecommerce.shipping.infrastructure.messaging;

import com.ecommerce.shipping.infrastructure.messaging.dto.OrderCancelledPayload;
import com.ecommerce.shipping.infrastructure.messaging.dto.OrderConfirmedPayload;
import com.ecommerce.shipping.service.ShipmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Spring Cloud Stream consumer. The bean name ("orderEvents") is what the binding
 * "orderEvents-in-0" in application.yml refers to; the topic and consumer group are
 * configured there, not in code.
 */
@Component("orderEvents")
public class OrderEventConsumer implements Consumer<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ShipmentService shipmentService, ObjectMapper objectMapper) {
        this.shipmentService = shipmentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void accept(String rawMessage) {
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
