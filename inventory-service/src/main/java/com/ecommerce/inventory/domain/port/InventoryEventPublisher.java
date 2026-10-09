package com.ecommerce.inventory.domain.port;

import com.ecommerce.inventory.domain.event.InventoryEvent;

/** Port: hands stock outcomes to whatever messaging system carries them to the Order context. */
public interface InventoryEventPublisher {

    void publish(InventoryEvent event);
}
