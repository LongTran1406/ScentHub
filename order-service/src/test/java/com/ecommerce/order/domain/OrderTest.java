package com.ecommerce.order.domain;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.event.OrderCancelled;
import com.ecommerce.order.domain.event.OrderConfirmed;
import com.ecommerce.order.domain.event.OrderPlaced;
import com.ecommerce.order.domain.event.OrderRejected;
import com.ecommerce.order.domain.exception.InvalidOrderException;
import com.ecommerce.order.domain.exception.OrderCancellationNotAllowedException;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderLineDraft;
import com.ecommerce.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final UUID customerId = UUID.randomUUID();
    private final UUID productA = UUID.randomUUID();
    private final UUID productB = UUID.randomUUID();

    private Order newOrder() {
        return Order.place(customerId, List.of(
                new OrderLineDraft(productA, 2, Money.of("79.99")),
                new OrderLineDraft(productB, 1, Money.of("10.00"))));
    }

    @Test
    void placeStartsPendingAndSumsLinesAtTheGivenPrices() {
        Order order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualTo(Money.of("169.98"));
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems().get(0).lineTotal()).isEqualTo(Money.of("159.98"));
    }

    @Test
    void placeRaisesAnOrderPlacedEventWithTheLines() {
        Order order = newOrder();

        List<DomainEvent> events = order.pullEvents();

        assertThat(events).hasSize(1);
        OrderPlaced placed = (OrderPlaced) events.get(0);
        assertThat(placed.orderId()).isEqualTo(order.getId());
        assertThat(placed.customerId()).isEqualTo(customerId);
        assertThat(placed.totalAmount()).isEqualTo(Money.of("169.98"));
        assertThat(placed.lines()).hasSize(2);
    }

    @Test
    void pullEventsClearsThem() {
        Order order = newOrder();
        order.pullEvents();

        assertThat(order.pullEvents()).isEmpty();
    }

    @Test
    void placeRejectsInvalidInput() {
        assertThatThrownBy(() -> Order.place(null, List.of(new OrderLineDraft(productA, 1, Money.ZERO))))
                .isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> Order.place(customerId, List.of()))
                .isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> Order.place(customerId, List.of(new OrderLineDraft(productA, 0, Money.ZERO))))
                .isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> Order.place(customerId, List.of(new OrderLineDraft(null, 1, Money.ZERO))))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void confirmMovesPendingToConfirmedAndRaisesEvent() {
        Order order = newOrder();
        order.pullEvents();

        assertThat(order.confirm()).isTrue();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.pullEvents()).singleElement().isInstanceOf(OrderConfirmed.class);
    }

    @Test
    void confirmIsIgnoredUnlessPending() {
        Order order = newOrder();
        order.cancel();
        order.pullEvents();

        assertThat(order.confirm()).isFalse();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.pullEvents()).isEmpty();
    }

    @Test
    void rejectStoresTheReasonAndRaisesAnInternalEvent() {
        Order order = newOrder();
        order.pullEvents();

        assertThat(order.reject("INSUFFICIENT_STOCK")).isTrue();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getRejectionReason()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(order.pullEvents()).singleElement().isInstanceOf(OrderRejected.class);
    }

    @Test
    void rejectIsIgnoredOnceConfirmed() {
        Order order = newOrder();
        order.confirm();

        assertThat(order.reject("late")).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelRecordsThePreviousStatus() {
        Order order = newOrder();
        order.confirm();
        order.pullEvents();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        OrderCancelled cancelled = (OrderCancelled) order.pullEvents().get(0);
        assertThat(cancelled.previousStatus()).isEqualTo("CONFIRMED");
        assertThat(cancelled.lines()).hasSize(2);
    }

    @Test
    void pendingOrdersCanBeCancelled() {
        Order order = newOrder();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelledAndRejectedOrdersCannotBeCancelledAgain() {
        Order cancelled = newOrder();
        cancelled.cancel();
        Order rejected = newOrder();
        rejected.reject("x");

        assertThatThrownBy(cancelled::cancel)
                .isInstanceOf(OrderCancellationNotAllowedException.class)
                .hasMessageContaining("already CANCELLED");
        assertThatThrownBy(rejected::cancel)
                .isInstanceOf(OrderCancellationNotAllowedException.class)
                .hasMessageContaining("already REJECTED");
    }
}
