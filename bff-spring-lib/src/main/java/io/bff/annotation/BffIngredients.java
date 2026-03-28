package io.bff.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BffIngredients {
    BffIngredient[] value();
}
