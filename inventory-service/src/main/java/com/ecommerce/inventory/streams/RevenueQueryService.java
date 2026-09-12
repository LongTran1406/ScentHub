package com.ecommerce.inventory.streams;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@Service
public class RevenueQueryService {

    private static final Logger log = LoggerFactory.getLogger(RevenueQueryService.class);
    private static final String REVENUE_STORE = "revenue-store";

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public RevenueQueryService(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    /**
     * Query the Kafka Streams state store for the running total revenue.
     *
     * @return total revenue seen from all OrderCreatedEvent messages, or 0.0 if unavailable
     */
    public Double getTotalRevenue() {
        try {
            KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
            if (streams == null || streams.state() != KafkaStreams.State.RUNNING) {
                log.warn("KafkaStreams is not in RUNNING state — returning 0.0");
                return 0.0;
            }

            ReadOnlyKeyValueStore<String, Double> store = streams.store(
                    StoreQueryParameters.fromNameAndType(REVENUE_STORE,
                            QueryableStoreTypes.keyValueStore())
            );

            Double total = store.get("TOTAL");
            return total != null ? total : 0.0;
        } catch (Exception e) {
            log.error("Could not query revenue-store: {}", e.getMessage(), e);
            return 0.0;
        }
    }
}
