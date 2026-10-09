package com.ecommerce.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object: an amount of money (single implicit currency). Immutable, compared by value, never negative.
 * Persisted as an embedded column of the entity that owns it (see @AttributeOverride on Product).
 */
@Embeddable
public class Money {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    protected Money() {
        // for JPA
    }

    public Money(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Money amount must be zero or positive: " + amount);
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    public BigDecimal amount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other && amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
