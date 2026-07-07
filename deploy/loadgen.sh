#!/bin/sh
# Weighted traffic generator. Drives the endpoints at deliberately different rates so Dynatrace
# shows a realistic production profile:
#   - POST /orders   : hot write path — high rate
#   - GET  /orders/id: hot read path
#   - GET  /inventory: moderate traffic
#   - POST /reports/nightly : cold path, ~once per many iterations
#
# The endpoint mix above is the production traffic profile Dynatrace observes for this service.

set -eu

BASE="${TARGET:-http://localhost:8080}"
LAST_ID=""

while true; do
  # Hot write burst.
  i=0
  while [ "$i" -lt 20 ]; do
    resp=$(curl -s -X POST "$BASE/orders" \
      -H 'Content-Type: application/json' \
      -d "{\"customer\":\"cust-$RANDOM\",\"amountCents\":$RANDOM}" || true)
    id=$(printf '%s' "$resp" | sed -n 's/.*"orderId":"\([^"]*\)".*/\1/p')
    [ -n "$id" ] && LAST_ID="$id"
    i=$((i + 1))
  done

  # Hot reads
  if [ -n "$LAST_ID" ]; then
    j=0
    while [ "$j" -lt 5 ]; do
      curl -s "$BASE/orders/$LAST_ID" >/dev/null || true
      j=$((j + 1))
    done
  fi

  # Clean moderate traffic
  curl -s "$BASE/inventory" >/dev/null || true

  # Cold path — roughly 1 in 500 iterations
  if [ "$((RANDOM % 500))" -eq 0 ]; then
    curl -s -X POST "$BASE/reports/nightly" >/dev/null || true
  fi

  sleep 1
done
