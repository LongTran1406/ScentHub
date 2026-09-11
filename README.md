# ScentHub — AI-powered E-commerce & Logistics Platform

## Project Overview
ScentHub is a modular AI-powered e-commerce and logistics platform built as multiple self-contained Spring Boot services. Services communicate via Kafka events where specified and expose REST APIs for CRUD and operations.

## Architecture
Services:
- Order Service (port 8081) — orders, emits order-events (not included in this member's tasks)
- Inventory Service (port 8082) — products and stock
- Shipping Service (port 8083) — consumes `order-events`, manages shipments
- Customer Service (port 8084) — manages customers, feedback, complaints

Kafka topics:
- `order-events` — published by Order Service, consumed by Shipping Service
- `inventory-events` — published/consumed by inventory-related services

(Events use an envelope: `{ "eventType": "OrderConfirmedEvent", "eventId": "<uuid>", "timestamp": "<ISO-8601>", "payload": { ... } }`)

## Tech Stack
- Java 21
- Spring Boot 3.2.x
- Spring Data JPA (H2 in-memory for local dev)
- Spring Kafka / spring-kafka
- Docker Compose (for local Kafka + Zookeeper)
- Maven

## Prerequisites
- Java 21 JDK
- Maven (for building services)
- Docker & Docker Compose (for Kafka)
- Optional: Postman to run the API collection

## How to run locally
1. Start Kafka + Zookeeper (from repo root):

```bash
docker-compose up -d
```

This starts Zookeeper and Kafka and creates the topics `order-events` and `inventory-events`.

2. Build and run services (each service is a separate Maven module):

```bash
# build all modules
mvn -DskipTests clean package

# run Shipping Service
cd shipping-service
mvn spring-boot:run
# runs on http://localhost:8083

# run Customer Service
cd ../customer-service
mvn spring-boot:run
# runs on http://localhost:8084
```

You can also run services from your IDE by running the `*Application` main classes in each module.

3. Access H2 Console (if needed):
- Shipping Service H2 console: http://localhost:8083/h2-console
  JDBC URL: `jdbc:h2:mem:shipping_db`
- Customer Service H2 console: http://localhost:8084/h2-console
  JDBC URL: `jdbc:h2:mem:customer_db`

## Postman collection
Import `postman/ecommerce-platform.postman_collection.json` and the environment `postman/local.postman_environment.json` into Postman. The environment provides base URLs for the services (8081–8084). The collection includes chained tests that save created IDs to environment variables.

## Kafka topics reference
- `order-events`: Publisher — Order Service; Consumers — Shipping Service; Event types: `OrderConfirmedEvent`, `OrderCancelledEvent` (envelope JSON format described above).
- `inventory-events`: Publisher/Consumers — Inventory-related services; event types depend on inventory implementation.

## Known limitations / Out of scope
- No authentication/authorization
- H2 in-memory databases are for local development only (data is not persisted across restarts)
- No real payment gateway integration
- Order Service and Inventory Service implementations may be partial (this member implemented Shipping and Customer services)

## Notes about building and testing in this environment
This repository contains the Shipping and Customer services fully implemented per Member 3 tasks. I created the Postman environment and README. The local environment used by this agent may not have Maven/Docker available; please run the `mvn` and `docker-compose` commands locally on your machine as shown above to start and test the system.
