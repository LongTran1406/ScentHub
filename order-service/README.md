# Order Service

Member 1's service for the ScentHub platform — order management, Kafka event
production/consumption, and two LangChain4j-powered conversational AI agents.

## What this service does

- Create and view orders (`Order` aggregate with its `OrderItem`s, backed by H2). Creating an order **checks that the
  customer and every product exist and prices each line from the Inventory Service** (no client-supplied prices)
- Publish order lifecycle events to Kafka (`OrderCreatedEvent`, `OrderConfirmedEvent`,
  `OrderCancelledEvent`) on the `order-events` topic
- Consume inventory responses from Kafka (`InventoryReservedEvent`,
  `InventoryRejectedEvent`) on the `inventory-events` topic, and confirm/reject
  orders accordingly
- Cancel orders — checking both the order's own status and Shipping Service's
  live shipment status before allowing it
- Two AI chat agents (via LangChain4j + Groq):
  - **Product Consultation** — answers product questions using real Inventory
    Service data (never hallucinated)
  - **Cancellation Support** — lets a customer cancel an order via natural
    language, using the exact same cancellation logic as the direct REST endpoint

## Tech stack

- Java 21, Spring Boot 3.2.5
- Spring Data JPA + H2 (in-memory, `order_db`)
- Spring Cloud Stream 4.1.x with the Kafka binder (functional style: a `Consumer<String>` bean for input,
  `StreamBridge` for output)
- LangChain4j 1.3.0 + `langchain4j-open-ai` (pointed at Groq's OpenAI-compatible API)
- RestTemplate for synchronous calls to Inventory Service and Shipping Service

## Prerequisites

- Java 21 JDK
- Maven (`mvn -version` should work — see repo root README if not installed)
- Docker + Docker Compose, for the shared Kafka/Zookeeper stack (`docker-compose.yml`
  at the repo root)
- For full end-to-end testing: `inventory-service` (port 8082) and
  `shipping-service` (port 8083) running too — this service calls both over HTTP
  and reacts to their Kafka events

## Configuration

All config lives in `src/main/resources/application.yml`. The one thing you need
to set yourself before the AI agents will actually respond with real answers:

```bash
# Windows PowerShell
$env:GROQ_API_KEY = "your-real-groq-api-key"

# bash
export GROQ_API_KEY="your-real-groq-api-key"
```

Without it, `groq.api-key` falls back to a placeholder value (`gsk_placeholder_...`),
Groq will reject every request with an authentication error, and both `/agents/*`
endpoints will return a fixed fallback message instead of a real AI reply — the
rest of the service (orders, Kafka, cancellation) is completely unaffected by this.

Get a free key at [console.groq.com](https://console.groq.com).

Other config, already set correctly for local dev, no changes needed:

| Property | Value | Used for |
|---|---|---|
| `server.port` | `8081` | this service's own port |
| `spring.cloud.stream.kafka.binder.brokers` | `localhost:9092` | matches `docker-compose.yml` |
| `spring.cloud.stream.bindings.inventoryEvents-in-0` | topic `inventory-events`, group `order-service` | input binding for `InventoryEventConsumer` (bean `inventoryEvents`) |
| `spring.cloud.stream.bindings.orderEvents-out-0` | topic `order-events` | output binding used by `KafkaOrderEventPublisher` via `StreamBridge` |
| `shipping-service.base-url` | `http://localhost:8083` | cancellation's shipment-status check |
| `product-service.base-url` | `http://localhost:8082` | product price/existence check when creating an order, and the AI agent's product lookup |
| `customer-service.base-url` | `http://localhost:8084` | customer existence check when creating an order |
| `groq.base-url` | `https://api.groq.com/openai/v1` | Groq's OpenAI-compatible endpoint |
| `groq.model-name` | `llama-3.1-8b-instant` | which Groq-hosted model to use |

## How to run

1. Start Kafka (from the repo root, `D:\csci318\ScentHub`):
   ```bash
   docker-compose up -d
   ```
2. (Optional, for full functionality) start `inventory-service` and
   `shipping-service` in their own terminals:
   ```bash
   cd inventory-service && mvn spring-boot:run
   cd shipping-service && mvn spring-boot:run
   ```
3. Set `GROQ_API_KEY` (see Configuration above) if you want the AI agents to
   give real answers instead of the fallback message.
4. Start this service:
   ```bash
   cd order-service
   mvn spring-boot:run
   ```
5. Confirm it's up: `Started OrderServiceApplication` in the console, listening
   on port 8081.

H2 console (inspect the `orders`/`order_items` tables directly):
`http://localhost:8081/h2-console` — JDBC URL `jdbc:h2:mem:order_db`, user `sa`,
blank password.

## REST endpoints

| Method | Path | Purpose | Error responses |
|---|---|---|---|
| POST | `/orders` | Create an order | `400` unknown customer/product, empty items, quantity ≤ 0; `503` Inventory/Customer unreachable |
| GET | `/orders/{orderId}` | Get one order | `404` |
| GET | `/orders?customerId={id}` | Get all orders for a customer | – (empty list allowed) |
| POST | `/orders/{orderId}/cancel` | Cancel an order | `404`; `409` not cancellable; `503` Shipping unreachable |
| POST | `/agents/product-consult` | Chat with the product-consultation agent | – (fallback text if the LLM is unavailable) |
| POST | `/agents/cancel-support` | Chat with the cancellation-support agent | – (fallback text if the LLM is unavailable) |

Example — create an order (the customer and product must exist; create them first in Customer / Inventory Service):
```bash
curl -X POST http://localhost:8081/orders   -H "Content-Type: application/json"   -d '{"customerId":"<customerId>","items":[{"productId":"<productId>","quantity":2}]}'
# -> 201 {"orderId":"...","status":"PENDING","totalAmount":159.98,"items":[{"productId":"...","quantity":2,"unitPrice":79.99}]}
```

Example — chat with the product agent (needs a real `GROQ_API_KEY` to get a real answer; otherwise returns the
fallback message):
```bash
curl -X POST http://localhost:8081/agents/product-consult   -H "Content-Type: application/json"   -d '{"message":"Do you have any wireless headphones under $100?"}'
```

## Code structure

```
com.ecommerce.order
├── presentation.controller   OrderController, AgentController, ApiExceptionHandler (@RestControllerAdvice)
├── presentation.dto          AgentChatRequest / AgentChatResponse
├── service                   OrderService (use cases: create, get, confirm, reject, cancel)
├── service.dto               CreateOrderRequest, OrderResponse, ...
├── service.agent             ProductConsultAgent, CancelSupportAgent (LangChain4j interfaces) + their @Tool classes
├── domain.model              Order (aggregate root), OrderItem, OrderStatus, Money (value object), OrderLineDraft
├── domain.event              OrderPlaced / OrderConfirmed / OrderCancelled / OrderRejected (domain events)
├── domain.service            CancellationPolicy (the single cancellation rule)
├── domain.repository         OrderRepository (port)
├── domain.port               ProductCatalog, CustomerDirectory, ShipmentGateway, OrderEventPublisher (ports)
├── domain.exception          OrderNotFound, OrderCancellationNotAllowed, InvalidOrder, ExternalServiceUnavailable
└── infrastructure
    ├── persistence           JpaOrderRepository + OrderRepositoryAdapter (Spring Data)
    ├── messaging             InventoryEventConsumer, KafkaOrderEventPublisher (Spring Cloud Stream) + .dto payloads
    ├── client                InventoryServiceClient, CustomerServiceClient, ShippingServiceClient (REST adapters)
    └── config                RestTemplateConfig (timeouts), DomainConfig, LangChain4jConfig
```

The cancellation rule exists **once**: `CancellationPolicy`, reached through `OrderService.cancelOrder()` — the REST
endpoint and the cancel-support agent's `OrderCancelTool` both call that method.

## Tests
`mvn -pl order-service test` — no Kafka or network needed.
- `OrderTest`, `MoneyAndPolicyTest` — the aggregate, value object and cancellation policy in isolation.
- `OrderServiceTest` — the use cases against in-memory fakes of every port.
- `OrderMessagingTest` — full Spring context with the real Spring Cloud Stream bindings on the in-memory test
  binder: verifies the exact JSON published on `order-events` and the reaction to `inventory-events`.

## Kafka

**Publishes to `order-events`:**

| Event | Triggered by | Consumed by |
|---|---|---|
| `OrderCreatedEvent` | `POST /orders` | Inventory Service (reserve stock) |
| `OrderConfirmedEvent` | `confirmOrder()`, after an `InventoryReservedEvent` arrives | Shipping Service (create shipment) |
| `OrderCancelledEvent` | `POST /orders/{id}/cancel` (or the cancel-support agent) | Inventory Service (release stock), Shipping Service (cancel shipment if not dispatched) |

**Consumes from `inventory-events`:**

| Event | Triggers |
|---|---|
| `InventoryReservedEvent` | `confirmOrder()` — order → `CONFIRMED`, publishes `OrderConfirmedEvent` |
| `InventoryRejectedEvent` | `rejectOrder()` — order → `REJECTED` with a reason, **no event published** (rejection is a dead end by design) |

Wire format: every message is a JSON envelope —
```json
{"eventType": "...", "eventId": "<uuid>", "timestamp": "<ISO-8601>", "payload": {...}}
```
built and parsed manually via Jackson's `ObjectMapper`: `KafkaOrderEventPublisher` translates the domain events into the envelope, serializes it to a JSON
string and sends it with Spring Cloud Stream's `StreamBridge` (binding `orderEvents-out-0`), and
`InventoryEventConsumer` is a `Consumer<String>` bean that parses the string (binding `inventoryEvents-in-0`).
Events are published only **after the database transaction commits**.
It does not use a Spring JSON serializer — this matches the pattern used by `inventory-service` and
`shipping-service`, for wire-format consistency across all three services. Topics and consumer groups live in
`application.yml`, not in code.

## Known limitations

- **The AI agents need a real `GROQ_API_KEY`** to do anything beyond return a fixed fallback message — without one,
  every `/agents/*` call fails Groq's auth check and gets caught by a fallback handler, returning HTTP 200 with an
  apology message rather than a 500. Order creation, Kafka events, and cancellation logic are independent of this.
- **Rejection is a dead end by design:** when Inventory rejects an order the status becomes `REJECTED` with a reason and
  nothing is published (the original task spec contradicted itself; the acceptance criteria were followed). The domain
  still raises an internal `OrderRejected` event, which the publisher deliberately drops.
- **No transactional outbox:** events are sent right after the commit; a crash in that instant would lose the event
  and leave the order `PENDING`.
- **A stock reply for an order that is no longer `PENDING` is ignored** (e.g. it was cancelled while waiting), which
  means stock reserved for such an order is only released by the `OrderCancelledEvent`, as designed.
- `SHIPPED` and `COMPLETED` exist as statuses but nothing sets them yet.
