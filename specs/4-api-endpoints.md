# 4. REST API reference

Base URLs: Order `:8081`, Inventory `:8082`, Shipping `:8083`, Customer `:8084`. All bodies are JSON; error bodies are
plain text messages. IDs are UUID strings. Machine-readable examples: `postman/ecommerce-platform.postman_collection.json`.

## Order Service (8081)
| Method | Path | Body / params | Success | Errors |
|---|---|---|---|---|
| POST | `/orders` | `{customerId, items:[{productId, quantity}]}` | `201` `{orderId, status:"PENDING", totalAmount, items:[{productId, quantity, unitPrice}]}` | `400` unknown customer/product, no items, quantity ≤ 0, missing customerId · `503` Inventory/Customer unreachable |
| GET | `/orders/{orderId}` | | `200` order | `404` |
| GET | `/orders?customerId={id}` | | `200` list (may be empty) | |
| POST | `/orders/{orderId}/cancel` | | `200` order with `status:"CANCELLED"` | `404` · `409` not cancellable (already cancelled/rejected/shipped, or shipment dispatched) · `503` Shipping unreachable |
| POST | `/agents/product-consult` | `{message}` | `200` `{reply}` | fallback reply text if the LLM is unavailable (still `200`) |
| POST | `/agents/cancel-support` | `{message}` | `200` `{reply}` | same |

Order `status` values: `PENDING`, `CONFIRMED`, `REJECTED`, `CANCELLED` (`SHIPPED`, `COMPLETED` reserved, not set yet).
The unit price is never sent by the client; it is read from Inventory.

## Inventory Service (8082)
| Method | Path | Body / params | Success | Errors |
|---|---|---|---|---|
| POST | `/products` | `{name, description, price, stockQuantity}` | `201` product | `400` blank name, negative/missing price, negative stock |
| PUT | `/products/{id}` | any subset of the create fields | `200` product | `404` · `400` (incl. stock below reserved) |
| DELETE | `/products/{id}` | | `204` | `404` |
| GET | `/products` | optional `?search=` (name contains, case-insensitive) | `200` list | |
| GET | `/products/{id}` | | `200` `{productId, name, description, price, stockQuantity, reservedQuantity, availableQuantity}` | `404` |
| GET | `/analytics/low-stock` | `?threshold=10` | `200` `[{productId, name, availableQuantity, threshold}]` | |
| GET | `/analytics/revenue` | | `200` `{totalRevenue}` (Kafka Streams) | |
| GET | `/analytics/trending-products` | `?minutes=10&limit=5` | `200` `[{productId, name, unitsOrdered}]` highest first (Kafka Streams) | |

`availableQuantity` is always `stockQuantity - reservedQuantity` (derived, never stored).

## Shipping Service (8083)
| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| GET | `/shipments/{orderId}` | | `200` `{orderId, status}` | `404` no shipment yet (Order Service reads this as "not shipped") |
| PATCH | `/shipments/{orderId}/status` | `{status}` one of `CREATED, DISPATCHED, IN_TRANSIT, DELIVERED, CANCELLED` | `200` `{orderId, status}` | `404` · `400` unknown status |

Shipments are created by events, not by an endpoint.

## Customer Service (8084)
| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/customers` | `{name, email}` | `201` `{customerId, name, email}` | `400` blank name / malformed email · `409` duplicate email |
| GET | `/customers/{id}` | | `200` | `404` |
| PUT | `/customers/{id}` | `{name?, email?}` | `200` | `404` · `400` · `409` |
| POST | `/customers/{id}/feedback` | `{content, rating?}` (rating 1-5) | `201` `{feedbackId, content, rating, createdAt}` | `404` · `400` |
| POST | `/customers/{id}/complaints` | `{content}` | `201` `{complaintId, content, status:"OPEN", createdAt}` | `404` · `400` |
| GET | `/customers/{id}/complaints` | | `200` list (empty for a customer with none, and also for an unknown id: this endpoint does not check the customer exists) | |

H2 consoles (development only): `http://localhost:{8081..8084}/h2-console`, user `sa`, empty password.
