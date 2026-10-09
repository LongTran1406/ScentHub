package com.ecommerce.inventory.infrastructure.streams;

import com.ecommerce.inventory.domain.port.OrderStatistics;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Reads the Kafka Streams state stores ("revenue-store", "product-demand-store") through interactive queries. */
@Component
public class StreamsOrderStatistics implements OrderStatistics {

    private static final Logger log = LoggerFactory.getLogger(StreamsOrderStatistics.class);

    private final InteractiveQueryService interactiveQueryService;

    public StreamsOrderStatistics(InteractiveQueryService interactiveQueryService) {
        this.interactiveQueryService = interactiveQueryService;
    }

    @Override
    public double totalRevenue() {
        try {
            ReadOnlyKeyValueStore<String, Double> store = interactiveQueryService.getQueryableStore(
                    RevenueStreamsTopology.REVENUE_STORE, QueryableStoreTypes.<String, Double>keyValueStore());
            Double total = store.get("TOTAL");
            return total != null ? total : 0.0;
        } catch (Exception e) {
            // Store not ready yet (e.g. streams still starting / rebalancing)
            log.warn("Could not query {}, returning 0.0: {}", RevenueStreamsTopology.REVENUE_STORE, e.getMessage());
            return 0.0;
        }
    }

    @Override
    public Map<String, Long> unitsOrderedSince(Instant since) {
        Map<String, Long> totals = new HashMap<>();
        try {
            ReadOnlyWindowStore<String, Long> store = interactiveQueryService.getQueryableStore(
                    ProductDemandTopology.DEMAND_STORE, QueryableStoreTypes.<String, Long>windowStore());
            try (KeyValueIterator<Windowed<String>, Long> it = store.fetchAll(since, Instant.now())) {
                while (it.hasNext()) {
                    var entry = it.next();
                    totals.merge(entry.key.key(), entry.value, Long::sum);
                }
            }
        } catch (Exception e) {
            log.warn("Could not query {}, returning empty result: {}", ProductDemandTopology.DEMAND_STORE, e.getMessage());
            return Map.of();
        }
        return totals;
    }
}
