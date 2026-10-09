package com.ecommerce.order.domain.service;

import com.ecommerce.order.domain.exception.OrderCancellationNotAllowedException;
import com.ecommerce.order.domain.model.Order;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Domain service: the ONE place that decides whether an order may be cancelled. It spans two things the order
 * aggregate cannot know alone (the order's own status and the state of its shipment, which belongs to the
 * Shipping context), so it is a service rather than a method on {@link Order}.
 *
 * <p>Both the REST endpoint and the cancellation AI agent reach this rule through {@code OrderService}.
 */
public class CancellationPolicy {

    private static final Set<String> ALREADY_LEFT_WAREHOUSE = Set.of("DISPATCHED", "IN_TRANSIT", "DELIVERED");

    /**
     * @param shipmentStatus looked up lazily, so the remote call to Shipping is skipped when the order's own
     *                       status already forbids cancelling
     * @throws OrderCancellationNotAllowedException if the order cannot be cancelled
     */
    public void verifyCancellable(Order order, Supplier<Optional<String>> shipmentStatus) {
        if (!order.isCancellable()) {
            throw new OrderCancellationNotAllowedException(
                    "Cannot cancel order " + order.getId() + ": already " + order.getStatus());
        }
        shipmentStatus.get()
                .filter(ALREADY_LEFT_WAREHOUSE::contains)
                .ifPresent(status -> {
                    throw new OrderCancellationNotAllowedException(
                            "Cannot cancel order " + order.getId() + ": shipment already " + status);
                });
    }
}
