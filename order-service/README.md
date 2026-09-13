# Order Service

Member 1's service for the ScentHub platform — order management, Kafka event
production/consumption, and two LangChain4j-powered conversational AI agents.

## What this service does

- Create and view orders (`Order` + `OrderItem` entities, backed by H2)
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
- Spring Kafka
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
| `spring.kafka.bootstrap-servers` | `localhost:9092` | matches `docker-compose.yml` |
| `shipping-service.base-url` | `http://localhost:8083` | cancellation's shipment-status check |
| `product-service.base-url` | `http://localhost:8082` | AI agent's product lookup tool |
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

| Method | Path | Purpose |
|---|---|---|
| POST | `/orders` | Create an order |
| GET | `/orders/{orderId}` | Get one order (404 if not found) |
| GET | `/orders?customerId={id}` | Get all orders for a customer |
| POST | `/orders/{orderId}/cancel` | Cancel an order (409 if not allowed) |
| POST | `/agents/product-consult` | Chat with the product-consultation agent |
| POST | `/agents/cancel-support` | Chat with the cancellation-support agent |

Example — create an order:
```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"a1b2c3d4-0000-0000-0000-000000000001","items":[{"productId":"b2c3d4e5-0000-0000-0000-000000000002","quantity":2}]}'
```

Example — chat with the product agent (needs a real `GROQ_API_KEY` to get a
real answer; otherwise returns the fallback message):
```bash
curl -X POST http://localhost:8081/agents/product-consult \
  -H "Content-Type: application/json" \
  -d '{"message":"Do you have any wireless headphones under $100?"}'
```

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
built and parsed manually via Jackson's `ObjectMapper` (`KafkaTemplate<String,String>`),
not Spring Kafka's `JsonSerializer` — this matches the pattern already used by
`inventory-service` and `shipping-service`, for wire-format consistency across
all three services.

## Known limitations

- **`unitPrice` is hardcoded** to `74.99` for every order item — there's no real
  price lookup from Inventory Service at order-creation time (only the AI agent's
  product-search tool actually queries real prices). This means `totalAmount`
  and the Kafka Streams revenue aggregate in `inventory-service` don't reflect
  real product prices.
- **`OrderRejectedPayload` is built but never published anywhere** — the task
  spec itself is inconsistent about this (one part says it should be published
  from `rejectOrder()`, the acceptance criteria for that same method says no
  event should be published on rejection). We followed the acceptance criteria,
  since that's the testable contract; the payload class exists but is dead code.
- **The AI agents need a real `GROQ_API_KEY`** to do anything beyond return a
  fixed fallback message — without one, every `/agents/*` call fails Groq's
  auth check and gets caught by a fallback handler, returning HTTP 200 with an
  apology message rather than a 500. Order creation, Kafka events, and
  cancellation logic are completely independent of this and work with no key at all.
- **No automated tests** — everything above was verified via live manual testing
  (real HTTP calls, real Kafka messages, real running `inventory-service`/
  `shipping-service`) during development, not via a JUnit suite.
