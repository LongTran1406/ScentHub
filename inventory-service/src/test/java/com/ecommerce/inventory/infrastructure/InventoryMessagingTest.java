package com.ecommerce.inventory.infrastructure;

import com.ecommerce.inventory.service.ProductService;
import com.ecommerce.inventory.service.dto.CreateProductRequest;
import com.ecommerce.inventory.service.dto.ProductResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whole Spring context (real H2 database, real consumer and publisher) with Kafka replaced by the in-memory
 * test binder. The Kafka Streams topologies are switched off here because they need a real broker; they are
 * covered by the end-to-end smoke test instead.
 */
@SpringBootTest(properties = {
        "spring.cloud.function.definition=orderEvents",
        "spring.cloud.stream.binders.integration.type=integration",
        "spring.cloud.stream.bindings.orderEvents-in-0.binder=integration",
        "spring.cloud.stream.bindings.inventoryEvents-out-0.binder=integration"
})
@Import(TestChannelBinderConfiguration.class)
class InventoryMessagingTest {

    @Autowired private InputDestination input;
    @Autowired private OutputDestination output;
    @Autowired private ProductService productService;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void drainLeftovers() {
        while (output.receive(50, "inventory-events") != null) {
            // discard messages from earlier tests
        }
    }

    private ProductResponse product(int stock) {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Rose Eau");
        request.setDescription("d");
        request.setPrice(new BigDecimal("79.99"));
        request.setStockQuantity(stock);
        return productService.createProduct(request);
    }

    private void sendOrderEvent(String eventType, String payloadJson) {
        String json = "{\"eventType\":\"" + eventType + "\",\"eventId\":\"" + UUID.randomUUID()
                + "\",\"timestamp\":\"2026-10-08T00:00:00Z\",\"payload\":" + payloadJson + "}";
        input.send(new GenericMessage<>(json.getBytes(StandardCharsets.UTF_8)), "order-events");
    }

    private String items(UUID productId, int quantity) {
        return "[{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}]";
    }

    private JsonNode nextInventoryEvent() throws Exception {
        Message<byte[]> message = output.receive(2000, "inventory-events");
        assertThat(message).as("a message on inventory-events").isNotNull();
        return objectMapper.readTree(new String(message.getPayload(), StandardCharsets.UTF_8));
    }

    @Test
    void anOrderCreatedEventReservesStockAndPublishesInventoryReserved() throws Exception {
        ProductResponse p = product(5);

        sendOrderEvent("OrderCreatedEvent", "{\"orderId\":\"order-1\",\"customerId\":\"c\",\"items\":"
                + items(p.getProductId(), 2) + ",\"totalAmount\":159.98}");

        JsonNode event = nextInventoryEvent();
        assertThat(event.path("eventType").asText()).isEqualTo("InventoryReservedEvent");
        assertThat(event.path("payload").path("orderId").asText()).isEqualTo("order-1");
        assertThat(event.path("payload").path("items").get(0).path("quantity").asInt()).isEqualTo(2);
        assertThat(productService.getProduct(p.getProductId()).getReservedQuantity()).isEqualTo(2);
        assertThat(productService.getProduct(p.getProductId()).getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    void anOversizedOrderIsRejectedWithTheShortageDetailAndReservesNothing() throws Exception {
        ProductResponse p = product(5);

        sendOrderEvent("OrderCreatedEvent", "{\"orderId\":\"order-2\",\"customerId\":\"c\",\"items\":"
                + items(p.getProductId(), 9) + ",\"totalAmount\":1}");

        JsonNode event = nextInventoryEvent();
        assertThat(event.path("eventType").asText()).isEqualTo("InventoryRejectedEvent");
        assertThat(event.path("payload").path("reason").asText()).isEqualTo("INSUFFICIENT_STOCK");
        JsonNode shortage = event.path("payload").path("unavailableItems").get(0);
        assertThat(shortage.path("productId").asText()).isEqualTo(p.getProductId().toString());
        assertThat(shortage.path("requested").asInt()).isEqualTo(9);
        assertThat(shortage.path("available").asInt()).isEqualTo(5);
        assertThat(productService.getProduct(p.getProductId()).getReservedQuantity()).isZero();
    }

    @Test
    void anOrderCancelledEventReleasesTheReservationAndPublishesNothing() throws Exception {
        ProductResponse p = product(5);
        sendOrderEvent("OrderCreatedEvent", "{\"orderId\":\"order-3\",\"customerId\":\"c\",\"items\":"
                + items(p.getProductId(), 4) + ",\"totalAmount\":1}");
        nextInventoryEvent(); // InventoryReservedEvent

        sendOrderEvent("OrderCancelledEvent", "{\"orderId\":\"order-3\",\"items\":"
                + items(p.getProductId(), 4) + ",\"previousStatus\":\"CONFIRMED\"}");

        assertThat(productService.getProduct(p.getProductId()).getReservedQuantity()).isZero();
        assertThat(output.receive(300, "inventory-events")).isNull();
    }

    @Test
    void ordersForUnknownProductsAreRejectedAsProductNotFound() throws Exception {
        sendOrderEvent("OrderCreatedEvent", "{\"orderId\":\"order-4\",\"customerId\":\"c\",\"items\":"
                + items(UUID.randomUUID(), 1) + ",\"totalAmount\":1}");

        JsonNode event = nextInventoryEvent();
        assertThat(event.path("eventType").asText()).isEqualTo("InventoryRejectedEvent");
        assertThat(event.path("payload").path("reason").asText()).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void eventsInventoryDoesNotActOnAreIgnored() {
        sendOrderEvent("OrderConfirmedEvent", "{\"orderId\":\"order-5\",\"customerId\":\"c\",\"items\":[]}");

        assertThat(output.receive(300, "inventory-events")).isNull();
    }
}
