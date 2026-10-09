package com.ecommerce.inventory.domain;

import com.ecommerce.inventory.domain.exception.InvalidProductException;
import com.ecommerce.inventory.domain.model.Money;
import com.ecommerce.inventory.domain.model.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private Product product(int stock) {
        return Product.create("Rose Eau", "Floral", Money.of("79.99"), stock);
    }

    @Test
    void availableIsStockMinusReserved() {
        Product p = product(10);
        p.reserve(3);

        assertThat(p.getStockQuantity()).isEqualTo(10);
        assertThat(p.getReservedQuantity()).isEqualTo(3);
        assertThat(p.availableQuantity()).isEqualTo(7);
    }

    @Test
    void canReserveOnlyWithinAvailableStock() {
        Product p = product(5);

        assertThat(p.canReserve(5)).isTrue();
        assertThat(p.canReserve(6)).isFalse();
        assertThat(p.canReserve(0)).isFalse();
        assertThat(p.canReserve(-1)).isFalse();
    }

    @Test
    void reservingMoreThanAvailableIsRefused() {
        Product p = product(2);

        assertThatThrownBy(() -> p.reserve(3)).isInstanceOf(InvalidProductException.class);
        assertThat(p.getReservedQuantity()).isZero();
    }

    @Test
    void releaseGivesStockBack() {
        Product p = product(10);
        p.reserve(4);

        boolean clamped = p.release(3);

        assertThat(clamped).isFalse();
        assertThat(p.getReservedQuantity()).isEqualTo(1);
    }

    @Test
    void releaseNeverGoesBelowZeroAndReportsTheInconsistency() {
        Product p = product(10);
        p.reserve(2);

        boolean clamped = p.release(5);

        assertThat(clamped).isTrue();
        assertThat(p.getReservedQuantity()).isZero();
    }

    @Test
    void createValidatesItsInput() {
        assertThatThrownBy(() -> Product.create(" ", "d", Money.of("1"), 1)).isInstanceOf(InvalidProductException.class);
        assertThatThrownBy(() -> Product.create("x", "d", null, 1)).isInstanceOf(InvalidProductException.class);
        assertThatThrownBy(() -> Product.create("x", "d", Money.of("1"), -1)).isInstanceOf(InvalidProductException.class);
        assertThatThrownBy(() -> Product.create("x", "d", Money.of("1"), null)).isInstanceOf(InvalidProductException.class);
    }

    @Test
    void updateChangesOnlyTheProvidedFields() {
        Product p = product(10);

        p.update("New name", null, null, null);

        assertThat(p.getName()).isEqualTo("New name");
        assertThat(p.getDescription()).isEqualTo("Floral");
        assertThat(p.getPrice()).isEqualTo(Money.of("79.99"));
        assertThat(p.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void stockCannotBeLoweredBelowWhatIsReserved() {
        Product p = product(10);
        p.reserve(6);

        assertThatThrownBy(() -> p.update(null, null, null, 5))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("reserved");
        assertThat(p.getStockQuantity()).isEqualTo(10);

        p.update(null, null, null, 6);
        assertThat(p.availableQuantity()).isZero();
    }
}
