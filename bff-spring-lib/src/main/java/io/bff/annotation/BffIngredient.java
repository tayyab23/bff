package io.bff.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(BffIngredients.class)
public @interface BffIngredient {
    String[] recipe() default {};
    String name() default "";
    String[] forwardHeaders() default {"INHERIT"};
    String[] customHeaders() default {"INHERIT"};
    String[] headerMapping() default {"INHERIT"};
}
