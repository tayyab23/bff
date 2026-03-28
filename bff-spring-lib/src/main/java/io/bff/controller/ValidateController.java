package io.bff.controller;

import io.bff.model.Recipe;
import io.bff.registry.IngredientRegistry;
import io.bff.validation.RecipeValidator;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class ValidateController {

    private final IngredientRegistry registry;
    private final RecipeValidator validator;

    public ValidateController(IngredientRegistry registry, RecipeValidator validator) {
        this.registry = registry;
        this.validator = validator;
    }

    @PostMapping("${bff-recipe.base-path:/bff}/{recipe}/validate")
    public Map<String, Object> validate(@PathVariable String recipe, @RequestBody Recipe request) {
        List<String> errors = validator.validate(recipe, request);
        if (errors.isEmpty()) {
            return Map.of("valid", true);
        }
        List<Map<String, String>> errorList = errors.stream()
            .map(e -> Map.of("error", e))
            .toList();
        return Map.of("valid", false, "errors", errorList);
    }
}
