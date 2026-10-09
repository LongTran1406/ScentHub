package com.ecommerce.order.domain.model;

import java.util.UUID;

/** Value object: one requested line of a new order, already priced by the application service. */
public record OrderLineDraft(UUID productId, int quantity, Money unitPrice) {
}
