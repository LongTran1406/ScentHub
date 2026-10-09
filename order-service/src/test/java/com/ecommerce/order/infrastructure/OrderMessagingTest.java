package com.ecommerce.order.infrastructure;

import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.port.CustomerDirectory;
import com.ecommerce.order.domain.port.ProductCatalog;
import com.ecommerce.order.domain.port.ProductSnapshot;
import com.ecommerce.order.domain.port.ShipmentGateway;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.service.dto.CreateOrderRequest;
import com.ecommerce.order.service.dto.OrderItemRequest;
import com.ecommerce.order.service.dto.OrderResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whole Spring context with the real Spring Cloud Stream bindings, but Kafka replaced by the in-memory test
 * binder and the other services replaced by fakes. Proves that domain events become the agreed JSON messages
 * on "order-events", and that messages on "inventory-events" drive the order lifecycle.
 */
@SpringBootTest
@Import({TestChannelBinderConfiguration.class, OrderMessagingTest.Fakes.class})
class OrderMessagingTest {

    static final UUID CUSTOMER = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();

    @TestConfiguration
    static class Fakes {
        @Bean @Primary
        CustomerDirectory customerDirectory() { return id -> id.equals(CUSTOMER); }

        @Bean @Primary
        ProductCatalog productCatalog() {
            ProductSnapshot rose = new ProductSnapshot(PRODUCT, "Rose Eau", Money.of("79.99"), 50);
            return new ProductCatalog() {
                public Optional<ProductSnapshot> findProduct(UUID id) {
                    return id.equals(PRODUCT) ? Optional.of(rose) : Optional.empty();
                }
                public List<ProductSnapshot> searchProducts(String keyword) { return List.of(rose); }
            };
        }

        @Bean @Primary
        ShipmentGateway shipmentGateway() { return orderId -> Optional.empty(); }
    }

    @Autowired private OrderService orderService;
    @Autowired private InputDestination input;
    @Autowired private OutputDestination output;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void drainLeftovers() {
        while (output.receive(50, "order-events") != null) {
            // discard messages from earlier tests
        }
    }

    private OrderResponse placeOrder(int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(PRODUCT);
        item.setQuantity(quantity);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(CUSTOMER);
        request.setItems(List.of(item));
        return orderService.createOrder(request);
    }

    private JsonNode nextOrderEvent() throws Exception {
        Message<byte[]> message = output.receive(2000, "order-events");
        assertThat(message).as("a message on order-events").isNotNull();
        return objectMapper.readTree(new String(message.getPayload(), StandardCharsets.UTF_8));
    }

    private void sendInventoryEvent(String eventType, String payloadJson) {
        String json = "{\"eventType\":\"" + eventType + "\",\"eventId\":\"" + UUID.randomUUID()
                + "\",\"timestamp\":\"2026-10-08T00:00:00Z\",\"payload\":" + payloadJson + "}";
        input.send(new GenericMessage<>(json.getBytes(StandardCharsets.UTF_8)), "inventory-events");
    }

    @Test
    void placingAnOrderPublishesAnOrderCreatedEventInTheAgreedFormat() throws Exception {
        OrderResponse order = placeOrder(2);

        JsonNode event = nextOrderEvent();

        assertThat(event.path("eventType").asText()).isEqualTo("OrderCreatedEvent");
        assertThat(event.path("eventId").asText()).isNotBlank();
        assertThat(event.path("timestamp").asText()).isNotBlank();
        JsonNode payload = event.path("payload");
        assertThat(payload.path("orderId").asText()).isEqualTo(order.getOrderId().toString());
        assertThat(payload.path("customerId").asText()).isEqualTo(CUSTOMER.toString());
        assertThat(payload.path("totalAmount").decimalValue()).isEqualByComparingTo("159.98");
        assertThat(payload.path("items")).hasSize(1);
        assertThat(payload.path("items").get(0).path("productId").asText()).isEqualTo(PRODUCT.toString());
        assertThat(payload.path("items").get(0).path("quantity").asInt()).isEqualTo(2);
    }

    @Test
    void anInventoryReservedEventConfirmsTheOrderAndPublishesOrderConfirmed() throws Exception {
        OrderResponse order = placeOrder(1);
        nextOrderEvent(); // OrderCreatedEvent

        sendInventoryEvent("InventoryReservedEvent",
                "{\"orderId\":\"" + order.getOrderId() + "\",\"items\":[]}");

        assertThat(orderService.getOrderById(order.getOrderId()).getStatus()).isEqualTo("CONFIRMED");
        JsonNode confirmed = nextOrderEvent();
        assertThat(confirmed.path("eventType").asText()).isEqualTo("OrderConfirmedEvent");
        assertThat(confirmed.path("payload").path("orderId").asText()).isEqualTo(order.getOrderId().toString());
        assertThat(confirmed.path("payload").path("customerId").asText()).isEqualTo(CUSTOMER.toString());
    }

    @Test
    void anInventoryRejectedEventRejectsTheOrderAndPublishesNothing() throws Exception {
        OrderResponse order = placeOrder(1);
        nextOrderEvent(); // OrderCreatedEvent

        sendInventoryEvent("InventoryRejectedEvent",
                "{\"orderId\":\"" + order.getOrderId() + "\",\"reason\":\"INSUFFICIENT_STOCK\",\"unavailableItems\":[]}");

        assertThat(orderService.getOrderById(order.getOrderId()).getStatus()).isEqualTo("REJECTED");
        assertThat(output.receive(300, "order-events")).as("rejection is a dead end").isNull();
    }

    @Test
    void cancellingAnOrderPublishesAnOrderCancelledEventWithThePreviousStatus() throws Exception {
        OrderResponse order = placeOrder(1);
        nextOrderEvent(); // OrderCreatedEvent
        sendInventoryEvent("InventoryReservedEvent", "{\"orderId\":\"" + order.getOrderId() + "\",\"items\":[]}");
        nextOrderEvent(); // OrderConfirmedEvent

        orderService.cancelOrder(order.getOrderId());

        JsonNode cancelled = nextOrderEvent();
        assertThat(cancelled.path("eventType").asText()).isEqualTo("OrderCancelledEvent");
        assertThat(cancelled.path("payload").path("previousStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(cancelled.path("payload").path("items")).hasSize(1);
    }

    @Test
    void garbageOnTheInventoryTopicIsIgnoredAndTheConsumerKeepsWorking() throws Exception {
        input.send(new GenericMessage<>("not json at all".getBytes(StandardCharsets.UTF_8)), "inventory-events");
        sendInventoryEvent("InventoryReservedEvent", "{\"orderId\":\"" + UUID.randomUUID() + "\",\"items\":[]}");

        OrderResponse order = placeOrder(1);
        sendInventoryEvent("InventoryReservedEvent", "{\"orderId\":\"" + order.getOrderId() + "\",\"items\":[]}");

        assertThat(orderService.getOrderById(order.getOrderId()).getStatus()).isEqualTo("CONFIRMED");
        nextOrderEvent();
    }
}
