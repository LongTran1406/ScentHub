package com.ecommerce.order.domain.port;

import com.ecommerce.order.domain.event.DomainEvent;

import java.util.List;

/** Port: hands domain events to whatever messaging system carries them to other contexts. */
public interface OrderEventPublisher {

    void publish(List<DomainEvent> events);
}
