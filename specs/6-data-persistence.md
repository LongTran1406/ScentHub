# 6. Data persistence and ownership

Each service has **its own** in-memory H2 database (`create-drop` schema from the JPA mappings) and is the only
writer of its tables. No service reads another service's database; they exchange ids through REST or events.

## 6.1 Ownership

| Service | Database (JDBC URL) | Tables | Owns (aggregate) |
|---|---|---|---|
| order-service | `jdbc:h2:mem:order_db` | `orders`, `order_items` | `Order` (+ `OrderItem`) |
| inventory-service | `jdbc:h2:mem:inventory_db` | `products` | `Product` · plus Kafka Streams state stores `revenue-store`, `product-demand-store` (RocksDB, under `${java.io.tmpdir}/scenthub-streams`) |
| shipping-service | `jdbc:h2:mem:shipping_db` | `shipments` | `Shipment` |
| customer-service | `jdbc:h2:mem:customer_db` | `customers`, `feedback`, `complaints` | `Customer`, `Feedback`, `Complaint` |

## 6.2 Tables

```plantuml
@startuml
hide circle
entity orders { * id : uuid <<PK>>\n version : bigint\n customer_id : uuid\n status : varchar\n total_amount : numeric(38,2)\n rejection_reason : varchar\n created_at, updated_at }
entity order_items { * id : uuid <<PK>>\n order_id : uuid <<FK>>\n product_id : uuid\n quantity : int\n unit_price : numeric(38,2) }
entity products { * id : uuid <<PK>>\n version : bigint\n name : varchar\n description : varchar\n price : numeric(38,2)\n stock_quantity : int\n reserved_quantity : int\n created_at, updated_at }
entity shipments { * id : uuid <<PK>>\n version : bigint\n order_id : uuid <<UNIQUE>>\n status : varchar\n created_at, updated_at }
entity customers { * id : uuid <<PK>>\n version : bigint\n name : varchar\n email : varchar <<UNIQUE>>\n created_at }
entity feedback { * id : uuid <<PK>>\n customer_id : uuid\n content : varchar\n rating : int (nullable)\n created_at }
entity complaints { * id : uuid <<PK>>\n customer_id : uuid\n content : varchar\n status : varchar\n created_at }
orders ||--o{ order_items
@enduml
```

Cross-service references are **plain ids with no foreign key** (`orders.customer_id`, `order_items.product_id`,
`shipments.order_id`, `feedback.customer_id`, `complaints.customer_id`): the owning service is elsewhere, so
integrity is enforced by the application (Order validates the customer and products when an order is created).

## 6.3 Notable design points
- Money is stored as `numeric(38,2)`; `Money` is an embedded value object (column names preserved via `@AttributeOverride`).
- `available` stock is **not** a column; it is `stock_quantity - reserved_quantity` (low-stock is a JPQL query on that expression).
- Enumerations (`status`) are stored as readable strings.
- `version` columns implement optimistic locking on the aggregate roots.
- Customer email uniqueness is enforced by a database unique constraint on the *normalised* (lower-case) address.
- Data is lost on restart (development database). Moving to a durable database would change only the datasource
  configuration and add schema migration scripts.
