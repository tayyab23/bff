# Changelog

All notable changes to this project will be documented in this file.

## [1.2.0] — 2026-04-01

### Added
- Proxy dispatch: `proxy-url` on ingredients and recipes for external service aggregation
- Recipe-level `proxy-url` as shared base URL, ingredient-level override
- `proxy-url` supports Spring property placeholders (`${ENV_VAR:default}`)
- Sequential execution mode: `parallel-threads: 0` disables the thread pool
- Validation errors now included in 400 response body (`response.errors`)
- 404 response for unknown recipe names (was 400)
- Cycle detection error messages include the full cycle path

### Fixed
- Thread safety: results map now uses ConcurrentHashMap (was LinkedHashMap read from pool threads)
- SecurityContext propagation: captured on request thread, set/cleared on pool threads
- URL encoding: proxy query params properly encoded via UriComponentsBuilder
- RestClient bean qualified to avoid collision with user-defined RestClient beans
- Annotation scan narrowed to @Controller/@RestController (was scanning all beans)
- Config-mode path validation restored (lost during proxy-url refactor)
- DAG cycle exceptions caught as 400 (was bubbling as 500)
- `::header::` expressions now throw clear error (was silently returning null)
- Thread pool shut down on context close via DisposableBean
- Schema endpoint returns 404 for unknown recipes (was 200 with empty ingredients)

### Removed
- Dead `DebugInfo` class (was never populated)
- Dead `IngredientResult.type` field (was never set)

## [1.1.0] — 2026-03-30

### Added
- Config mode: define ingredients and recipes entirely in `application.yml` — no annotations needed
- `mode: annotation` (default) or `mode: config` in `bff-recipe` properties
- Config-defined ingredients with `method` and `path`
- Config-defined recipes with ingredient lists and per-recipe timeout overrides
- Startup validation: missing paths and unknown ingredient references fail fast

## [1.0.0] — 2026-03-28

### Added
- `@BffIngredient` annotation with multi-recipe support, header policies (INHERIT/wildcard/list/regex)
- Zero-config auto-configuration (activates on classpath, no yml required)
- `POST /bff/{recipe}` — recipe execution with DAG resolution and parallel dispatch
- `GET /bff/{recipe}/schema` — ingredient discovery (opt-in)
- `POST /bff/{recipe}/validate` — dry-run validation (opt-in)
- Array operators: `[*]`, `[n]`, `[-n]`, `[start:end]`, `[?filter]`, `[?in()]`, `[?==REG()]`, `[?exists]`, `[?missing]`, compound `&&` and `||`, nested field filters
- `BffRecipeProperties` — typed configuration with nested classes
- Per-recipe config overrides in yml
- Header system: forward, custom, mapping with global blocklist enforcement
- Debug mode with header masking
- `ingredient-timeout-ms` and `recipe-timeout-ms` with per-recipe overrides
- SecurityContext propagation (always on)
- In-process dispatch through full Spring MVC chain
- `@bff-recipe/types` npm package — zero-runtime TypeScript type definitions
- Example app with 28 integration tests and OpenAPI codegen
- Documentation site at tayyab23.github.io/bff
- Anti-patterns guide with SSR guidance

## [0.1.0] — 2026-03-28

### Added
- Initial publish to Maven Central (missing `-parameters` compiler flag)

### Known Issues
- Compiled without `-parameters` — `@PathVariable` without explicit names fails at runtime. Fixed in 1.0.0.
