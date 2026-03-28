package io.bff.validation;

import io.bff.model.*;
import io.bff.registry.IngredientRegistry;

import java.util.*;
import java.util.stream.Collectors;

public class RecipeValidator {

    private final IngredientRegistry registry;
    private final int maxIngredients;

    public RecipeValidator(IngredientRegistry registry, int maxIngredients) {
        this.registry = registry;
        this.maxIngredients = maxIngredients;
    }

    public List<String> validate(String recipe, Recipe request) {
        List<String> errors = new ArrayList<>();
        if (request.ingredients == null || request.ingredients.isEmpty()) {
            errors.add("No ingredients provided");
            return errors;
        }
        if (request.ingredients.size() > maxIngredients) {
            errors.add("MaxIngredientsExceeded: recipe contains " + request.ingredients.size() + " ingredients, maximum allowed is " + maxIngredients);
        }
        Set<String> known = registry.getIngredients(recipe).stream()
            .map(m -> m.name()).collect(Collectors.toSet());
        Set<String> requested = request.ingredients.stream()
            .map(i -> i.id).collect(Collectors.toSet());

        for (String id : requested) {
            if (!known.contains(id)) errors.add("Unknown ingredient: " + id);
        }
        return errors;
    }
}
