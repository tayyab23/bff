# bff-spring-lib

The core Spring Boot starter for [bff-recipe](https://tayyab23.github.io/bff). Add it to your Spring Boot 3.x project and annotate existing controllers to create composite BFF endpoints.

## Install

### Gradle

```groovy
implementation 'io.github.tayyab23:bff-spring-lib:1.0.0'
```

### Maven

```xml
<dependency>
  <groupId>io.github.tayyab23</groupId>
  <artifactId>bff-spring-lib</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Usage

### 1. Annotate your controllers

```java
@BffIngredient(recipe = "payments", name = "getAccount")
@GetMapping("/api/accounts/{accountId}")
public ResponseEntity<Account> getAccount(@PathVariable String accountId) {
    // your existing logic — unchanged
}

@BffIngredient(recipe = "payments", name = "getInvoices")
@GetMapping("/api/invoices")
public ResponseEntity<InvoiceList> getInvoices(@RequestParam String billingGroupId) {
    // billingGroupId will be wired from getAccount's response
}
```

Your controllers keep working as standalone APIs. The annotation just registers them as ingredients in a recipe.

### 2. Call the recipe endpoint

```bash
curl -X POST http://localhost:8080/bff/payments \
  -H "Content-Type: application/json" \
  -d '{
    "ingredients": [
      { "id": "getAccount", "params": { "accountId": "acc-123" } },
      { "id": "getInvoices", "map": { "query": { "billingGroupId": "getAccount::body::${billingGroupId}" } } }
    ]
  }'
```

The library resolves dependencies, executes independent ingredients in parallel, and returns all results in one response.

## What you get

- `POST /bff/{recipe}` — execute a recipe
- `GET /bff/{recipe}/schema` — discover available ingredients (opt-in)
- `POST /bff/{recipe}/validate` — dry-run validation (opt-in)
- Automatic dependency resolution from `::` expressions
- Parallel execution of independent ingredients
- Full Spring MVC chain per ingredient (security, filters, interceptors)
- SecurityContext propagation to every ingredient

## Configuration (optional)

Zero config required. Everything below is opt-in:

```yaml
bff-recipe:
  enabled: true                          # default: true
  execution:
    ingredient-timeout-ms: "5000"
    recipe-timeout-ms: "15000"
    max-ingredients: 10
  schema:
    enabled: true                        # default: false
  validate:
    enabled: true                        # default: false
  debug:
    enabled: true                        # default: false
    mask-headers: ["Authorization"]
  headers:
    forward:
      blocked: ["Host", "Content-Length"]
    custom:
      enabled: true
      allowed: ["X-Idempotency-Key"]
```

## Array Operators

Collect, filter, and slice values from array responses to pass to batch endpoints:

```json
"invoiceIds": "getInvoices::body::${items[*].id}"
"overdueOnly": "getInvoices::body::${items[?status==OVERDUE].id}"
"firstFive": "getInvoices::body::${items[:5].id}"
"filtered": "getInvoices::body::${items[?status in (OVERDUE,UNPAID)&&amount>100].id}"
```

No fan-out — the result is always a single list passed to one batch call.

## Requirements

- Java 17+
- Spring Boot 3.x

## Documentation

Full docs at [tayyab23.github.io/bff](https://tayyab23.github.io/bff)

## License

MIT
