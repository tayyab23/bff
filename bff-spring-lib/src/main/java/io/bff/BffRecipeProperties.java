package io.bff;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@ConfigurationProperties(prefix = "bff-recipe")
public class BffRecipeProperties {
    private boolean enabled = true;
    private String basePath = "/bff";
    private Mode mode = Mode.ANNOTATION;
    private Schema schema = new Schema();
    private Validate validate = new Validate();
    private Debug debug = new Debug();
    private Execution execution = new Execution();
    private Map<String, IngredientDef> ingredients = new HashMap<>();
    private Map<String, RecipeDef> recipes = new HashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public Schema getSchema() { return schema; }
    public Validate getValidate() { return validate; }
    public Debug getDebug() { return debug; }
    public Execution getExecution() { return execution; }
    public Map<String, IngredientDef> getIngredients() { return ingredients; }
    public void setIngredients(Map<String, IngredientDef> ingredients) { this.ingredients = ingredients; }
    public Map<String, RecipeDef> getRecipes() { return recipes; }
    public void setRecipes(Map<String, RecipeDef> recipes) { this.recipes = recipes; }

    public enum Mode { ANNOTATION, CONFIG }

    public static class IngredientDef {
        private String method = "GET";
        private String path;

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class RecipeDef {
        private List<String> ingredients = List.of();
        private Long ingredientTimeoutMs;
        private Long recipeTimeoutMs;

        public List<String> getIngredients() { return ingredients; }
        public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
        public Long getIngredientTimeoutMs() { return ingredientTimeoutMs; }
        public void setIngredientTimeoutMs(Long ingredientTimeoutMs) { this.ingredientTimeoutMs = ingredientTimeoutMs; }
        public Long getRecipeTimeoutMs() { return recipeTimeoutMs; }
        public void setRecipeTimeoutMs(Long recipeTimeoutMs) { this.recipeTimeoutMs = recipeTimeoutMs; }
    }

    public static class Schema {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Validate {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Debug {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Execution {
        private int parallelThreads = 10;
        private long ingredientTimeoutMs = 5000;
        private long recipeTimeoutMs = 15000;
        private int maxIngredients = 10;

        public int getParallelThreads() { return parallelThreads; }
        public void setParallelThreads(int parallelThreads) { this.parallelThreads = parallelThreads; }
        public long getIngredientTimeoutMs() { return ingredientTimeoutMs; }
        public void setIngredientTimeoutMs(long ingredientTimeoutMs) { this.ingredientTimeoutMs = ingredientTimeoutMs; }
        public long getRecipeTimeoutMs() { return recipeTimeoutMs; }
        public void setRecipeTimeoutMs(long recipeTimeoutMs) { this.recipeTimeoutMs = recipeTimeoutMs; }
        public int getMaxIngredients() { return maxIngredients; }
        public void setMaxIngredients(int maxIngredients) { this.maxIngredients = maxIngredients; }
    }
}
