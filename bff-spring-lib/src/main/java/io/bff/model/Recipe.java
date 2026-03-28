package io.bff.model;

import java.util.List;
import java.util.Map;

public class Recipe {
    public List<IngredientInput> ingredients;
    public boolean debug = false;
    public boolean failFast = false;
    public RecipeHeaders headers;

    public static class RecipeHeaders {
        public boolean forward = true;
        public List<String> forwardOnly;
    }
}
