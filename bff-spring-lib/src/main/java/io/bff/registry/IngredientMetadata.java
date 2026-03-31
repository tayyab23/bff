package io.bff.registry;

import io.bff.BffRecipeProperties;
import io.bff.annotation.BffIngredient;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

public record IngredientMetadata(
    String name,
    String httpMethod,
    String path,
    Method method,
    Object bean,
    Class<?> responseType,
    BffIngredient annotation
) {
    public static IngredientMetadata from(Method method, BffIngredient ann, Object bean) {
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        String httpMethod = resolveHttpMethod(method);
        String path = resolvePath(method);
        Class<?> responseType = method.getReturnType();
        return new IngredientMetadata(name, httpMethod, path, method, bean, responseType, ann);
    }

    public static IngredientMetadata fromConfig(String name, BffRecipeProperties.IngredientDef def) {
        return new IngredientMetadata(name, def.getMethod().toUpperCase(), def.getPath(),
                null, null, Object.class, null);
    }

    private static String resolveHttpMethod(Method m) {
        if (m.isAnnotationPresent(GetMapping.class)) return "GET";
        if (m.isAnnotationPresent(PostMapping.class)) return "POST";
        if (m.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (m.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (m.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        RequestMapping rm = m.getAnnotation(RequestMapping.class);
        if (rm != null && rm.method().length > 0) return rm.method()[0].name();
        throw new IllegalStateException("No request mapping on method: " + m.getName());
    }

    private static String resolvePath(Method m) {
        GetMapping g = m.getAnnotation(GetMapping.class);
        if (g != null && g.value().length > 0) return g.value()[0];
        PostMapping p = m.getAnnotation(PostMapping.class);
        if (p != null && p.value().length > 0) return p.value()[0];
        PutMapping pu = m.getAnnotation(PutMapping.class);
        if (pu != null && pu.value().length > 0) return pu.value()[0];
        DeleteMapping d = m.getAnnotation(DeleteMapping.class);
        if (d != null && d.value().length > 0) return d.value()[0];
        PatchMapping pa = m.getAnnotation(PatchMapping.class);
        if (pa != null && pa.value().length > 0) return pa.value()[0];
        RequestMapping rm = m.getAnnotation(RequestMapping.class);
        if (rm != null && rm.value().length > 0) return rm.value()[0];
        return "/";
    }
}
