package com.ecommerce.inventory.domain;

import com.ecommerce.inventory.domain.event.InventoryEvent;
import com.ecommerce.inventory.domain.event.StockLine;
import com.ecommerce.inventory.domain.event.StockRejected;
import com.ecommerce.inventory.domain.event.StockReserved;
import com.ecommerce.inventory.domain.model.Money;
import com.ecommerce.inventory.domain.model.Product;
import com.ecommerce.inventory.domain.service.StockReservationService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StockReservationServiceTest {

    private final StockReservationService service = new StockReservationService();

    private Product product(int stock) {
        return Product.create("P", "d", Money.of("10"), stock);
    }

    private Map<UUID, Product> catalogue(Product... products) {
        Map<UUID, Product> map = new HashMap<>();
        for (Product p : products) {
            map.put(p.getId(), p);
        }
        return map;
    }

    @Test
    void reservesEveryLineWhenAllHaveEnoughStock() {
        Product a = product(5);
        Product b = product(5);

        InventoryEvent outcome = service.reserveAll("o1",
                List.of(new StockLine(a.getId(), 2), new StockLine(b.getId(), 5)), catalogue(a, b));

        assertThat(outcome).isInstanceOf(StockReserved.class);
        assertThat(a.getReservedQuantity()).isEqualTo(2);
        assertThat(b.getReservedQuantity()).isEqualTo(5);
    }

    @Test
    void reservesNothingWhenAnyLineIsShort() {
        Product a = product(5);
        Product b = product(1);

        InventoryEvent outcome = service.reserveAll("o1",
                List.of(new StockLine(a.getId(), 2), new StockLine(b.getId(), 3)), catalogue(a, b));

        StockRejected rejected = (StockRejected) outcome;
        assertThat(rejected.reason()).isEqualTo(StockReservationService.INSUFFICIENT_STOCK);
        assertThat(rejected.shortages()).singleElement().satisfies(s -> {
            assertThat(s.productId()).isEqualTo(b.getId());
            assertThat(s.requested()).isEqualTo(3);
            assertThat(s.available()).isEqualTo(1);
        });
        assertThat(a.getReservedQuantity()).as("all-or-nothing: the line that fit is not reserved").isZero();
        assertThat(b.getReservedQuantity()).isZero();
    }

    @Test
    void anUnknownProductRejectsTheWholeOrder() {
        Product a = product(5);
        UUID missing = UUID.randomUUID();

        InventoryEvent outcome = service.reserveAll("o1",
                List.of(new StockLine(a.getId(), 1), new StockLine(missing, 1)), catalogue(a));

        StockRejected rejected = (StockRejected) outcome;
        assertThat(rejected.reason()).isEqualTo(StockReservationService.PRODUCT_NOT_FOUND);
        assertThat(rejected.shortages()).singleElement().satisfies(s -> assertThat(s.productId()).isEqualTo(missing));
        assertThat(a.getReservedQuantity()).isZero();
    }

    @Test
    void linesForTheSameProductAreSummedSoOrdersCannotOversell() {
        Product a = product(5);

        InventoryEvent outcome = service.reserveAll("o1",
                List.of(new StockLine(a.getId(), 3), new StockLine(a.getId(), 3)), catalogue(a));

        StockRejected rejected = (StockRejected) outcome;
        assertThat(rejected.shortages()).singleElement().satisfies(s -> {
            assertThat(s.requested()).isEqualTo(6);
            assertThat(s.available()).isEqualTo(5);
        });
        assertThat(a.getReservedQuantity()).isZero();
    }

    @Test
    void splitLinesThatFitAreReservedOnce() {
        Product a = product(5);

        InventoryEvent outcome = service.reserveAll("o1",
                List.of(new StockLine(a.getId(), 2), new StockLine(a.getId(), 3)), catalogue(a));

        assertThat(outcome).isInstanceOf(StockReserved.class);
        assertThat(a.getReservedQuantity()).isEqualTo(5);
    }
}
