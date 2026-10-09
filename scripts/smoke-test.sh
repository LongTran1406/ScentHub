#!/usr/bin/env bash
# End-to-end smoke test for ScentHub. Needs: Kafka running (docker compose up -d) and all four services started
# (order 8081, inventory 8082, shipping 8083, customer 8084). Uses only curl + sed. Run with:  bash scripts/smoke-test.sh
# Each check prints PASS/FAIL; the exit code is the number of failures.

ORDER=${ORDER_URL:-http://localhost:8081}
INVENTORY=${INVENTORY_URL:-http://localhost:8082}
SHIPPING=${SHIPPING_URL:-http://localhost:8083}
CUSTOMER=${CUSTOMER_URL:-http://localhost:8084}
H='Content-Type: application/json'
FAILS=0

json() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }          # extract a string field
num()  { sed -n "s/.*\"$1\":\([0-9.]*\).*/\1/p"; }              # extract a number field
status_of() { curl -s "$1" | json status; }
check() {  # check "description" expected actual
  if [ "$2" = "$3" ]; then echo "PASS  $1"; else echo "FAIL  $1 (expected '$2', got '$3')"; FAILS=$((FAILS+1)); fi
}
http() { curl -s -o /dev/null -w "%{http_code}" "$@"; }
wait_status() {  # wait_status url expected  (polls up to ~15s for an asynchronous state change)
  for _ in $(seq 1 15); do [ "$(status_of "$1")" = "$2" ] && return 0; sleep 1; done; return 1
}

echo "== setup"
EMAIL="smoke$RANDOM$RANDOM@example.com"
CID=$(curl -s -X POST $CUSTOMER/customers -H "$H" -d "{\"name\":\"Smoke Test\",\"email\":\"$EMAIL\"}" | json customerId)
PID=$(curl -s -X POST $INVENTORY/products -H "$H" -d '{"name":"Smoke Eau","description":"d","price":79.99,"stockQuantity":5}' | json productId)
check "customer created" "yes" "$([ -n "$CID" ] && echo yes || echo no)"
check "product created" "yes" "$([ -n "$PID" ] && echo yes || echo no)"

echo "== customer service rules"
check "duplicate email (case-insensitive) -> 409" 409 "$(http -X POST $CUSTOMER/customers -H "$H" -d "{\"name\":\"X\",\"email\":\"$(echo $EMAIL | tr a-z A-Z)\"}")"
check "malformed email -> 400" 400 "$(http -X POST $CUSTOMER/customers -H "$H" -d '{"name":"X","email":"nope"}')"
check "rating out of range -> 400" 400 "$(http -X POST $CUSTOMER/customers/$CID/feedback -H "$H" -d '{"content":"x","rating":9}')"
check "feedback created -> 201" 201 "$(http -X POST $CUSTOMER/customers/$CID/feedback -H "$H" -d '{"content":"Lovely","rating":5}')"
check "complaint created -> 201" 201 "$(http -X POST $CUSTOMER/customers/$CID/complaints -H "$H" -d '{"content":"Late"}')"
check "complaint status is OPEN" OPEN "$(curl -s $CUSTOMER/customers/$CID/complaints | json status)"
check "unknown customer -> 404" 404 "$(http $CUSTOMER/customers/00000000-0000-0000-0000-000000000009)"

echo "== inventory service rules"
check "negative price -> 400" 400 "$(http -X POST $INVENTORY/products -H "$H" -d '{"name":"Bad","price":-1,"stockQuantity":1}')"
check "blank name -> 400" 400 "$(http -X POST $INVENTORY/products -H "$H" -d '{"name":" ","price":1,"stockQuantity":1}')"
check "unknown product -> 404" 404 "$(http $INVENTORY/products/00000000-0000-0000-0000-000000000009)"

echo "== order validation"
check "unknown customer -> 400" 400 "$(http -X POST $ORDER/orders -H "$H" -d "{\"customerId\":\"00000000-0000-0000-0000-000000000009\",\"items\":[{\"productId\":\"$PID\",\"quantity\":1}]}")"
check "unknown product -> 400" 400 "$(http -X POST $ORDER/orders -H "$H" -d "{\"customerId\":\"$CID\",\"items\":[{\"productId\":\"00000000-0000-0000-0000-000000000009\",\"quantity\":1}]}")"
check "empty items -> 400" 400 "$(http -X POST $ORDER/orders -H "$H" -d "{\"customerId\":\"$CID\",\"items\":[]}")"
check "quantity 0 -> 400" 400 "$(http -X POST $ORDER/orders -H "$H" -d "{\"customerId\":\"$CID\",\"items\":[{\"productId\":\"$PID\",\"quantity\":0}]}")"

echo "== happy path: order -> reserve -> confirm -> shipment"
R=$(curl -s -X POST $ORDER/orders -H "$H" -d "{\"customerId\":\"$CID\",\"items\":[{\"productId\":\"$PID\",\"quantity\":2}]}")
O1=$(echo "$R" | json orderId)
check "order starts PENDING" PENDING "$(echo "$R" | json status)"
check "total uses the real catalogue price (2 x 79.99)" 159.98 "$(echo "$R" | num totalAmount)"
wait_status $ORDER/orders/$O1 CONFIRMED; check "order becomes CONFIRMED" CONFIRMED "$(status_of $ORDER/orders/$O1)"
wait_status $SHIPPING/shipments/$O1 CREATED; check "shipment is CREATED" CREATED "$(status_of $SHIPPING/shipments/$O1)"
check "2 units reserved" 2 "$(curl -s $INVENTORY/products/$PID | num reservedQuantity)"

echo "== rejection when stock is insufficient"
O2=$(curl -s -X POST $ORDER/orders -H "$H" -d "{\"customerId\":\"$CID\",\"items\":[{\"productId\":\"$PID\",\"quantity\":50}]}" | json orderId)
wait_status $ORDER/orders/$O2 REJECTED; check "oversized order becomes REJECTED" REJECTED "$(status_of $ORDER/orders/$O2)"
check "stock unchanged by the rejected order" 2 "$(curl -s $INVENTORY/products/$PID | num reservedQuantity)"
check "cancelling a REJECTED order -> 409" 409 "$(http -X POST $ORDER/orders/$O2/cancel)"

echo "== cancellation rules"
check "cancel CONFIRMED order -> 200" 200 "$(http -X POST $ORDER/orders/$O1/cancel)"
wait_status $SHIPPING/shipments/$O1 CANCELLED; check "shipment becomes CANCELLED" CANCELLED "$(status_of $SHIPPING/shipments/$O1)"
for _ in $(seq 1 15); do [ "$(curl -s $INVENTORY/products/$PID | num reservedQuantity)" = "0" ] && break; sleep 1; done
check "reserved stock released" 0 "$(curl -s $INVENTORY/products/$PID | num reservedQuantity)"
check "cancel again -> 409" 409 "$(http -X POST $ORDER/orders/$O1/cancel)"
O3=$(curl -s -X POST $ORDER/orders -H "$H" -d "{\"customerId\":\"$CID\",\"items\":[{\"productId\":\"$PID\",\"quantity\":1}]}" | json orderId)
wait_status $SHIPPING/shipments/$O3 CREATED
check "invalid shipment status -> 400" 400 "$(http -X PATCH $SHIPPING/shipments/$O3/status -H "$H" -d '{"status":"FLYING"}')"
check "dispatch shipment -> 200" 200 "$(http -X PATCH $SHIPPING/shipments/$O3/status -H "$H" -d '{"status":"DISPATCHED"}')"
check "cancel after DISPATCHED -> 409" 409 "$(http -X POST $ORDER/orders/$O3/cancel)"
check "unknown order -> 404" 404 "$(http $ORDER/orders/00000000-0000-0000-0000-000000000009)"
check "no shipment for unknown order -> 404" 404 "$(http $SHIPPING/shipments/00000000-0000-0000-0000-000000000009)"

echo "== real-time analytics (Kafka Streams)"
sleep 3
REV=$(curl -s $INVENTORY/analytics/revenue | num totalRevenue)
check "revenue endpoint returns a positive total" yes "$(awk -v r="${REV:-0}" 'BEGIN{print (r>0)?"yes":"no"}')"
check "trending products lists the ordered product" "Smoke Eau" "$(curl -s "$INVENTORY/analytics/trending-products?minutes=10&limit=50" | sed -n "s/.*\"productId\":\"$PID\",\"name\":\"\([^\"]*\)\".*/\1/p")"
check "low-stock endpoint responds" 200 "$(http "$INVENTORY/analytics/low-stock?threshold=10")"

echo
if [ "$FAILS" -eq 0 ]; then echo "ALL CHECKS PASSED"; else echo "$FAILS CHECK(S) FAILED"; fi
exit $FAILS
