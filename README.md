# ScentHub — AI-powered E-commerce & Logistics Platform

## Project Overview
ScentHub is an AI-powered, event-driven e-commerce and logistics platform built as four independent Spring Boot
microservices. Customers place orders; stock is reserved asynchronously; confirmed orders get a shipment; and two
LangChain4j agents let customers ask about products and cancel orders in natural language. Inventory also runs a
real-time Kafka Streams analysis of order traffic.

## Architecture

| Service | Port | Database | Responsibility |
|---|---|---|---|
| order-service | 8081 | `order_db` | Orders and their lifecycle, cancellation rules, **2 AI agents** |
| inventory-service | 8082 | `inventory_db` | Products, stock reservation/release, **stream analytics** |
| shipping-service | 8083 | `shipping_db` | Shipments and their status |
| customer-service | 8084 | `customer_db` | Customer profiles, feedback, complaints (no Kafka) |

```
                      order-events                        inventory-events
 Order Service  ───────────────────────►  Inventory Service  ───────────────────►  Order Service
 (8081)            OrderCreated/Cancelled   (8082)            InventoryReserved/Rejected  (confirm / reject)
    │                       │                  │
    │  OrderConfirmed/      ▼                  └─ Kafka Streams: revenue + trending products
    │  OrderCancelled  Shipping Service (8083)
    │                  (create / cancel shipment)
    ├── REST GET /shipments/{orderId} ──► Shipping   (cancellation eligibility check)
    └── LangChain4j agents ──► Inventory REST /products (lookup)  and  OrderService.cancelOrder()
```

### Layered architecture (every service)
Each service has the same four layers, matched one-to-one by its package structure (`com.ecommerce.<service>.…`):

| Layer | Package | Responsibility |
|---|---|---|
| Presentation | `presentation.controller` | REST controllers and one `@RestControllerAdvice` that maps exceptions to HTTP status codes |
| Service (application) | `service`, `service.dto` | One class per group of use cases; loads aggregates, calls domain behaviour, saves, publishes. Contains no business rules |
| Domain | `domain.model`, `.event`, `.service`, `.repository`, `.port`, `.exception` | Aggregates, value objects, domain events, domain services and the *interfaces* (ports) the domain needs |
| Infrastructure | `infrastructure.persistence`, `.messaging`, `.client`, `.streams`, `.config` | Everything technical: Spring Data adapters, Kafka/Spring Cloud Stream, REST clients, Kafka Streams, bean wiring |

Dependencies point inwards (`presentation → service → domain ← infrastructure`): the domain declares ports such as
`OrderRepository`, `ProductCatalog` or `OrderEventPublisher`, and the infrastructure layer implements them.

### Domain-driven design in the code
| Service | Aggregate roots (entities inside) | Value objects | Domain events | Domain services |
|---|---|---|---|---|
| order | `Order` (`OrderItem`) | `Money`, `OrderLineDraft`, `EventLine` | `OrderPlaced`, `OrderConfirmed`, `OrderCancelled`, `OrderRejected` | `CancellationPolicy` |
| inventory | `Product` | `Money`, `StockLine`, `Shortage` | `StockReserved`, `StockRejected` | `StockReservationService` (all-or-nothing across products) |
| shipping | `Shipment` | `ShipmentStatus` | – (consumes events only) | – |
| customer | `Customer`, `Feedback`, `Complaint` | `Email`, `ComplaintStatus` | – (no messaging) | – |

Aggregates enforce their own invariants (e.g. an order can only be cancelled while `PENDING`/`CONFIRMED`; a product's
reserved stock can never exceed its stock) and use `@Version` for optimistic locking. Domain events are raised by the
aggregate and published **after the database transaction commits**, translated by the infrastructure layer into the
JSON messages below (`KafkaOrderEventPublisher`, `KafkaInventoryEventPublisher`).

### Messaging — Spring Cloud Stream (Kafka binder)
All services use **Spring Cloud Stream** (functional style). Topics and consumer groups are configured in each
`application.yml`; the code only contains plain `Consumer<String>` beans and `StreamBridge` publishers.
Messages are JSON strings in a common envelope:
`{ "eventType": "...", "eventId": "<uuid>", "timestamp": "<ISO-8601>", "payload": { ... } }`

| Service | Binding | Topic | Group | Direction |
|---|---|---|---|---|
| order | `inventoryEvents-in-0` | `inventory-events` | `order-service` | consume |
| order | `orderEvents-out-0` | `order-events` | – | publish (via `StreamBridge`) |
| inventory | `orderEvents-in-0` | `order-events` | `inventory-service` | consume |
| inventory | `inventoryEvents-out-0` | `inventory-events` | – | publish (via `StreamBridge`) |
| inventory | `revenue-in-0` | `order-events` | – | Kafka Streams (revenue) |
| inventory | `demand-in-0` | `order-events` | – | Kafka Streams (trending products) |
| shipping | `orderEvents-in-0` | `order-events` | `shipping-service` | consume |

Inventory uses **two binders** at once (`kafka` and `kstream`). Both are declared in `spring.cloud.stream.binders`
with `inheritEnvironment: false`, so any Kafka setting for a plain-Kafka binding (for example `startOffset`, or the
producer serializer) must be placed inside that binder's own `environment:` block, not at the top level.

### Stream processing (inventory-service, Kafka Streams binder)
1. **Revenue** (`revenue` topology → state store `revenue-store`): running sum of `totalAmount` over every
   `OrderCreatedEvent`. Exposed at `GET /analytics/revenue`.
2. **Trending products** (`demand` topology → windowed store `product-demand-store`): units ordered per product in
   5-minute tumbling windows. Exposed at `GET /analytics/trending-products?minutes=10&limit=5`.

Both are read through Spring Cloud Stream's `InteractiveQueryService`. Equivalent streaming queries:

```sql
-- revenue
SELECT SUM(totalAmount) FROM order_events WHERE eventType = 'OrderCreatedEvent';
-- trending products
SELECT productId, SUM(quantity) FROM order_created_items
WINDOW TUMBLING (SIZE 5 MINUTES) GROUP BY productId;
```

Each Kafka Streams function is its own `KafkaStreams` instance, so each needs a **distinct `applicationId` and
`state.dir`**. They are set per function under `spring.cloud.stream.kafka.streams.binder.functions.<name>` — in the
main `streams` section (read by the function processor) and mirrored in the `kstream` binder's `environment`.

## Tech Stack
- Java 21, Spring Boot 3.2.5, Spring Cloud 2023.0.1 (Spring Cloud Stream 4.1.x)
- Spring Web, Spring Data JPA, H2 (in-memory, local dev)
- Apache Kafka (Confluent images via Docker Compose), Kafka Streams (through the Spring Cloud Stream binder)
- LangChain4j 1.3.0 (`langchain4j-open-ai`, pointed at Groq's OpenAI-compatible API)
- Maven multi-module build

## Prerequisites
- **JDK 21** (required by the course). The build uses `maven.compiler.release=21`, so it fails if code uses an API newer
  than 21, even on a newer JDK. If several JDKs are installed, point the current PowerShell window at 21 with
  `. .\scripts\use-jdk21.ps1` (expects `%USERPROFILE%\tools\jdk-21`, or set `JDK21_HOME`), check that `mvn -version`
  says `Java version: 21…`, and start the services from that same window.
- Maven 3.9+
- Docker + Docker Compose (Kafka + Zookeeper)
- Optional: Postman; a free Groq API key for the AI agents

## How to run locally
1. Start Kafka + Zookeeper (from this folder). This also creates `order-events` and `inventory-events`:
   ```bash
   docker compose up -d
   ```
   If Kafka exits with `NodeExists` after an unclean shutdown, run `docker compose down` and `up -d` again.
2. (Optional, for real AI replies) set the Groq key before starting order-service:
   ```powershell
   $env:GROQ_API_KEY = "your-key"      # PowerShell
   ```
   ```bash
   export GROQ_API_KEY="your-key"      # bash
   ```
   Without it the `/agents/*` endpoints return a fixed fallback message; everything else works.
3. Build, then start each service in its own terminal (any order — Kafka decouples them):
   ```bash
   mvn -DskipTests clean package
   mvn -pl shipping-service  spring-boot:run     # 8083
   mvn -pl inventory-service spring-boot:run     # 8082
   mvn -pl order-service     spring-boot:run     # 8081
   mvn -pl customer-service  spring-boot:run     # 8084
   ```
4. H2 consoles (`user: sa`, blank password): `http://localhost:{8081..8084}/h2-console`, JDBC URLs
   `jdbc:h2:mem:order_db`, `inventory_db`, `shipping_db`, `customer_db`.

> Always stop old service processes before restarting one: a leftover instance keeps its Kafka partition in the
> consumer group and silently steals the messages.

## Tests
```bash
mvn test                       # 106 unit/integration tests, no Kafka or Docker needed
bash scripts/smoke-test.sh     # ~37 end-to-end checks against the four RUNNING services (Kafka + services up)
```
- *Domain tests* check the aggregates, value objects and domain services in isolation.
- *Service tests* run the application services against in-memory fakes of every port.
- *Messaging tests* load the whole Spring context with the real Spring Cloud Stream bindings, replacing Kafka by the
  in-memory test binder (`spring-cloud-stream-test-binder`): they send the agreed JSON messages in and assert on the
  JSON messages coming out (order, inventory and shipping services).
- The *smoke test* is a plain `curl` script (a lightweight client) that exercises every business rule across services,
  including the Kafka Streams analytics. Set `ORDER_URL`, `INVENTORY_URL`, … to point it elsewhere.

## Sample inputs (use cases)
Import `postman/ecommerce-platform.postman_collection.json` and `postman/local.postman_environment.json` for the
full chained collection. Core requests:

```bash
# Create a product                                                    -> Inventory
curl -X POST localhost:8082/products -H "Content-Type: application/json" \
  -d '{"name":"Rose Eau","description":"Floral","price":79.99,"stockQuantity":50}'

# Create a customer                                                   -> Customer
curl -X POST localhost:8084/customers -H "Content-Type: application/json" \
  -d '{"name":"Jane Doe","email":"jane@example.com"}'

# Place an order (stock is reserved, then the order becomes CONFIRMED) -> Order
curl -X POST localhost:8081/orders -H "Content-Type: application/json" \
  -d '{"customerId":"<customerId>","items":[{"productId":"<productId>","quantity":2}]}'

# Check order / shipment
curl localhost:8081/orders/<orderId>
curl localhost:8083/shipments/<orderId>

# Move the shipment along (cancel is refused once DISPATCHED)
curl -X PATCH localhost:8083/shipments/<orderId>/status -H "Content-Type: application/json" -d '{"status":"DISPATCHED"}'

# Cancel an order (409 if already shipped/dispatched)
curl -X POST localhost:8081/orders/<orderId>/cancel

# AI agents
curl -X POST localhost:8081/agents/product-consult -H "Content-Type: application/json" \
  -d '{"message":"Do you have any rose perfume under $100?"}'
curl -X POST localhost:8081/agents/cancel-support -H "Content-Type: application/json" \
  -d '{"message":"Please cancel order <orderId>"}'

# Real-time analytics (Kafka Streams)
curl localhost:8082/analytics/revenue
curl "localhost:8082/analytics/trending-products?minutes=10&limit=5"
curl "localhost:8082/analytics/low-stock?threshold=10"

# Feedback and complaints
curl -X POST localhost:8084/customers/<customerId>/feedback   -H "Content-Type: application/json" -d '{"content":"Great scent","rating":5}'
curl -X POST localhost:8084/customers/<customerId>/complaints -H "Content-Type: application/json" -d '{"content":"Late delivery"}'
curl localhost:8084/customers/<customerId>/complaints
```

## Kafka topics reference
| Topic | Publisher | Consumers | Event types |
|---|---|---|---|
| `order-events` | order-service | inventory-service (reserve/release), shipping-service (create/cancel shipment), inventory Kafka Streams (analytics) | `OrderCreatedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent` |
| `inventory-events` | inventory-service | order-service (confirm/reject) | `InventoryReservedEvent`, `InventoryRejectedEvent` |

## Project documentation
- `member1_tasks_order_service_ai.txt`, `member2_tasks_inventory_streams.txt`, `member3_tasks_shipping_customer_docs.txt`
  — the original AI-assisted task specifications for each service. Each starts with a dated **REVISION NOTES** block
  listing where the implementation deliberately deviates from the original plan.
- `specs/` — the specification set, written to match the code: `1-architecture`, `2-requirements-and-user-stories`
  (functional requirements, user stories, use cases, UML activity diagrams), `3-domain-model` (bounded contexts, DDD
  patterns, class diagram), `4-api-endpoints`, `5-event-contracts-and-streams`, `6-data-persistence`, `7-agentic-ai`,
  `8-sample-inputs-outputs` (real captured requests/responses), `9-ai-driven-development-notes` (evidence for the report).
- `scripts/smoke-test.sh` — end-to-end smoke test (a lightweight client for the running system).
- `order-service/README.md` — details for the order service and AI agents.
- `CSCI318 Project Specification Spring 2026.pdf` — the course specification this project is built against.

## Business rules and error responses
| Situation | Response |
|---|---|
| Order for an unknown customer or product, empty order, quantity ≤ 0 | `400` with a message |
| Inventory, Customer or Shipping Service unreachable while creating/cancelling an order | `503` |
| Order total | Sum of `quantity × product price`, with the price read from Inventory at order time |
| Not enough stock for any line of an order | Order becomes `REJECTED`; nothing is reserved for any line |
| Cancelling an order that is `CANCELLED`, `REJECTED`, `SHIPPED`/`COMPLETED`, or whose shipment is `DISPATCHED`/`IN_TRANSIT`/`DELIVERED` | `409` |
| Stock reply arrives for an order that is no longer `PENDING` (e.g. cancelled meanwhile) | Ignored |
| Duplicate customer email (case-insensitive) | `409`; malformed email, rating outside 1–5 → `400` |
| Invalid product data (blank name, negative price/stock, stock below reserved) | `400` |
| Unknown shipment status | `400`; no shipment yet → `404` (Order Service reads this as "not shipped") |

## Known limitations / out of scope
- No authentication/authorization; no real payment gateway; no returns flow.
- H2 is in-memory — data is lost on restart (Kafka topics also lose data when the containers are recreated).
- Revenue counts every `OrderCreatedEvent`, including orders that inventory later rejects (kept deliberately simple).
- Events are published after commit but without a transactional outbox, so a crash between commit and publish can
  lose an event; order status can then stay `PENDING`.
- Stock is reserved when the order is *placed*, not when payment succeeds (there is no payment step).
- The AI agents need a real `GROQ_API_KEY`; without one they return a fixed fallback message.
- The Kafka Streams topologies are verified by the smoke test, not by unit tests (they need a broker).
