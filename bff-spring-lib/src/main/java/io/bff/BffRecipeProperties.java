package io.bff;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bff-recipe")
public class BffRecipeProperties {
    private boolean enabled = true;
    private String basePath = "/bff";
    private Schema schema = new Schema();
    private Validate validate = new Validate();
    private Debug debug = new Debug();
    private Execution execution = new Execution();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }
    public Schema getSchema() { return schema; }
    public Validate getValidate() { return validate; }
    public Debug getDebug() { return debug; }
    public Execution getExecution() { return execution; }

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
