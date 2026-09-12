package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.LowStockResponse;
import com.ecommerce.inventory.dto.RevenueResponse;
import com.ecommerce.inventory.service.ProductService;
import com.ecommerce.inventory.streams.RevenueQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final ProductService productService;
    private final RevenueQueryService revenueQueryService;

    public AnalyticsController(ProductService productService,
                               RevenueQueryService revenueQueryService) {
        this.productService = productService;
        this.revenueQueryService = revenueQueryService;
    }

    /**
     * GET /analytics/low-stock?threshold=10
     * Returns products where (stockQuantity - reservedQuantity) < threshold.
     * Defaults to threshold=10 if not provided.
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockResponse>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(productService.findLowStock(threshold));
    }

    /**
     * GET /analytics/revenue
     * Returns the running total revenue from all OrderCreatedEvents processed by Kafka Streams.
     */
    @GetMapping("/revenue")
    public ResponseEntity<RevenueResponse> getRevenue() {
        Double total = revenueQueryService.getTotalRevenue();
        return ResponseEntity.ok(new RevenueResponse(total));
    }
}
