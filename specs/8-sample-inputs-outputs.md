# 8. Sample inputs and outputs (one per user story)

> Source material for report Section 4.2. **These are real responses captured from the running system on
> 2026-10-08** (all four services + Kafka, fresh databases). IDs differ on every run. Asynchronous steps (stock
> reservation, shipment creation, cancellation side effects) complete within about a second; the captures wait
> 5-6 seconds before reading the result. A runnable version of the happy path and the error cases is
> `scripts/smoke-test.sh`; the Postman collection holds the same requests with chained variables.

Setup used for the captures: customer `6baa840d-…` (Jane Doe); products *Rose Eau de Parfum* (price 79.99, stock 20) and
*Oud Noir* (price 120.00, stock 3).

## US-01 Register a customer
`POST :8084/customers`
```json
{"name":"Jane Doe","email":"jane.doe@example.com"}
```
→ `201`
```json
{"customerId":"6baa840d-2b1c-442c-b421-7875fcbeca7e","name":"Jane Doe","email":"jane.doe@example.com"}
```

## US-02 Add a product
`POST :8082/products`
```json
{"name":"Rose Eau de Parfum","description":"Floral, 50ml","price":79.99,"stockQuantity":20}
```
→ `201`
```json
{"productId":"3c7751a5-d345-48be-9d7b-776ac9a68d02","name":"Rose Eau de Parfum","description":"Floral, 50ml","price":79.99,"stockQuantity":20,"reservedQuantity":0,"availableQuantity":20}
```

## US-03 Browse / search
`GET :8082/products?search=rose` → `200`
```json
[{"productId":"3c7751a5-d345-48be-9d7b-776ac9a68d02","name":"Rose Eau de Parfum","description":"Floral, 50ml","price":79.99,"stockQuantity":20,"reservedQuantity":0,"availableQuantity":20}]
```

## US-04 Place an order (price comes from the catalogue)
`POST :8081/orders` - 2 × Rose (79.99) + 1 × Oud (120.00)
```json
{"customerId":"6baa840d-2b1c-442c-b421-7875fcbeca7e","items":[{"productId":"3c7751a5-d345-48be-9d7b-776ac9a68d02","quantity":2},{"productId":"6eaf7d68-c35a-4958-bad7-2db4a12ea944","quantity":1}]}
```
→ `201` (total 2 × 79.99 + 120.00 = 279.98)
```json
{"orderId":"3932f87e-49f4-4335-b1ee-a25e49a0b19b","status":"PENDING","totalAmount":279.98,"items":[{"productId":"3c7751a5-d345-48be-9d7b-776ac9a68d02","quantity":2,"unitPrice":79.99},{"productId":"6eaf7d68-c35a-4958-bad7-2db4a12ea944","quantity":1,"unitPrice":120.00}]}
```

**Rejected for lack of stock** - 10 × Oud while only 2 are still available:
→ `201 PENDING`, then `GET :8081/orders/0fda6c62-…`
```json
{"orderId":"0fda6c62-580f-47e8-bdbf-5bb3a7e1a93e","status":"REJECTED","totalAmount":1200.00,"items":[{"productId":"6eaf7d68-c35a-4958-bad7-2db4a12ea944","quantity":10,"unitPrice":120.00}]}
```

**Invalid** - unknown product id → `400`
```text
Product not found: 00000000-0000-0000-0000-000000000009
```

## US-05 View the order and the order history
`GET :8081/orders/3932f87e-…` → `200` (a moment later the status is `CONFIRMED`)
```json
{"orderId":"3932f87e-49f4-4335-b1ee-a25e49a0b19b","status":"CONFIRMED","totalAmount":279.98,"items":[{"productId":"3c7751a5-d345-48be-9d7b-776ac9a68d02","quantity":2,"unitPrice":79.99},{"productId":"6eaf7d68-c35a-4958-bad7-2db4a12ea944","quantity":1,"unitPrice":120.00}]}
```
`GET :8081/orders?customerId=6baa840d-…` → `200` a list containing that order.

Stock after the reservation, `GET :8082/products/6eaf7d68-…` (Oud: stock 3, one reserved):
```json
{"productId":"6eaf7d68-c35a-4958-bad7-2db4a12ea944","name":"Oud Noir","description":"Woody, 30ml","price":120.00,"stockQuantity":3,"reservedQuantity":1,"availableQuantity":2}
```

## US-06 Shipment created automatically
`GET :8083/shipments/3932f87e-…` → `200`
```json
{"orderId":"3932f87e-49f4-4335-b1ee-a25e49a0b19b","status":"CREATED"}
```

## US-07 Update the shipment status
`PATCH :8083/shipments/3932f87e-…/status`
```json
{"status":"DISPATCHED"}
```
→ `200`
```json
{"orderId":"3932f87e-49f4-4335-b1ee-a25e49a0b19b","status":"DISPATCHED"}
```

## US-08 Cancel an order
**Refused** - the shipment is already dispatched: `POST :8081/orders/3932f87e-…/cancel` → `409`
```text
Cannot cancel order 3932f87e-49f4-4335-b1ee-a25e49a0b19b: shipment already DISPATCHED
```
**Allowed** - a confirmed order whose shipment is still `CREATED`: `POST :8081/orders/f6d2a709-…/cancel` → `200`
```json
{"orderId":"f6d2a709-9683-4621-80eb-54e2df388131","status":"CANCELLED","totalAmount":79.99,"items":[{"productId":"3c7751a5-d345-48be-9d7b-776ac9a68d02","quantity":1,"unitPrice":79.99}]}
```
and the shipment follows (`GET :8083/shipments/f6d2a709-…`): `{"orderId":"f6d2a709-…","status":"CANCELLED"}`; the reserved stock is released.

## US-09 Feedback
`POST :8084/customers/6baa840d-…/feedback`
```json
{"content":"Beautiful scent, fast delivery","rating":5}
```
→ `201`
```json
{"feedbackId":"25030edf-6432-4f4c-92db-5a2dff25c429","content":"Beautiful scent, fast delivery","rating":5,"createdAt":"2026-10-08T12:51:26.959394100Z"}
```

## US-10 Complaint
`POST :8084/customers/6baa840d-…/complaints`
```json
{"content":"Box arrived damaged"}
```
→ `201`
```json
{"complaintId":"f245c2d4-3647-48f7-92e2-349e31272143","content":"Box arrived damaged","status":"OPEN","createdAt":"2026-10-08T12:51:27.015821400Z"}
```
`GET :8084/customers/6baa840d-…/complaints` → `200` a list with that complaint.

## US-11 Low-stock report
`GET :8082/analytics/low-stock?threshold=10` → `200`
```json
[{"productId":"6eaf7d68-c35a-4958-bad7-2db4a12ea944","name":"Oud Noir","availableQuantity":2,"threshold":10}]
```

## US-12 Live revenue (Kafka Streams)
`GET :8082/analytics/revenue` → `200` (sum of the totals of every order placed in this run, rejected ones included)
```json
{"totalRevenue":1559.97}
```

## US-13 Trending products (Kafka Streams)
`GET :8082/analytics/trending-products?minutes=10&limit=5` → `200` (units ordered, including the rejected 10 × Oud)
```json
[{"productId":"6eaf7d68-c35a-4958-bad7-2db4a12ea944","name":"Oud Noir","unitsOrdered":11},{"productId":"3c7751a5-d345-48be-9d7b-776ac9a68d02","name":"Rose Eau de Parfum","unitsOrdered":3}]
```

## US-14 / US-15 AI agents - **real LLM output still to be captured**
`POST :8081/agents/product-consult` `{"message":"Do you have any rose perfume?"}` and
`POST :8081/agents/cancel-support` `{"message":"Please cancel order 3932f87e-…"}`.

With no `GROQ_API_KEY` configured (the situation in which the capture above was taken) both return `200`:
```json
{"reply":"Sorry, the assistant is temporarily unavailable. Please try again later."}
```
**TODO for the team:** set `GROQ_API_KEY`, repeat these two calls, and paste the genuine replies here. The tools the
agents call are covered by `AgentToolsTest`, e.g. the cancellation tool returns
`Order <id> has been successfully cancelled.` or `Cannot cancel order <id>: shipment already DISPATCHED`, and the
product tool returns lines such as `Rose Eau de Parfum - $79.99 - 18 in stock`.
