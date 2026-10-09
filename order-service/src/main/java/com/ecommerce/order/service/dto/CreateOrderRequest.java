// Request body of POST /orders. Unit prices are NOT sent by the client: OrderService looks them up
// in the Inventory Service, and also checks that the customer and every product exist.
package com.ecommerce.order.service.dto;

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
