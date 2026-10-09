package com.ecommerce.order.service.agent;

import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.port.ProductSnapshot;
import com.ecommerce.order.domain.service.CancellationPolicy;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.service.OrderServiceTest;
import com.ecommerce.order.service.dto.CreateOrderRequest;
import com.ecommerce.order.service.dto.OrderItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The LLM agents themselves cannot be tested without a real model, but the TOOLS they call are plain Java: what
 * they return is exactly what the model reads and relays to the customer, so their wording and behaviour are
 * tested here.
 */
class AgentToolsTest {

    private OrderServiceTest.InMemoryOrders orders;
    private OrderServiceTest.FakeCatalog catalog;
    private OrderServiceTest.FakeShipping shipping;
    private OrderService orderService;
    private OrderCancelTool cancelTool;
    private ProductLookupTool lookupTool;

    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orders = new OrderServiceTest.InMemoryOrders();
        catalog = new OrderServiceTest.FakeCatalog();
        OrderServiceTest.FakeCustomers customers = new OrderServiceTest.FakeCustomers();
        shipping = new OrderServiceTest.FakeShipping();
        orderService = new OrderService(orders, new OrderServiceTest.RecordingPublisher(), catalog, customers,
                shipping, new CancellationPolicy());
        cancelTool = new OrderCancelTool(orderService);
        lookupTool = new ProductLookupTool(catalog);

        customers.known.add(customerId);
        catalog.products.put(productId, new ProductSnapshot(productId, "Rose Eau", Money.of("79.99"), 12));
    }

    private UUID placeOrder() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setItems(List.of(item));
        return orderService.createOrder(request).getOrderId();
    }

    @Test
    void cancelToolConfirmsASuccessfulCancellation() {
        UUID orderId = placeOrder();

        String result = cancelTool.cancelOrder(orderId.toString());

        assertThat(result).isEqualTo("Order " + orderId + " has been successfully cancelled.");
        assertThat(orderService.getOrderById(orderId).getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelToolRelaysWhyAnOrderCannotBeCancelled() {
        UUID orderId = placeOrder();
        shipping.status = "DISPATCHED";

        String result = cancelTool.cancelOrder(orderId.toString());

        assertThat(result).contains("shipment already DISPATCHED");
        assertThat(orderService.getOrderById(orderId).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void cancelToolSaysWhenTheOrderDoesNotExist() {
        UUID unknown = UUID.randomUUID();

        assertThat(cancelTool.cancelOrder(unknown.toString())).isEqualTo("No order found with ID " + unknown);
    }

    @Test
    void cancelToolSurvivesAMalformedOrderId() {
        assertThat(cancelTool.cancelOrder("my-order")).contains("not a valid order ID");
    }

    @Test
    void lookupToolListsRealProductsWithPriceAndAvailableStock() {
        String result = lookupTool.searchProducts("rose");

        assertThat(result).isEqualTo("Rose Eau - $79.99 - 12 in stock");
    }

    @Test
    void lookupToolSaysWhenNothingMatches() {
        catalog.products.clear();

        assertThat(lookupTool.searchProducts("nothing")).isEqualTo("No matching products found.");
    }
}
