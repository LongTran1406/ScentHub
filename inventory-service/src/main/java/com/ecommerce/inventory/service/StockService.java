package com.ecommerce.inventory.service;

import com.ecommerce.inventory.domain.event.InventoryEvent;
import com.ecommerce.inventory.domain.event.StockLine;
import com.ecommerce.inventory.domain.model.Product;
import com.ecommerce.inventory.domain.port.InventoryEventPublisher;
import com.ecommerce.inventory.domain.repository.ProductRepository;
import com.ecommerce.inventory.domain.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Application service for the order-driven stock use cases: reserve for a new order, release on cancellation. */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductRepository productRepository;
    private final StockReservationService reservationService;
    private final InventoryEventPublisher eventPublisher;

    public StockService(ProductRepository productRepository,
                        StockReservationService reservationService,
                        InventoryEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.reservationService = reservationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Reserves stock for every line of the order, or for none of them, and announces the outcome
     * (StockReserved / StockRejected) once the stock change is committed.
     */
    @Transactional
    public void reserveForOrder(String orderId, List<StockLine> lines) {
        Map<UUID, Product> products = loadProducts(lines);
        InventoryEvent outcome = reservationService.reserveAll(orderId, lines, products);
        products.values().forEach(productRepository::save);
        log.debug("Order {} -> {}", orderId, outcome.getClass().getSimpleName());
        publishAfterCommit(outcome);
    }

    /** Gives reserved stock back after an order was cancelled. No event is published. */
    @Transactional
    public void releaseForOrder(List<StockLine> lines) {
        Map<UUID, Product> products = loadProducts(lines);
        for (StockLine line : lines) {
            Product product = products.get(line.productId());
            if (product == null) {
                log.warn("Cannot release stock for unknown product {}", line.productId());
                continue;
            }
            if (product.release(line.quantity())) {
                log.warn("reservedQuantity would go negative for product [{}]; clamped to 0 - data inconsistency detected.",
                        line.productId());
            }
            productRepository.save(product);
        }
    }

    private Map<UUID, Product> loadProducts(List<StockLine> lines) {
        List<UUID> ids = lines.stream().map(StockLine::productId).distinct().toList();
        Map<UUID, Product> products = new HashMap<>();
        productRepository.findAllById(ids).forEach(p -> products.put(p.getId(), p));
        return products;
    }

    private void publishAfterCommit(InventoryEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publish(event);
                }
            });
        } else {
            eventPublisher.publish(event);
        }
    }
}
