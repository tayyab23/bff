package io.bff.execution;

import io.bff.model.IngredientInput;

import java.util.*;
import java.util.regex.*;

public class FieldMapper {

    private static final Pattern REF = Pattern.compile("^(\\w+)::(body|header)::\\$\\{([^}]+)\\}$");
    private static final Pattern BRACKET = Pattern.compile("\\[[^]]+]");

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
    static Object extractPath(Object obj, String path) {
        List<String> segments = splitPath(path);
        Object current = obj;
        for (int i = 0; i < segments.size(); i++) {
            if (current == null) return null;
            String seg = segments.get(i);
            Matcher bm = BRACKET.matcher(seg);
            String key = bm.find() ? seg.substring(0, bm.start()) : seg;
            if (!key.isEmpty() && current instanceof Map<?, ?> map) current = map.get(key);
            bm.reset(seg);
            if (bm.find() && current instanceof List<?> list) {
                ArrayOperator op = ArrayOperator.parse(bm.group());
                Object result = op.apply(list);
                if (result instanceof List<?> collected && i + 1 < segments.size()) {
                    String remaining = String.join(".", segments.subList(i + 1, segments.size()));
                    return collected.stream()
                            .map(item -> extractPath(item, remaining))
                            .filter(Objects::nonNull)
                            .toList();
                }
                current = result;
            }
        }
        return current;
    }

    private static List<String> splitPath(String path) {
        List<String> parts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (char c : path.toCharArray()) {
            if (c == '[') depth++;
            if (c == ']') depth--;
            if (c == '.' && depth == 0) { parts.add(sb.toString()); sb.setLength(0); }
            else sb.append(c);
        }
        if (!sb.isEmpty()) parts.add(sb.toString());
        return parts;
    }
}
