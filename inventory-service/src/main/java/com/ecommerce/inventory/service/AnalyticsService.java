package com.ecommerce.inventory.service;

import com.ecommerce.inventory.domain.port.OrderStatistics;
import com.ecommerce.inventory.domain.repository.ProductRepository;
import com.ecommerce.inventory.service.dto.LowStockResponse;
import com.ecommerce.inventory.service.dto.TrendingProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Application service for the analytics use cases: low stock (database) and revenue/trending (streams). */
@Service
public class AnalyticsService {

    private final ProductRepository productRepository;
    private final OrderStatistics orderStatistics;

    public AnalyticsService(ProductRepository productRepository, OrderStatistics orderStatistics) {
        this.productRepository = productRepository;
        this.orderStatistics = orderStatistics;
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> findLowStock(int threshold) {
        return productRepository.findLowStock(threshold).stream()
                .map(p -> new LowStockResponse(p.getId(), p.getName(), p.availableQuantity(), threshold))
                .toList();
    }

    public double totalRevenue() {
        return orderStatistics.totalRevenue();
    }

    /** Most-ordered products over the last {@code minutes} minutes, highest first. */
    @Transactional(readOnly = true)
    public List<TrendingProductResponse> trendingProducts(int minutes, int limit) {
        Map<String, Long> units = orderStatistics.unitsOrderedSince(Instant.now().minusSeconds(minutes * 60L));
        return units.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(Math.max(limit, 0))
                .map(e -> new TrendingProductResponse(e.getKey(), nameOf(e.getKey()), e.getValue()))
                .toList();
    }

    private String nameOf(String productId) {
        try {
            return productRepository.findById(UUID.fromString(productId)).map(p -> p.getName()).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
