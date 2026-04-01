package io.bff.execution;

import io.bff.BffRecipeProperties;
import io.bff.model.*;
import io.bff.registry.IngredientMetadata;
import io.bff.registry.IngredientRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RecipeExecutor implements DisposableBean {

    private final IngredientRegistry registry;
    private final IngredientDispatcher dispatcher;
    private final ExecutorService pool;
    private final long ingredientTimeoutMs;
    private final long recipeTimeoutMs;

    public RecipeExecutor(IngredientRegistry registry, IngredientDispatcher dispatcher,
                          BffRecipeProperties.Execution config) {
        this.registry = registry;
        this.dispatcher = dispatcher;
        this.pool = Executors.newFixedThreadPool(config.getParallelThreads());
        this.ingredientTimeoutMs = config.getIngredientTimeoutMs();
        this.recipeTimeoutMs = config.getRecipeTimeoutMs();
    }

    public RecipeResponse execute(String recipeName, Recipe recipe,
                                   jakarta.servlet.http.HttpServletRequest originalRequest) {
        Map<String, IngredientMetadata> metaMap = registry.getIngredients(recipeName)
            .stream().collect(Collectors.toMap(IngredientMetadata::name, m -> m));

        Map<String, IngredientInput> inputMap = recipe.ingredients.stream()
            .collect(Collectors.toMap(i -> i.id, i -> i));

        List<List<String>> levels = DagResolver.resolve(recipe.ingredients);

        // Capture SecurityContext on the request thread — pool threads don't inherit it
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Map<String, IngredientResult> results = new ConcurrentHashMap<>();
        List<Object> executionOrder = new ArrayList<>();
        Set<String> failed = new HashSet<>();
        long deadline = System.currentTimeMillis() + recipeTimeoutMs;

        for (List<String> level : levels) {
            if (System.currentTimeMillis() >= deadline) {
                level.forEach(id -> results.put(id, new IngredientResult(504,
                    Map.of("error", "RecipeTimeout", "message", "Recipe timeout exceeded"))));
                break;
            }

            List<String> runnable = level.stream()
                .filter(id -> !hasFailed(id, inputMap, failed))
                .collect(Collectors.toList());

            level.stream()
                .filter(id -> hasFailed(id, inputMap, failed))
                .forEach(id -> {
                    String dep = getFailedDep(id, inputMap, failed);
                    results.put(id, new IngredientResult(422,
                        Map.of("error", "DependencyFailed", "message", "Skipped: dependency '" + dep + "' failed")));
                    failed.add(id);
                });

            if (runnable.isEmpty()) continue;
            executionOrder.add(runnable.size() == 1 ? runnable.get(0) : runnable);

            List<Future<Map.Entry<String, IngredientResult>>> futures = runnable.stream().map(id ->
                pool.submit(() -> {
                    SecurityContextHolder.setContext(securityContext);
                    try {
                        IngredientMetadata meta = metaMap.get(id);
                        IngredientInput input = inputMap.get(id);
                        IngredientResult result = dispatcher.dispatch(meta, input, results, originalRequest);
                        return Map.entry(id, result);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                })
            ).collect(Collectors.toList());

            for (Future<Map.Entry<String, IngredientResult>> f : futures) {
                try {
                    long remaining = Math.min(ingredientTimeoutMs, deadline - System.currentTimeMillis());
                    Map.Entry<String, IngredientResult> entry = f.get(Math.max(remaining, 1), TimeUnit.MILLISECONDS);
                    results.put(entry.getKey(), entry.getValue());
                    if (entry.getValue().status >= 400) {
                        failed.add(entry.getKey());
                        if (recipe.failFast) { cancelRemaining(futures); break; }
                    }
                } catch (TimeoutException e) {
                    String id = runnable.get(futures.indexOf(f));
                    results.put(id, new IngredientResult(504, Map.of("error", "IngredientTimeout", "message", "Timed out after " + ingredientTimeoutMs + "ms")));
                    failed.add(id);
                    f.cancel(true);
                } catch (Exception e) {
                    String id = runnable.get(futures.indexOf(f));
                    results.put(id, new IngredientResult(500, Map.of("error", e.getMessage())));
                    failed.add(id);
                }
            }
        }

        RecipeResponse response = new RecipeResponse();
        response.results = results;
        response.executionOrder = executionOrder;
        return response;
    }

    private boolean hasFailed(String id, Map<String, IngredientInput> inputMap, Set<String> failed) {
        IngredientInput input = inputMap.get(id);
        if (input == null) return false;
        List<String> deps = FieldMapper.inferDependencies(input);
        if (input.dependsOn != null) deps.addAll(input.dependsOn);
        return deps.stream().anyMatch(failed::contains);
    }

    private String getFailedDep(String id, Map<String, IngredientInput> inputMap, Set<String> failed) {
        IngredientInput input = inputMap.get(id);
        List<String> deps = FieldMapper.inferDependencies(input);
        if (input.dependsOn != null) deps.addAll(input.dependsOn);
        return deps.stream().filter(failed::contains).findFirst().orElse("unknown");
    }

    private void cancelRemaining(List<? extends Future<?>> futures) {
        futures.forEach(f -> f.cancel(true));
    }

    @Override
    public void destroy() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
