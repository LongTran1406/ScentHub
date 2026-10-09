package com.ecommerce.order.service;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.exception.InvalidOrderException;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderLineDraft;
import com.ecommerce.order.domain.port.CustomerDirectory;
import com.ecommerce.order.domain.port.OrderEventPublisher;
import com.ecommerce.order.domain.port.ProductCatalog;
import com.ecommerce.order.domain.port.ProductSnapshot;
import com.ecommerce.order.domain.port.ShipmentGateway;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.domain.service.CancellationPolicy;
import com.ecommerce.order.service.dto.CreateOrderRequest;
import com.ecommerce.order.service.dto.OrderItemRequest;
import com.ecommerce.order.service.dto.OrderItemResponse;
import com.ecommerce.order.service.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application service: orchestrates one use case at a time. Business rules live in the domain layer
 * ({@link Order}, {@link CancellationPolicy}); this class only loads, calls, saves and publishes.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final ProductCatalog productCatalog;
    private final CustomerDirectory customerDirectory;
    private final ShipmentGateway shipmentGateway;
    private final CancellationPolicy cancellationPolicy;

    public OrderService(OrderRepository orderRepository,
                        OrderEventPublisher eventPublisher,
                        ProductCatalog productCatalog,
                        CustomerDirectory customerDirectory,
                        ShipmentGateway shipmentGateway,
                        CancellationPolicy cancellationPolicy) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.productCatalog = productCatalog;
        this.customerDirectory = customerDirectory;
        this.shipmentGateway = shipmentGateway;
        this.cancellationPolicy = cancellationPolicy;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request == null || request.getCustomerId() == null) {
            throw new InvalidOrderException("customerId is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOrderException("An order needs at least one item");
        }
        if (!customerDirectory.exists(request.getCustomerId())) {
            throw new InvalidOrderException("Customer not found: " + request.getCustomerId());
        }

        List<OrderLineDraft> lines = new ArrayList<>();
        for (OrderItemRequest item : request.getItems()) {
            if (item.getProductId() == null) {
                throw new InvalidOrderException("Every item needs a productId");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new InvalidOrderException("Quantity must be positive for product " + item.getProductId());
            }
            ProductSnapshot product = productCatalog.findProduct(item.getProductId())
                    .orElseThrow(() -> new InvalidOrderException("Product not found: " + item.getProductId()));
            lines.add(new OrderLineDraft(item.getProductId(), item.getQuantity(), product.price()));
        }

        Order order = Order.place(request.getCustomerId(), lines);
        orderRepository.save(order);
        publishAfterCommit(order.pullEvents());
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        return toResponse(load(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
    }

    /** Inventory reserved the stock for this order. */
    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = load(orderId);
        if (order.confirm()) {
            publishAfterCommit(order.pullEvents());
        } else {
            log.info("Ignoring stock reservation for order {}: status is {}", orderId, order.getStatus());
        }
    }

    /** Inventory could not reserve the stock; the rejection is a dead end (no event leaves this service). */
    @Transactional
    public void rejectOrder(UUID orderId, String reason) {
        Order order = load(orderId);
        if (order.reject(reason)) {
            publishAfterCommit(order.pullEvents());
        } else {
            log.info("Ignoring stock rejection for order {}: status is {}", orderId, order.getStatus());
        }
    }

    /**
     * The single entry point for cancelling an order. The REST endpoint and the cancellation AI agent both call
     * this method, so the eligibility rule exists exactly once ({@link CancellationPolicy}).
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = load(orderId);
        cancellationPolicy.verifyCancellable(order, () -> shipmentGateway.findShipmentStatus(orderId));
        order.cancel();
        publishAfterCommit(order.pullEvents());
        return toResponse(order);
    }

    private Order load(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /** Events leave the service only once the database change is durable. */
    private void publishAfterCommit(List<DomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publish(events);
                }
            });
        } else {
            eventPublisher.publish(events);
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProductId(), i.getQuantity(), i.getUnitPrice().amount()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus().name(), order.getTotalAmount().amount(), items);
    }
}
