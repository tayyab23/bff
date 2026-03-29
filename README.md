# bff-recipe

A zero-config Backend For Frontend aggregation library for Spring Boot.

Add one dependency, annotate your existing controllers with `@BffIngredient`, and get composite API endpoints that eliminate client-side round trips.

## Packages

| Package | Install | Description |
|---|---|---|
| [bff-spring-lib](./bff-spring-lib) | `implementation 'io.github.tayyab23:bff-spring-lib:1.0.0'` | Spring Boot starter — the core library |
| [@bff-recipe/types](./bff-types) | `npm install @bff-recipe/types --save-dev` | TypeScript type definitions for frontend consumers |
| [bff-example](./bff-example) | — | Working example app with integration tests |

## 30-Second Overview

```java
// 1. Annotate your existing controller — it keeps working as a standalone API
@BffIngredient(recipe = "payments")
@GetMapping("/api/accounts/{accountId}")
public ResponseEntity<Account> getAccount(@PathVariable String accountId) { ... }
```

```json
// 2. Client sends one request instead of N
POST /bff/payments
{
  "ingredients": [
    { "id": "getAccount", "params": { "accountId": "acc-123" } },
    { "id": "getInvoices", "map": { "query": { "billingGroupId": "getAccount::body::${billingGroupId}" } } }
  ]
}
```

```json
// 3. Get everything back in one response
{
  "executionOrder": ["getAccount", "getInvoices"],
  "results": {
    "getAccount": { "status": 200, "body": { "accountId": "acc-123", "billingGroupId": "bg-1" } },
    "getInvoices": { "status": 200, "body": { "items": [...], "total": 249.99 } }
  }
}
```

No `application.yml` required. Configuration is entirely optional.

## Documentation

Full docs at [tayyab23.github.io/bff](https://tayyab23.github.io/bff)

## License

MIT
