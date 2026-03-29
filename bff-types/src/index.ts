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
