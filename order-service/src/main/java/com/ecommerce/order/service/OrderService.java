package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ecommerce.order.event.dto.OrderCreatedPayload;
import com.ecommerce.order.event.dto.OrderItemEventDto;
import com.ecommerce.order.event.producer.OrderEventProducer;

import com.ecommerce.order.event.dto.OrderConfirmedPayload;
import com.ecommerce.order.client.ShippingServiceClient;
import com.ecommerce.order.event.dto.OrderCancelledPayload;
import com.ecommerce.order.exception.OrderCancellationNotAllowedException;


@Service
public class OrderService {

    private static final BigDecimal TEMP_UNIT_PRICE = new BigDecimal("74.99");
    private static final List<String> NOT_YET_DISPATCHED_STATUSES = List.of("DISPATCHED", "IN_TRANSIT", "DELIVERED");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventProducer orderEventProducer;
    private final ShippingServiceClient shippingServiceClient;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        OrderEventProducer orderEventProducer, ShippingServiceClient shippingServiceClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderEventProducer = orderEventProducer;
        this.shippingServiceClient = shippingServiceClient;
    }


    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(request.getCustomerId());
        order.setStatus("PENDING");
        Instant now = Instant.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(TEMP_UNIT_PRICE);
            item.setOrder(order);
            order.getItems().add(item);

            total = total.add(TEMP_UNIT_PRICE.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        List<OrderItemEventDto> eventItems = saved.getItems().stream()
                .map(item -> new OrderItemEventDto(item.getProductId().toString(), item.getQuantity()))
                .collect(Collectors.toList());
        OrderCreatedPayload payload = new OrderCreatedPayload(
                saved.getId().toString(),
                saved.getCustomerId().toString(),
                eventItems,
                saved.getTotalAmount()
        );
        orderEventProducer.publish("OrderCreatedEvent", payload);

        return toResponse(saved);
    }

    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return toResponse(order);
    }

    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void confirmOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus("CONFIRMED");
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);

        List<OrderItemEventDto> eventItems = saved.getItems().stream()
                .map(item -> new OrderItemEventDto(item.getProductId().toString(), item.getQuantity()))
                .collect(Collectors.toList());
        OrderConfirmedPayload payload = new OrderConfirmedPayload(
                saved.getId().toString(),
                saved.getCustomerId().toString(),
                eventItems
        );
        orderEventProducer.publish("OrderConfirmedEvent", payload);
    }

    public void rejectOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus("REJECTED");
        order.setRejectionReason(reason);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }

    public OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus().equals("SHIPPED") || order.getStatus().equals("COMPLETED")
                || order.getStatus().equals("CANCELLED")) {
            throw new OrderCancellationNotAllowedException(
                    "Cannot cancel order " + orderId + ": already " + order.getStatus());
        }

        String shipmentStatus = shippingServiceClient.getShipmentStatus(orderId);
        if (shipmentStatus != null && NOT_YET_DISPATCHED_STATUSES.contains(shipmentStatus)) {
            throw new OrderCancellationNotAllowedException(
                    "Cannot cancel order " + orderId + ": shipment already " + shipmentStatus);
        }

        String previousStatus = order.getStatus();
        order.setStatus("CANCELLED");
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);

        List<OrderItemEventDto> eventItems = saved.getItems().stream()
                .map(item -> new OrderItemEventDto(item.getProductId().toString(), item.getQuantity()))
                .collect(Collectors.toList());
        OrderCancelledPayload payload = new OrderCancelledPayload(
                saved.getId().toString(), eventItems, previousStatus);
        orderEventProducer.publish("OrderCancelledEvent", payload);

        return toResponse(saved);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .collect(Collectors.toList());
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(), itemResponses);
    }
}
