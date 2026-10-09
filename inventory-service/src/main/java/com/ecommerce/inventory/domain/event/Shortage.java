package com.ecommerce.inventory.domain.event;

import java.util.UUID;

/** Value object: one product that could not be reserved in the requested quantity. */
public record Shortage(UUID productId, int requested, int available) {
}
