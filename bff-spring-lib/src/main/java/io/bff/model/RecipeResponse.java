package io.bff.model;

import java.util.Map;
import java.util.List;

public class RecipeResponse {
    public Map<String, IngredientResult> results;
    public List<Object> executionOrder;
    public List<String> errors;
}
