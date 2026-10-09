package com.ecommerce.inventory.infrastructure.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Real-time "trending products" analysis (Kafka Streams binder). The bean name ("demand") is what the
 * binding "demand-in-0" in application.yml refers to (it reads the "order-events" topic).
 *
 * Equivalent streaming query (KSQL-style):
 * <pre>
 *   SELECT productId, SUM(quantity) FROM orderCreatedItems
 *   WINDOW TUMBLING (SIZE 5 MINUTES) GROUP BY productId;
 * </pre>
 *  1. Filter "OrderCreatedEvent" messages
 *  2. Explode each order into one (productId, quantity) record per line item
 *  3. Group by productId and sum quantity in 5-minute tumbling windows (event time = Kafka record timestamp)
 *  4. Materialize into the windowed state store "product-demand-store"
 */
@Configuration
public class ProductDemandTopology {

    private static final Logger log = LoggerFactory.getLogger(ProductDemandTopology.class);
    static final String DEMAND_STORE = "product-demand-store";
    static final Duration WINDOW_SIZE = Duration.ofMinutes(5);

    @Bean
    public Consumer<KStream<String, String>> demand() {
        ObjectMapper mapper = new ObjectMapper();

        return source -> source
            .filter((key, value) -> {
                try {
                    return "OrderCreatedEvent".equals(mapper.readTree(value).path("eventType").asText());
                } catch (Exception e) {
                    log.warn("Could not parse Kafka message for filtering: {}", e.getMessage());
                    return false;
                }
            })
            .flatMap((key, value) -> {
                List<KeyValue<String, Long>> lines = new ArrayList<>();
                try {
                    for (JsonNode item : mapper.readTree(value).path("payload").path("items")) {
                        String productId = item.path("productId").asText();
                        long quantity = item.path("quantity").asLong(0);
                        if (!productId.isEmpty() && quantity > 0) {
                            lines.add(KeyValue.pair(productId, quantity));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not extract order items: {}", e.getMessage());
                }
                return lines;
            })
            .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(WINDOW_SIZE))
            .aggregate(
                () -> 0L,
                (productId, quantity, total) -> total + quantity,
                Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(DEMAND_STORE)
                    .withKeySerde(Serdes.String())
                    .withValueSerde(Serdes.Long())
            );
    }
}
