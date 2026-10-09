package com.ecommerce.inventory.domain.port;

import java.time.Instant;
import java.util.Map;

/**
 * Port: real-time figures derived from the stream of order events (implemented with Kafka Streams state
 * stores). Implementations return empty/zero values, never throw, while the stream is still starting.
 */
public interface OrderStatistics {

    /** Running total of {@code totalAmount} over every OrderCreatedEvent seen. */
    double totalRevenue();

    /** Units ordered per productId, summed over the 5-minute windows that started at or after {@code since}. */
    Map<String, Long> unitsOrderedSince(Instant since);
}
