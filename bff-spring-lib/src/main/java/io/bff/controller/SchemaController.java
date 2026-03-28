package io.bff.controller;

import io.bff.registry.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class SchemaController {

    private final IngredientRegistry registry;

    public SchemaController(IngredientRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("${bff-recipe.base-path:/bff}/{recipe}/schema")
    public Map<String, Object> schema(@PathVariable String recipe) {
        List<IngredientMetadata> ingredients = registry.getIngredients(recipe);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recipe", recipe);
        Map<String, Object> ingredientMap = new LinkedHashMap<>();
        for (IngredientMetadata m : ingredients) {
            ingredientMap.put(m.name(), Map.of(
                "method", m.httpMethod(),
                "path", m.path(),
                "responseType", m.responseType().getSimpleName()
            ));
        }
        result.put("ingredients", ingredientMap);
        return result;
    }
}
