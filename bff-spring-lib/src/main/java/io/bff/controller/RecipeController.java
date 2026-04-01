package io.bff.controller;

import io.bff.execution.RecipeExecutor;
import io.bff.model.*;
import io.bff.registry.IngredientRegistry;
import io.bff.validation.RecipeValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class RecipeController {

    private final IngredientRegistry registry;
    private final RecipeExecutor executor;
    private final RecipeValidator validator;

    public RecipeController(IngredientRegistry registry, RecipeExecutor executor,
                            RecipeValidator validator) {
        this.registry = registry;
        this.executor = executor;
        this.validator = validator;
    }

    @PostMapping("${bff-recipe.base-path:/bff}")
    public ResponseEntity<RecipeResponse> executeDefault(
            @RequestBody Recipe request, HttpServletRequest httpRequest) {
        return doExecute("", request, httpRequest);
    }

    @PostMapping("${bff-recipe.base-path:/bff}/{recipe}")
    public ResponseEntity<RecipeResponse> execute(
            @PathVariable String recipe, @RequestBody Recipe request,
            HttpServletRequest httpRequest) {
        return doExecute(recipe, request, httpRequest);
    }

    private ResponseEntity<RecipeResponse> doExecute(String recipe, Recipe request,
                                                      HttpServletRequest httpRequest) {
        var errors = validator.validate(recipe, request);
        if (!errors.isEmpty()) {
            var response = new RecipeResponse();
            response.results = Map.of();
            response.executionOrder = java.util.List.of();
            response.errors = errors;
            return ResponseEntity.badRequest().body(response);
        }

        RecipeResponse response = executor.execute(recipe, request, httpRequest);
        boolean anyFailed = response.results.values().stream().anyMatch(r -> r.status >= 400);
        int status = anyFailed ? 207 : 200;
        return ResponseEntity.status(status).body(response);
    }
}
