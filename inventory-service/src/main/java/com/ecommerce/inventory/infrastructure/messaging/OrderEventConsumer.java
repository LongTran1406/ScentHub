package com.ecommerce.inventory.infrastructure.messaging;

import com.ecommerce.inventory.domain.event.StockLine;
import com.ecommerce.inventory.infrastructure.messaging.dto.OrderCancelledPayload;
import com.ecommerce.inventory.infrastructure.messaging.dto.OrderCreatedPayload;
import com.ecommerce.inventory.infrastructure.messaging.dto.OrderItemEventDto;
import com.ecommerce.inventory.service.StockService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Spring Cloud Stream consumer. The bean name ("orderEvents") is what the binding
 * "orderEvents-in-0" in application.yml refers to; topic and group are configured there.
 */
@Component("orderEvents")
public class OrderEventConsumer implements Consumer<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final StockService stockService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(StockService stockService, ObjectMapper objectMapper) {
        this.stockService = stockService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void accept(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            String eventType = root.path("eventType").asText();
            JsonNode payloadNode = root.path("payload");

            switch (eventType) {
                case "OrderCreatedEvent" -> {
                    OrderCreatedPayload payload = objectMapper.treeToValue(payloadNode, OrderCreatedPayload.class);
                    log.debug("Received OrderCreatedEvent for orderId [{}]", payload.getOrderId());
                    stockService.reserveForOrder(payload.getOrderId(), toLines(payload.getItems()));
                }
                case "OrderCancelledEvent" -> {
                    OrderCancelledPayload payload = objectMapper.treeToValue(payloadNode, OrderCancelledPayload.class);
                    log.debug("Received OrderCancelledEvent for orderId [{}]", payload.getOrderId());
                    stockService.releaseForOrder(toLines(payload.getItems()));
                }
                case "OrderConfirmedEvent", "OrderRejectedEvent" -> {
                    // This service does not act on these events
                    log.debug("Ignoring event type [{}]", eventType);
                }
                default -> log.warn("Unknown event type received: [{}]", eventType);
            }
        } catch (OptimisticLockingFailureException e) {
            // Another message changed the same product at the same moment: let the binder retry this one.
            log.warn("Concurrent stock update, message will be retried: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", e.getMessage(), e);
        }
    }

    private static List<StockLine> toLines(List<OrderItemEventDto> items) {
        return items.stream()
                .map(i -> new StockLine(UUID.fromString(i.getProductId()), i.getQuantity()))
                .toList();
    }
}
