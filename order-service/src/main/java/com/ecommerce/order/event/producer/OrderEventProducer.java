package com.ecommerce.order.event.producer;

import com.ecommerce.order.event.dto.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String eventType, Object payload) {
        try {
            EventEnvelope envelope = new EventEnvelope(
                    eventType,
                    UUID.randomUUID().toString(),
                    Instant.now().toString(),
                    payload
            );
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC, json);
            log.debug("Published event [{}] to topic [{}]", eventType, TOPIC);
        } catch (Exception e) {
            log.error("Failed to publish event [{}]: {}", eventType, e.getMessage(), e);
        }
    }
}
