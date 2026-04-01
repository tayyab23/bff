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
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("method", m.httpMethod());
            entry.put("path", m.path());
            entry.put("responseType", m.responseType().getSimpleName());
            if (m.isExternal()) entry.put("proxyUrl", m.proxyUrl());
            ingredientMap.put(m.name(), entry);
        }
        result.put("ingredients", ingredientMap);
        return result;
    }
}
