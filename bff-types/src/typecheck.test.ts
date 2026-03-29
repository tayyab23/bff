// Type-level verification — this file must compile with zero errors.
// If the generics are wrong, `tsc` will catch it here.

import type {
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

// ── 1. RecipeResponse with Axios / fetch / any HTTP client ──

function testTypedResponse() {
  type PaymentsResult = {
    getAccount: Account;
    getInvoices: InvoiceList;
    getPaymentMethods: PaymentMethodList;
  };

  // Simulating: const { data } = await axios.post<RecipeResponse<PaymentsResult>>(...)
  const data: RecipeResponse<PaymentsResult> = {} as any;

  // These must compile — proves the generic maps ingredient ID → typed body
  const accountId: string = data.results.getAccount.body.accountId;
  const plan: 'FREE' | 'PRO' | 'ENTERPRISE' = data.results.getAccount.body.plan;
  const invoiceTotal: number = data.results.getInvoices.body.total;
  const currency: string = data.results.getInvoices.body.currency;
  const firstInvoiceAmount: number = data.results.getInvoices.body.items[0].amount;
  const methodType: 'CARD' | 'BANK_ACCOUNT' = data.results.getPaymentMethods.body.methods[0].type;
  const status: number = data.results.getAccount.status;
  const executionOrder: (string | string[])[] = data.executionOrder;

  // @ts-expect-error — 'nonExistent' is not a key in the result map
  const bad1 = data.results.nonExistent;

  // @ts-expect-error — 'plan' is not a number
  const bad2: number = data.results.getAccount.body.plan;
}

// ── 2. Subset of ingredients ──

function testSubset() {
  type DashboardResult = { getAccount: Account; getInvoices: InvoiceList };

  const data: RecipeResponse<DashboardResult> = {} as any;

  const accountId: string = data.results.getAccount.body.accountId;
  const items = data.results.getInvoices.body.items;

  // @ts-expect-error — 'getPaymentMethods' not in DashboardResult
  const bad = data.results.getPaymentMethods;
}

// ── 3. IngredientResult individually ──

function testIngredientResult() {
  const result: IngredientResult<Account> = {} as any;
  const id: string = result.body.accountId;
  const status: number = result.status;

  // @ts-expect-error — 'foo' doesn't exist on Account
  const bad = result.body.foo;
}

// ── 4. RecipeRequest is structurally valid ──

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
