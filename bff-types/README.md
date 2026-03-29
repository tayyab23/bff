# @bff-recipe/types

TypeScript type definitions for [bff-recipe](https://tayyab23.github.io/bff) — a Backend For Frontend aggregation library for Spring Boot.

This package contains only type definitions. It adds **zero bytes** to your runtime bundle when used with `import type`.

## Install

```bash
npm install @bff-recipe/types --save-dev
```

## Usage

### Define your API response types

These come from your backend — OpenAPI codegen, Smithy, or hand-written:

```typescript
interface Account {
  accountId: string;
  billingGroupId: string;
  plan: 'FREE' | 'PRO' | 'ENTERPRISE';
}

interface InvoiceList {
  items: { id: string; amount: number; status: string }[];
  total: number;
}
```

### Type your BFF request and response

```typescript
import type { RecipeRequest, RecipeResponse } from '@bff-recipe/types';
import axios from 'axios';

// Define what your recipe returns
type PaymentsPage = {
  getAccount: Account;
  getInvoices: InvoiceList;
};

// Build the request — RecipeRequest gives you autocomplete on ingredients
const request: RecipeRequest = {
  ingredients: [
    { id: 'getAccount', params: { accountId: 'acc-123' } },
    {
      id: 'getInvoices',
      map: { query: { billingGroupId: 'getAccount::body::${billingGroupId}' } },
    },
  ],
};

// Type the response — each ingredient's body is fully typed
const { data } = await axios.post<RecipeResponse<PaymentsPage>>(
  '/bff/payments',
  request
);

data.results.getAccount.body.plan;           // ✓ 'FREE' | 'PRO' | 'ENTERPRISE'
data.results.getInvoices.body.items[0].amount; // ✓ number
data.results.getAccount.status;              // ✓ number (HTTP status)
data.results.nonExistent;                    // ✗ compile error
```

### Works with any HTTP client

The types are framework-agnostic. Use them with whatever you already have:

```typescript
// fetch
const res = await fetch('/bff/payments', { method: 'POST', body: JSON.stringify(request) });
const data: RecipeResponse<PaymentsPage> = await res.json();

// ky
const data = await ky.post('/bff/payments', { json: request }).json<RecipeResponse<PaymentsPage>>();

// generated SDK
const data = await sdk.bff.execute<RecipeResponse<PaymentsPage>>(request);
```

## Exported Types

| Type | Purpose |
|---|---|
| `RecipeRequest` | The request body sent to `POST /bff/{recipe}` |
| `RecipeResponse<T>` | Typed response — maps ingredient IDs to their response bodies |
| `IngredientResult<T>` | Single ingredient result: `{ status, body }` |
| `IngredientInput` | Single ingredient in a request: `{ id, params, map, body, ... }` |
| `DebugInfo` | Debug details when `debug: true` is requested |

## Documentation

Full docs at [tayyab23.github.io/bff](https://tayyab23.github.io/bff)

## License

MIT
