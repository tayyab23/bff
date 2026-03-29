// Type-level verification — this file must compile with zero errors.
// If the generics are wrong, `tsc` will catch it here.

import {
  createBffClient,
  RecipeRequest,
  RecipeResponse,
  IngredientResult,
} from './index';

// ── Simulated user types (from OpenAPI codegen, Smithy, or hand-written) ──

interface Account {
  accountId: string;
  billingGroupId: string;
  customerId: string;
  plan: 'FREE' | 'PRO' | 'ENTERPRISE';
}

interface InvoiceList {
  items: { id: string; amount: number; status: 'UNPAID' | 'PAID' | 'OVERDUE' }[];
  total: number;
  currency: string;
}

interface PaymentMethodList {
  methods: { id: string; type: 'CARD' | 'BANK_ACCOUNT'; last4: string }[];
}

// ── 1. createBffClient — typed response ──

async function testClient() {
  const bff = createBffClient('/bff');

  const res = await bff.execute<{
    getAccount: Account;
    getInvoices: InvoiceList;
    getPaymentMethods: PaymentMethodList;
  }>('payments', {
    ingredients: [
      { id: 'getAccount', params: { accountId: 'acc-123' } },
      { id: 'getInvoices', map: { query: { billingGroupId: 'getAccount::body::${billingGroupId}' } } },
      { id: 'getPaymentMethods', map: { query: { customerId: 'getAccount::body::${customerId}' } } },
    ],
  });

  // Proves the generic maps ingredient ID → typed body
  const accountId: string = res.results.getAccount.body.accountId;
  const plan: 'FREE' | 'PRO' | 'ENTERPRISE' = res.results.getAccount.body.plan;
  const invoiceTotal: number = res.results.getInvoices.body.total;
  const methodType: 'CARD' | 'BANK_ACCOUNT' = res.results.getPaymentMethods.body.methods[0].type;
  const status: number = res.results.getAccount.status;
  const executionOrder: (string | string[])[] = res.executionOrder;

  // @ts-expect-error — 'nonExistent' is not a key in the result map
  const bad1 = res.results.nonExistent;

  // @ts-expect-error — 'plan' is not a number
  const bad2: number = res.results.getAccount.body.plan;
}

// ── 2. RecipeResponse standalone (bring your own client) ──

async function testBYOClient() {
  type PaymentsResult = { getAccount: Account; getInvoices: InvoiceList };

  // Simulating: const { data } = await axios.post<RecipeResponse<PaymentsResult>>(...)
  const data: RecipeResponse<PaymentsResult> = {} as any;

  const accountId: string = data.results.getAccount.body.accountId;
  const items = data.results.getInvoices.body.items;

  // @ts-expect-error — 'getPaymentMethods' not in PaymentsResult
  const bad = data.results.getPaymentMethods;
}

// ── 3. IngredientResult individually ──

function testIngredientResult() {
  const result: IngredientResult<Account> = {} as any;
  const id: string = result.body.accountId;

  // @ts-expect-error — 'foo' doesn't exist on Account
  const bad = result.body.foo;
}

// ── 4. RecipeRequest structure ──

const validRequest: RecipeRequest = {
  ingredients: [
    { id: 'getAccount', params: { accountId: 'acc-123' } },
    { id: 'getInvoices', map: { query: { billingGroupId: 'getAccount::body::${billingGroupId}' } }, dependsOn: ['getAccount'] },
    { id: 'submitPayment', body: { amount: 100 }, map: { body: { accountId: 'getAccount::body::${accountId}' } }, headers: { custom: { 'X-Idempotency-Key': 'idem-123' } } },
  ],
  debug: true,
  failFast: false,
  headers: { forward: true, forwardOnly: ['Authorization'] },
};
