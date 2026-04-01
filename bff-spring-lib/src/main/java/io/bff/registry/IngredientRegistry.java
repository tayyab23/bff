package io.bff.registry;

import io.bff.BffRecipeProperties;
import io.bff.annotation.BffIngredient;
import io.bff.annotation.BffIngredients;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.*;

public class IngredientRegistry {

    private final Map<String, List<IngredientMetadata>> recipeMap = new HashMap<>();

    public IngredientRegistry(ApplicationContext ctx, BffRecipeProperties props) {
        if (props.getMode() == BffRecipeProperties.Mode.ANNOTATION) {
            discoverFromAnnotations(ctx);
        } else {
            loadFromConfig(props);
        }
        validateNoDuplicates();
    }

    private void discoverFromAnnotations(ApplicationContext ctx) {
        // Only scan @Controller/@RestController beans — not every bean in the context
        Map<String, Object> controllers = new HashMap<>();
        controllers.putAll(ctx.getBeansWithAnnotation(RestController.class));
        controllers.putAll(ctx.getBeansWithAnnotation(Controller.class));
        controllers.values().forEach(bean -> {
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
    }

    private void loadFromConfig(BffRecipeProperties props) {
        Map<String, BffRecipeProperties.IngredientDef> ingredientDefs = new HashMap<>(props.getIngredients());

        // Validate all ingredient definitions upfront
        ingredientDefs.forEach((name, def) -> {
            if (def.getPath() == null || def.getPath().isBlank()) {
                throw new IllegalStateException("Ingredient '" + name + "' must have a path");
            }
        });

        props.getRecipes().forEach((recipeName, recipeDef) -> {
            String recipeProxyUrl = recipeDef.getProxyUrl();
            List<IngredientMetadata> list = new ArrayList<>();
            for (String ingredientName : recipeDef.getIngredients()) {
                BffRecipeProperties.IngredientDef def = ingredientDefs.get(ingredientName);
                if (def == null) {
                    throw new IllegalStateException(
                        "Recipe '" + recipeName + "' references unknown ingredient '" + ingredientName + "'");
                }
                list.add(IngredientMetadata.fromConfig(ingredientName, def, recipeProxyUrl));
            }
            recipeMap.put(recipeName, list);
        });
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
