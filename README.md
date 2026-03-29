# bff-recipe

A Backend For Frontend aggregation library for Spring Boot.

## Project Structure

| Directory | Purpose |
|---|---|
| `bff-spring-lib/` | Spring Boot starter library — published to Maven Central as `io.github.tayyab23:bff-spring-lib` |
| `bff-example/` | Example Spring Boot app with integration tests demonstrating all BFF patterns |
| `bff-client/` | TypeScript client package — `@bff-recipe/client`. Use `import type` for zero-byte runtime overhead |
| `bff-docs/` | Single-page documentation site |

## Quick Start

Add the dependency:

```groovy
implementation 'io.github.tayyab23:bff-spring-lib:1.0.0'
```

Annotate any controller:

```java
@BffIngredient
@GetMapping("/api/accounts/{accountId}")
public ResponseEntity<Account> getAccount(@PathVariable String accountId) { ... }
```

Call `POST /bff`:

```json
{ "ingredients": [{ "id": "getAccount", "params": { "accountId": "acc-123" } }] }
```

No `application.yml` needed. Configuration is entirely optional.

## Running the Example App

```bash
./gradlew :bff-example:bootRun
```

```bash
curl -X POST http://localhost:8080/bff/payments \
  -H "Authorization: Bearer demo-token" \
  -H "Content-Type: application/json" \
  -d '{"ingredients": [
    {"id": "getAccount", "params": {"accountId": "acc-123"}},
    {"id": "getInvoices", "map": {"query": {"billingGroupId": "getAccount::body::${billingGroupId}"}}},
    {"id": "getPaymentMethods", "map": {"query": {"customerId": "getAccount::body::${customerId}"}}}
  ]}'
```

## Running Tests

```bash
./gradlew :bff-example:test
```

The integration tests cover:
- Single ingredient execution
- Parallel execution with dependency wiring
- Multi-recipe support (payments + dashboard)
- Schema and validate discovery endpoints
- Auth enforcement (401 without token)
- Custom header forwarding
- Unknown ingredient rejection (400)
- Max ingredients limit (400)
- Debug mode
- Partial failure (207 Multi-Status)

## Publishing the Library

### Prerequisites

- GPG key published to `keyserver.ubuntu.com`
- Sonatype Central token in `~/.gradle/gradle.properties`:
  ```properties
  sonatypeUsername=YOUR_TOKEN
  sonatypePassword=YOUR_PASSWORD
  signing.gnupg.executable=gpg
  signing.gnupg.keyName=YOUR_KEY_FINGERPRINT!
  ```

### Publish

```bash
./gradlew :bff-spring-lib:clean :bff-spring-lib:publishToCentral
```

## License

MIT
