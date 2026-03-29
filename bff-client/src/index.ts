export interface IngredientResult<T> {
  status: number;
  body: T;
  type?: string;
}

export type RecipeResponse<T extends Record<string, unknown>> = {
  executionOrder: (string | string[])[];
  results: { [K in keyof T]: IngredientResult<T[K]> };
  debug?: Record<string, DebugInfo>;
};

export interface DebugInfo {
  resolvedRequest: {
    method: string;
    path: string;
    headers: { applied: Record<string, string>; stripped: Record<string, string> };
  };
  resolvedResponse: { headers: Record<string, string> };
  durationMs: number;
}

export interface IngredientInput {
  id: string;
  params?: Record<string, unknown>;
  body?: unknown;
  map?: {
    path?: Record<string, unknown>;
    query?: Record<string, unknown>;
    body?: Record<string, unknown>;
  };
  dependsOn?: string[];
  headers?: {
    forward?: boolean;
    forwardOnly?: string[];
    custom?: Record<string, string>;
    mappings?: Record<string, string>;
  };
}

export interface RecipeRequest {
  ingredients: IngredientInput[];
  debug?: boolean;
  failFast?: boolean;
  headers?: { forward?: boolean; forwardOnly?: string[] };
}

export function createBffClient(baseUrl: string) {
  return {
    async execute<T extends Record<string, unknown>>(
      recipe: string,
      request: RecipeRequest
    ): Promise<RecipeResponse<T>> {
      const res = await fetch(`${baseUrl}/${recipe}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      });
      if (!res.ok && res.status !== 207) throw new Error(`BFF error: ${res.status}`);
      return res.json();
    },
  };
}
