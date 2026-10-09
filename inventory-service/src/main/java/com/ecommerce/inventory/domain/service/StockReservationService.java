package com.ecommerce.inventory.domain.service;

import com.ecommerce.inventory.domain.event.InventoryEvent;
import com.ecommerce.inventory.domain.event.Shortage;
import com.ecommerce.inventory.domain.event.StockLine;
import com.ecommerce.inventory.domain.event.StockRejected;
import com.ecommerce.inventory.domain.event.StockReserved;
import com.ecommerce.inventory.domain.model.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain service: reserves stock for a whole order <b>all-or-nothing</b>. It spans several {@link Product}
 * aggregates, so it cannot live on any single one of them.
 *
 * <p>Lines for the same product are summed first, so an order cannot oversell by splitting a product across two
 * lines. If any product is missing or short, nothing at all is reserved.
 */
public class StockReservationService {

    public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";

    /**
     * @param products the products referenced by the order, keyed by id (missing keys mean "does not exist")
     * @return the outcome event; the products in {@code products} have been modified only if it is StockReserved
     */
    public InventoryEvent reserveAll(String orderId, List<StockLine> lines, Map<UUID, Product> products) {
        Map<UUID, Integer> requested = new LinkedHashMap<>();
        for (StockLine line : lines) {
            requested.merge(line.productId(), line.quantity(), Integer::sum);
        }

        List<Shortage> shortages = new ArrayList<>();
        boolean anyMissing = false;
        for (Map.Entry<UUID, Integer> entry : requested.entrySet()) {
            Product product = products.get(entry.getKey());
            if (product == null) {
                anyMissing = true;
                shortages.add(new Shortage(entry.getKey(), entry.getValue(), 0));
            } else if (!product.canReserve(entry.getValue())) {
                shortages.add(new Shortage(entry.getKey(), entry.getValue(), product.availableQuantity()));
            }
        }

        if (!shortages.isEmpty()) {
            return new StockRejected(orderId, anyMissing ? PRODUCT_NOT_FOUND : INSUFFICIENT_STOCK, shortages);
        }

        requested.forEach((productId, quantity) -> products.get(productId).reserve(quantity));
        return new StockReserved(orderId, lines);
    }
}
