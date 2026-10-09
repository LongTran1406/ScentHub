package com.ecommerce.inventory.presentation.controller;

import com.ecommerce.inventory.service.AnalyticsService;
import com.ecommerce.inventory.service.dto.LowStockResponse;
import com.ecommerce.inventory.service.dto.RevenueResponse;
import com.ecommerce.inventory.service.dto.TrendingProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * GET /analytics/low-stock?threshold=10
     * Returns products where (stockQuantity - reservedQuantity) < threshold.
     * Defaults to threshold=10 if not provided.
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockResponse>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(analyticsService.findLowStock(threshold));
    }

    /**
     * GET /analytics/revenue
     * Returns the running total revenue from all OrderCreatedEvents processed by Kafka Streams.
     */
    @GetMapping("/revenue")
    public ResponseEntity<RevenueResponse> getRevenue() {
        return ResponseEntity.ok(new RevenueResponse(analyticsService.totalRevenue()));
    }

    /**
     * GET /analytics/trending-products?minutes=10&limit=5
     * Returns the products ordered most in the last {@code minutes} minutes, computed in real time by the
     * Kafka Streams "demand" topology (5-minute tumbling windows over OrderCreatedEvents).
     */
    @GetMapping("/trending-products")
    public ResponseEntity<List<TrendingProductResponse>> getTrendingProducts(
            @RequestParam(defaultValue = "10") int minutes,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(analyticsService.trendingProducts(minutes, limit));
    }
}
