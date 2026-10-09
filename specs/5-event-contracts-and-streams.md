# 5. Event contracts and stream processing

> Source material for report Section 3.5. Every message on both topics is a JSON string in the same envelope.

## 5.1 Envelope
```json
{ "eventType": "OrderCreatedEvent", "eventId": "<uuid>", "timestamp": "<ISO-8601>", "payload": { } }
```
Consumers read `eventType` first and route on it; unknown types are logged and ignored. Producers serialise with
Jackson; the key is not set. Topics have one partition (local development).

## 5.2 Topic `order-events` - published by Order Service

| eventType | Raised when | Payload |
|---|---|---|
| `OrderCreatedEvent` | an order is placed (`PENDING`) | `{orderId, customerId, items:[{productId, quantity}], totalAmount}` |
| `OrderConfirmedEvent` | inventory reserved the stock | `{orderId, customerId, items:[{productId, quantity}]}` |
| `OrderCancelledEvent` | an order was cancelled | `{orderId, items:[{productId, quantity}], previousStatus}` |

(There is no `OrderRejectedEvent` on the wire: rejection is a dead end by design. The domain raises `OrderRejected`
internally and the publisher drops it.)

| Consumer | Group | Reacts to |
|---|---|---|
| Inventory Service | `inventory-service` | `OrderCreatedEvent` → reserve stock (all-or-nothing) · `OrderCancelledEvent` → release stock · others ignored |
| Shipping Service | `shipping-service` | `OrderConfirmedEvent` → create shipment (idempotent) · `OrderCancelledEvent` → cancel shipment if still `CREATED` · others ignored |
| Inventory Kafka Streams | `inventory-service-streams`, `inventory-service-demand` | `OrderCreatedEvent` → revenue and trending aggregations |

## 5.3 Topic `inventory-events` - published by Inventory Service

| eventType | Raised when | Payload |
|---|---|---|
| `InventoryReservedEvent` | every line of an order was reserved | `{orderId, items:[{productId, quantity}]}` |
| `InventoryRejectedEvent` | nothing was reserved | `{orderId, reason, unavailableItems:[{productId, requested, available}]}` with `reason` = `INSUFFICIENT_STOCK` or `PRODUCT_NOT_FOUND` |

Consumer: Order Service (group `order-service`): reserved → order `CONFIRMED` (+ `OrderConfirmedEvent`); rejected →
order `REJECTED` with the reason. A reply for an order that is no longer `PENDING` is ignored.

## 5.4 Spring Cloud Stream bindings

| Service | Binding | Topic | Group | Direction |
|---|---|---|---|---|
| order | `inventoryEvents-in-0` | `inventory-events` | `order-service` | consume (`Consumer<String>` bean `inventoryEvents`) |
| order | `orderEvents-out-0` | `order-events` | – | publish through `StreamBridge` |
| inventory | `orderEvents-in-0` | `order-events` | `inventory-service` | consume (bean `orderEvents`) |
| inventory | `inventoryEvents-out-0` | `inventory-events` | – | publish through `StreamBridge` |
| inventory | `revenue-in-0` | `order-events` | – | Kafka Streams function `revenue` |
| inventory | `demand-in-0` | `order-events` | – | Kafka Streams function `demand` |
| shipping | `orderEvents-in-0` | `order-events` | `shipping-service` | consume (bean `orderEvents`) |

Sample code (publishing - translating a domain event into the message):
```java
case OrderPlaced e -> send("OrderCreatedEvent", new OrderCreatedPayload(
        e.orderId().toString(), e.customerId().toString(), items(e.lines()), e.totalAmount().amount()));
...
streamBridge.send("orderEvents-out-0", objectMapper.writeValueAsString(envelope));
```
Sample code (consuming):
```java
@Component("orderEvents")
public class OrderEventConsumer implements Consumer<String> {
    public void accept(String rawMessage) {
        JsonNode root = objectMapper.readTree(rawMessage);
        switch (root.path("eventType").asText()) {
            case "OrderConfirmedEvent" -> shipmentService.createShipment(orderIdOf(root));
            case "OrderCancelledEvent" -> shipmentService.cancelIfNotDispatched(orderIdOf(root));
            default -> log.debug("Ignoring event type");
        }
    }
}
```

## 5.5 Stream-processing queries (Inventory Service)

**Revenue** - function `revenue`, store `revenue-store`
```text
order-events ─ filter(eventType = OrderCreatedEvent)
             ─ selectKey("TOTAL") ─ mapValues(payload.totalAmount)
             ─ groupByKey ─ aggregate(0.0, (k, amount, total) -> total + amount) ─▶ revenue-store
```
SQL-like: `SELECT SUM(totalAmount) FROM order_events WHERE eventType = 'OrderCreatedEvent';`

**Trending products** - function `demand`, windowed store `product-demand-store`
```text
order-events ─ filter(eventType = OrderCreatedEvent)
             ─ flatMap(order -> one (productId, quantity) per line item)
             ─ groupByKey ─ windowedBy(tumbling 5 min) ─ aggregate(0, (k, qty, total) -> total + qty)
             ─▶ product-demand-store
```
SQL-like: `SELECT productId, SUM(quantity) FROM order_created_items WINDOW TUMBLING (SIZE 5 MINUTES) GROUP BY productId;`

Reading: `StreamsOrderStatistics` uses `InteractiveQueryService.getQueryableStore(...)`; `AnalyticsService` ranks the
per-product sums of the windows that started within the requested range and adds product names.

**Operational notes.** Each function is its own `KafkaStreams` instance, so each has its own `applicationId` and
`state.dir` (`inventory-service-streams` / `inventory-service-demand`). Streams start from the earliest offset, so a
fresh start replays history. Revenue includes orders that inventory later rejects (deliberately simple).

## 5.6 How the event flow is tested
- Without Kafka: `OrderMessagingTest`, `InventoryMessagingTest`, `OrderEventsBindingTest` put JSON messages into the
  real bindings (Spring Cloud Stream test binder) and assert on the JSON that comes out.
- With Kafka: `scripts/smoke-test.sh` runs the full loop across the four services including both stream queries.
