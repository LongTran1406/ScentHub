package com.ecommerce.order.service;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.event.OrderCancelled;
import com.ecommerce.order.domain.event.OrderConfirmed;
import com.ecommerce.order.domain.event.OrderPlaced;
import com.ecommerce.order.domain.exception.InvalidOrderException;
import com.ecommerce.order.domain.exception.OrderCancellationNotAllowedException;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.port.CustomerDirectory;
import com.ecommerce.order.domain.port.OrderEventPublisher;
import com.ecommerce.order.domain.port.ProductCatalog;
import com.ecommerce.order.domain.port.ProductSnapshot;
import com.ecommerce.order.domain.port.ShipmentGateway;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.domain.service.CancellationPolicy;
import com.ecommerce.order.service.dto.CreateOrderRequest;
import com.ecommerce.order.service.dto.OrderItemRequest;
import com.ecommerce.order.service.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Application-service tests with in-memory fakes for every port (no Spring, no Kafka, no network). */
public class OrderServiceTest {

    // ---- fakes -------------------------------------------------------------------------------------------

    public static class InMemoryOrders implements OrderRepository {
        public final Map<UUID, Order> store = new HashMap<>();
        public Order save(Order order) { store.put(order.getId(), order); return order; }
        public Optional<Order> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        public List<Order> findByCustomerId(UUID customerId) {
            return store.values().stream().filter(o -> o.getCustomerId().equals(customerId)).toList();
        }
    }

    public static class FakeCatalog implements ProductCatalog {
        public final Map<UUID, ProductSnapshot> products = new HashMap<>();
        public Optional<ProductSnapshot> findProduct(UUID id) { return Optional.ofNullable(products.get(id)); }
        public List<ProductSnapshot> searchProducts(String keyword) { return new ArrayList<>(products.values()); }
    }

    public static class FakeCustomers implements CustomerDirectory {
        public final Set<UUID> known = new HashSet<>();
        public boolean exists(UUID id) { return known.contains(id); }
    }

    public static class FakeShipping implements ShipmentGateway {
        public String status; // null = no shipment yet
        public Optional<String> findShipmentStatus(UUID orderId) { return Optional.ofNullable(status); }
    }

    public static class RecordingPublisher implements OrderEventPublisher {
        public final List<DomainEvent> published = new ArrayList<>();
        public void publish(List<DomainEvent> events) { published.addAll(events); }
    }

    // ---- fixture -----------------------------------------------------------------------------------------

    private InMemoryOrders orders;
    private FakeCatalog catalog;
    private FakeCustomers customers;
    private FakeShipping shipping;
    private RecordingPublisher publisher;
    private OrderService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
        catalog = new FakeCatalog();
        customers = new FakeCustomers();
        shipping = new FakeShipping();
        publisher = new RecordingPublisher();
        service = new OrderService(orders, publisher, catalog, customers, shipping, new CancellationPolicy());

        customers.known.add(customerId);
        catalog.products.put(productId, new ProductSnapshot(productId, "Rose Eau", Money.of("79.99"), 10));
    }

    private CreateOrderRequest request(UUID customer, UUID product, Integer quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(product);
        item.setQuantity(quantity);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customer);
        request.setItems(List.of(item));
        return request;
    }

    // ---- create ------------------------------------------------------------------------------------------

    @Test
    void createOrderUsesTheCataloguePriceAndPublishesOrderPlaced() {
        OrderResponse response = service.createOrder(request(customerId, productId, 2));

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("159.98");
        assertThat(response.getItems()).singleElement()
                .satisfies(i -> assertThat(i.getUnitPrice()).isEqualByComparingTo("79.99"));
        assertThat(publisher.published).singleElement().isInstanceOf(OrderPlaced.class);
        assertThat(orders.store).hasSize(1);
    }

    @Test
    void createOrderRejectsAnUnknownCustomer() {
        assertThatThrownBy(() -> service.createOrder(request(UUID.randomUUID(), productId, 1)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Customer not found");
        assertThat(orders.store).isEmpty();
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void createOrderRejectsAnUnknownProduct() {
        assertThatThrownBy(() -> service.createOrder(request(customerId, UUID.randomUUID(), 1)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Product not found");
        assertThat(orders.store).isEmpty();
    }

    @Test
    void createOrderRejectsBadQuantitiesAndEmptyOrders() {
        assertThatThrownBy(() -> service.createOrder(request(customerId, productId, 0)))
                .isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> service.createOrder(request(customerId, productId, null)))
                .isInstanceOf(InvalidOrderException.class);

        CreateOrderRequest empty = new CreateOrderRequest();
        empty.setCustomerId(customerId);
        empty.setItems(List.of());
        assertThatThrownBy(() -> service.createOrder(empty)).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> service.createOrder(new CreateOrderRequest()))
                .isInstanceOf(InvalidOrderException.class);
    }

    // ---- inventory replies -------------------------------------------------------------------------------

    @Test
    void confirmOrderPublishesOrderConfirmed() {
        UUID orderId = placeOrder();
        publisher.published.clear();

        service.confirmOrder(orderId);

        assertThat(service.getOrderById(orderId).getStatus()).isEqualTo("CONFIRMED");
        assertThat(publisher.published).singleElement().isInstanceOf(OrderConfirmed.class);
    }

    @Test
    void aLateStockReservationDoesNotResurrectACancelledOrder() {
        UUID orderId = placeOrder();
        service.cancelOrder(orderId);
        publisher.published.clear();

        service.confirmOrder(orderId);

        assertThat(service.getOrderById(orderId).getStatus()).isEqualTo("CANCELLED");
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void rejectOrderStoresTheReason() {
        UUID orderId = placeOrder();

        service.rejectOrder(orderId, "INSUFFICIENT_STOCK");

        assertThat(service.getOrderById(orderId).getStatus()).isEqualTo("REJECTED");
    }

    @Test
    void confirmingAnUnknownOrderFails() {
        assertThatThrownBy(() -> service.confirmOrder(UUID.randomUUID())).isInstanceOf(OrderNotFoundException.class);
    }

    // ---- cancel ------------------------------------------------------------------------------------------

    @Test
    void cancelOrderWithNoShipmentSucceedsAndPublishesOrderCancelled() {
        UUID orderId = placeOrder();
        publisher.published.clear();

        OrderResponse response = service.cancelOrder(orderId);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(publisher.published).singleElement().isInstanceOf(OrderCancelled.class);
    }

    @Test
    void cancelOrderIsRefusedOnceTheShipmentIsDispatched() {
        UUID orderId = placeOrder();
        shipping.status = "DISPATCHED";
        publisher.published.clear();

        assertThatThrownBy(() -> service.cancelOrder(orderId))
                .isInstanceOf(OrderCancellationNotAllowedException.class)
                .hasMessageContaining("shipment already DISPATCHED");

        assertThat(service.getOrderById(orderId).getStatus()).isEqualTo("PENDING");
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void cancelOrderIsRefusedTwice() {
        UUID orderId = placeOrder();
        service.cancelOrder(orderId);

        assertThatThrownBy(() -> service.cancelOrder(orderId))
                .isInstanceOf(OrderCancellationNotAllowedException.class);
    }

    @Test
    void cancelUnknownOrderFails() {
        assertThatThrownBy(() -> service.cancelOrder(UUID.randomUUID())).isInstanceOf(OrderNotFoundException.class);
    }

    // ---- queries -----------------------------------------------------------------------------------------

    @Test
    void ordersCanBeListedPerCustomer() {
        placeOrder();
        placeOrder();

        assertThat(service.getOrdersByCustomer(customerId)).hasSize(2);
        assertThat(service.getOrdersByCustomer(UUID.randomUUID())).isEmpty();
    }

    private UUID placeOrder() {
        return service.createOrder(request(customerId, productId, 1)).getOrderId();
    }
}
