package com.ecommerce.order.domain.model;

/** Lifecycle of an order. SHIPPED and COMPLETED are reserved for future use (nothing sets them yet). */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    SHIPPED,
    COMPLETED
}
