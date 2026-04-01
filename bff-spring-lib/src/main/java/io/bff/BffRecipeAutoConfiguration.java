package io.bff;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bff.controller.*;
import io.bff.execution.*;
import io.bff.registry.IngredientRegistry;
import io.bff.validation.RecipeValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.*;

@AutoConfiguration
@ConditionalOnProperty(prefix = "bff-recipe", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(BffRecipeProperties.class)
public class BffRecipeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IngredientRegistry ingredientRegistry(ApplicationContext ctx, BffRecipeProperties props) {
        return new IngredientRegistry(ctx, props);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestClient bffRestClient() {
        return RestClient.create();
    }

    @Bean
    @ConditionalOnMissingBean
    public IngredientDispatcher ingredientDispatcher(RequestMappingHandlerMapping mapping,
                                                     RequestMappingHandlerAdapter adapter,
                                                     ObjectMapper objectMapper,
                                                     RestClient bffRestClient) {
        return new IngredientDispatcher(mapping, adapter, objectMapper, bffRestClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public RecipeExecutor recipeExecutor(IngredientRegistry registry,
                                         IngredientDispatcher dispatcher,
                                         BffRecipeProperties props) {
        return new RecipeExecutor(registry, dispatcher, props.getExecution());
    }

    @Bean
    @ConditionalOnMissingBean
    public RecipeValidator recipeValidator(IngredientRegistry registry, BffRecipeProperties props) {
        return new RecipeValidator(registry, props.getExecution().getMaxIngredients());
    }

    @Bean
    @ConditionalOnMissingBean
    public RecipeController recipeController(IngredientRegistry registry, RecipeExecutor executor,
                                              RecipeValidator validator) {
        return new RecipeController(registry, executor, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "bff-recipe.schema", name = "enabled", havingValue = "true")
    public SchemaController schemaController(IngredientRegistry registry) {
        return new SchemaController(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "bff-recipe.validate", name = "enabled", havingValue = "true")
    public ValidateController validateController(IngredientRegistry registry, RecipeValidator validator) {
        return new ValidateController(registry, validator);
    }
}
