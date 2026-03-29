# bff-example

A working Spring Boot application demonstrating [bff-recipe](https://tayyab23.github.io/bff) patterns. Use this as a reference for integrating the library into your own project.

## Run

```bash
./gradlew :bff-example:bootRun
```

Starts on port 8080. Pass `Authorization: Bearer demo-token` on all requests.

## Try it

```bash
# Single ingredient
curl -s -X POST http://localhost:8080/bff/payments \
  -H "Authorization: Bearer demo-token" \
  -H "Content-Type: application/json" \
  -d '{"ingredients": [{"id": "getAccount", "params": {"accountId": "acc-123"}}]}' | jq

# Parallel with dependency wiring
curl -s -X POST http://localhost:8080/bff/payments \
  -H "Authorization: Bearer demo-token" \
  -H "Content-Type: application/json" \
  -d '{"ingredients": [
    {"id": "getAccount", "params": {"accountId": "acc-123"}},
    {"id": "getInvoices", "map": {"query": {"billingGroupId": "getAccount::body::${billingGroupId}"}}},
    {"id": "getPaymentMethods", "map": {"query": {"customerId": "getAccount::body::${customerId}"}}}
  ]}' | jq

# Array operators — collect invoice IDs for batch balance lookup
curl -s -X POST http://localhost:8080/bff/payments \
  -H "Authorization: Bearer demo-token" \
  -H "Content-Type: application/json" \
  -d '{"ingredients": [
    {"id": "getAccount", "params": {"accountId": "acc-123"}},
    {"id": "getInvoices", "map": {"query": {"billingGroupId": "getAccount::body::${billingGroupId}"}}},
    {"id": "batchGetInvoiceBalance", "map": {"body": {"invoiceIds": "getInvoices::body::${items[*].id}"}}}
  ]}' | jq

# Schema discovery
curl -s http://localhost:8080/bff/payments/schema \
  -H "Authorization: Bearer demo-token" | jq
```

## Recipes

| Recipe | Ingredients |
|---|---|
| `payments` | getAccount, getInvoices, getPaymentMethods, submitPayment, sendNotification, batchGetInvoiceBalance |
| `dashboard` | getAccount, getUserProfile, getInvoices, sendNotification |

## What's demonstrated

- `@BffIngredient` annotation with multi-recipe membership
- Dependency wiring via `::` expressions
- Parallel execution of independent ingredients
- Array operators (`[*]`, `[?filter]`, `[:slice]`)
- Header forwarding (`Authorization`)
- Custom headers (`X-Idempotency-Key`)
- Schema and validate endpoints
- OpenAPI codegen for realistic models
- Spring Security integration

## Tests

```bash
./gradlew :bff-example:test
```

28 integration tests covering single ingredients, parallel execution, dependency chains, array operators, error handling, auth enforcement, and discovery endpoints.

## Documentation

Full docs at [tayyab23.github.io/bff](https://tayyab23.github.io/bff)
