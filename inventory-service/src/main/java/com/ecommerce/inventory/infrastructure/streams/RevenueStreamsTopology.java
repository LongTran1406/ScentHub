package com.ecommerce.inventory.infrastructure.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class RevenueStreamsTopology {

    private static final Logger log = LoggerFactory.getLogger(RevenueStreamsTopology.class);
    static final String REVENUE_STORE = "revenue-store";

    /**
     * Spring Cloud Stream (Kafka Streams binder) topology. The bean name ("revenue") is what the
     * binding "revenue-in-0" in application.yml refers to (it reads the "order-events" topic).
     *  1. Filters only "OrderCreatedEvent" messages
     *  2. Extracts totalAmount from each message's payload
     *  3. Groups everything under a single key ("TOTAL")
     *  4. Aggregates into a running sum persisted in the "revenue-store" state store
     */
    @Bean
    public Consumer<KStream<String, String>> revenue() {
        ObjectMapper mapper = new ObjectMapper();

        return source -> source
            // 1. Filter only OrderCreatedEvent messages
            .filter((key, value) -> {
                try {
                    JsonNode root = mapper.readTree(value);
                    return "OrderCreatedEvent".equals(root.path("eventType").asText());
                } catch (Exception e) {
                    log.warn("Could not parse Kafka message for filtering: {}", e.getMessage());
                    return false;
                }
            })
            // 2. Extract totalAmount as a string (re-key to constant "TOTAL")
            .selectKey((key, value) -> "TOTAL")
            // 3. Map to the totalAmount value
            .mapValues(value -> {
                try {
                    JsonNode root = mapper.readTree(value);
                    return root.path("payload").path("totalAmount").asText("0.0");
                } catch (Exception e) {
                    log.warn("Could not extract totalAmount: {}", e.getMessage());
                    return "0.0";
                }
            })
            // 4. Group by the constant key "TOTAL"
            .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            // 5. Aggregate into a running Double sum stored in "revenue-store"
            .aggregate(
                () -> 0.0,
                (key, amountStr, currentTotal) -> {
                    try {
                        return currentTotal + Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse totalAmount [{}]: {}", amountStr, e.getMessage());
                        return currentTotal;
                    }
                },
                Materialized.<String, Double>as(
                    Stores.persistentKeyValueStore(REVENUE_STORE))
                    .withKeySerde(Serdes.String())
                    .withValueSerde(Serdes.Double())
            );
    }
}
