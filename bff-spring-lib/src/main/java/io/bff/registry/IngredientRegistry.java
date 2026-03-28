package io.bff.registry;

import io.bff.annotation.BffIngredient;
import io.bff.annotation.BffIngredients;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.*;

public class IngredientRegistry {

    private final Map<String, List<IngredientMetadata>> recipeMap = new HashMap<>();

    public IngredientRegistry(ApplicationContext ctx) {
        ctx.getBeansOfType(Object.class).values().forEach(bean -> {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                List<BffIngredient> annotations = new ArrayList<>();
                if (method.isAnnotationPresent(BffIngredients.class)) {
                    annotations.addAll(Arrays.asList(method.getAnnotation(BffIngredients.class).value()));
                } else if (method.isAnnotationPresent(BffIngredient.class)) {
                    annotations.add(method.getAnnotation(BffIngredient.class));
                }
                for (BffIngredient ann : annotations) {
                    IngredientMetadata meta = IngredientMetadata.from(method, ann, bean);
                    String[] recipes = ann.recipe().length == 0 ? new String[]{""} : ann.recipe();
                    for (String recipe : recipes) {
                        recipeMap.computeIfAbsent(recipe, k -> new ArrayList<>()).add(meta);
                    }
                }
            }
        });
        validateNoDuplicates();
    }

    public List<IngredientMetadata> getIngredients(String recipe) {
        return recipeMap.getOrDefault(recipe, Collections.emptyList());
    }

    public Map<String, List<IngredientMetadata>> getAll() {
        return Collections.unmodifiableMap(recipeMap);
    }

    private void validateNoDuplicates() {
        recipeMap.forEach((recipe, ingredients) -> {
            Set<String> seen = new HashSet<>();
            for (IngredientMetadata m : ingredients) {
                if (!seen.add(m.name())) {
                    throw new IllegalStateException(
                        "Duplicate ingredient name '" + m.name() + "' in recipe '" + recipe + "'");
                }
            }
        });
    }
}
