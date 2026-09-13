// TEMP: unitPrice hardcoded in OrderService until Product Service
// integration is added (see Task 1.5)
package com.ecommerce.order.dto;

import java.util.List;
import java.util.UUID;

public class CreateOrderRequest {

    private UUID customerId;
    private List<OrderItemRequest> items;

    public CreateOrderRequest() {}

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
