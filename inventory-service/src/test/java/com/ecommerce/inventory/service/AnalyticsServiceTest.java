package com.ecommerce.inventory.service;

import com.ecommerce.inventory.domain.model.Money;
import com.ecommerce.inventory.domain.model.Product;
import com.ecommerce.inventory.domain.port.OrderStatistics;
import com.ecommerce.inventory.service.dto.LowStockResponse;
import com.ecommerce.inventory.service.dto.TrendingProductResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsServiceTest {

    private final StockServiceTest.InMemoryProducts products = new StockServiceTest.InMemoryProducts();

    private final Map<String, Long> windowTotals = new LinkedHashMap<>();
    private Instant lastSince;
    private final OrderStatistics statistics = new OrderStatistics() {
        public double totalRevenue() { return 123.45; }
        public Map<String, Long> unitsOrderedSince(Instant since) { lastSince = since; return windowTotals; }
    };

    private final AnalyticsService service = new AnalyticsService(products, statistics);

    @Test
    void lowStockUsesAvailableQuantityNotRawStock() {
        Product plenty = products.save(Product.create("Plenty", "d", Money.of("1"), 100));
        Product low = products.save(Product.create("Low", "d", Money.of("1"), 12));
        low.reserve(5); // available 7

        List<LowStockResponse> result = service.findLowStock(10);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.getProductId()).isEqualTo(low.getId());
            assertThat(r.getAvailableQuantity()).isEqualTo(7);
            assertThat(r.getThreshold()).isEqualTo(10);
        });
        assertThat(plenty.availableQuantity()).isEqualTo(100);
    }

    @Test
    void revenueComesFromTheStreamStatistics() {
        assertThat(service.totalRevenue()).isEqualTo(123.45);
    }

    @Test
    void trendingProductsAreRankedByUnitsAndCapped() {
        Product rose = products.save(Product.create("Rose Eau", "d", Money.of("1"), 100));
        Product oud = products.save(Product.create("Oud Noir", "d", Money.of("1"), 100));
        windowTotals.put(rose.getId().toString(), 3L);
        windowTotals.put(oud.getId().toString(), 8L);
        windowTotals.put("not-a-uuid", 1L);

        List<TrendingProductResponse> top2 = service.trendingProducts(10, 2);

        assertThat(top2).extracting(TrendingProductResponse::getUnitsOrdered).containsExactly(8L, 3L);
        assertThat(top2).extracting(TrendingProductResponse::getName).containsExactly("Oud Noir", "Rose Eau");
        assertThat(lastSince).isBefore(Instant.now().minusSeconds(9 * 60));
    }

    @Test
    void unknownProductsAppearWithoutAName() {
        windowTotals.put("not-a-uuid", 4L);

        assertThat(service.trendingProducts(10, 5)).singleElement()
                .satisfies(t -> assertThat(t.getName()).isNull());
    }

    @Test
    void anEmptyStreamGivesAnEmptyList() {
        assertThat(service.trendingProducts(10, 5)).isEmpty();
    }
}
