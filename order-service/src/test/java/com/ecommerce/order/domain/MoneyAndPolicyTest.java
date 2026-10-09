package com.ecommerce.order.domain;

import com.ecommerce.order.domain.exception.OrderCancellationNotAllowedException;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderLineDraft;
import com.ecommerce.order.domain.service.CancellationPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyAndPolicyTest {

    private final CancellationPolicy policy = new CancellationPolicy();

    private Order pendingOrder() {
        return Order.place(UUID.randomUUID(), List.of(new OrderLineDraft(UUID.randomUUID(), 1, Money.of("5"))));
    }

    @Test
    void moneyIsComparedByValueAndNormalisedToTwoDecimals() {
        assertThat(Money.of("10")).isEqualTo(Money.of("10.00"));
        assertThat(Money.of("10").hashCode()).isEqualTo(Money.of("10.00").hashCode());
        assertThat(Money.of("74.99").times(3)).isEqualTo(Money.of("224.97"));
        assertThat(Money.of("1.10").plus(Money.of("2.20"))).isEqualTo(Money.of("3.30"));
    }

    @Test
    void moneyCannotBeNegativeOrNull() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-0.01"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anOrderWithNoShipmentCanBeCancelled() {
        assertThatCode(() -> policy.verifyCancellable(pendingOrder(), Optional::empty)).doesNotThrowAnyException();
    }

    @Test
    void aShipmentThatHasNotLeftTheWarehouseDoesNotBlockCancellation() {
        assertThatCode(() -> policy.verifyCancellable(pendingOrder(), () -> Optional.of("CREATED")))
                .doesNotThrowAnyException();
    }

    @Test
    void dispatchedShipmentsBlockCancellation() {
        for (String status : List.of("DISPATCHED", "IN_TRANSIT", "DELIVERED")) {
            assertThatThrownBy(() -> policy.verifyCancellable(pendingOrder(), () -> Optional.of(status)))
                    .isInstanceOf(OrderCancellationNotAllowedException.class)
                    .hasMessageContaining("shipment already " + status);
        }
    }

    @Test
    void shippingIsNotAskedWhenTheOrderStatusAlreadyForbidsCancelling() {
        Order cancelled = pendingOrder();
        cancelled.cancel();
        AtomicInteger lookups = new AtomicInteger();

        assertThatThrownBy(() -> policy.verifyCancellable(cancelled, () -> {
            lookups.incrementAndGet();
            return Optional.empty();
        })).isInstanceOf(OrderCancellationNotAllowedException.class);

        assertThat(lookups).hasValue(0);
    }
}
