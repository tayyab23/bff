package io.bff.execution;

import io.bff.model.IngredientInput;

import java.util.*;
import java.util.regex.*;

public class FieldMapper {

    private static final Pattern REF = Pattern.compile("^(\\w+)::(body|header)::\\$\\{([^}]+)\\}$");

    public static List<String> inferDependencies(IngredientInput input) {
        Set<String> deps = new HashSet<>();
        if (input.map == null) return new ArrayList<>();
        collectRefs(input.map.path, deps);
        collectRefs(input.map.query, deps);
        collectRefs(input.map.body, deps);
        return new ArrayList<>(deps);
    }

    private static void collectRefs(Map<String, Object> map, Set<String> deps) {
        if (map == null) return;
        map.values().forEach(v -> {
            if (v instanceof String s && s.contains("::")) {
                Matcher m = REF.matcher(s);
                if (m.matches()) deps.add(m.group(1));
            }
        });
    }

    public static Object resolve(Object value, Map<String, io.bff.model.IngredientResult> completed) {
        if (!(value instanceof String s) || !s.contains("::")) return value;
        Matcher m = REF.matcher(s);
        if (!m.matches()) return value;
        String ingredientName = m.group(1);
        String location = m.group(2);
        String fieldPath = m.group(3);
        io.bff.model.IngredientResult result = completed.get(ingredientName);
        if (result == null) return null;
        Object source = "body".equals(location) ? result.body : null;
        return extractPath(source, fieldPath);
    }

    @SuppressWarnings("unchecked")
    private static Object extractPath(Object obj, String path) {
        String[] parts = path.split("\\.");
        Object current = obj;
        for (String part : parts) {
            if (current == null) return null;
            int idx = -1;
            String key = part;
            if (part.contains("[")) {
                key = part.substring(0, part.indexOf('['));
                idx = Integer.parseInt(part.replaceAll(".*\\[(\\d+)\\].*", "$1"));
            }
            if (current instanceof Map<?, ?> map) current = map.get(key);
            if (idx >= 0 && current instanceof List<?> list) current = list.get(idx);
        }
        return current;
    }
}
