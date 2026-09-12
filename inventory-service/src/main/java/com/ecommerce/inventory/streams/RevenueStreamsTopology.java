package com.ecommerce.inventory.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RevenueStreamsTopology {

    private static final Logger log = LoggerFactory.getLogger(RevenueStreamsTopology.class);
    private static final String REVENUE_STORE = "revenue-store";

    /**
     * Builds a Kafka Streams topology that:
     *  1. Reads raw JSON strings from "order-events"
     *  2. Filters only "OrderCreatedEvent" messages
     *  3. Extracts totalAmount from each message's payload
     *  4. Groups everything under a single key ("TOTAL")
     *  5. Aggregates into a running sum persisted in the "revenue-store" state store
     */
    @Bean
    public KStream<String, String> revenueStream(StreamsBuilder streamsBuilder) {
        ObjectMapper mapper = new ObjectMapper();

        KStream<String, String> source = streamsBuilder.stream("order-events");

        source
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
            .groupByKey()
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

        return source;
    }
}
