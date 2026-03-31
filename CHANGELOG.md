# Changelog

All notable changes to this project will be documented in this file.

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
