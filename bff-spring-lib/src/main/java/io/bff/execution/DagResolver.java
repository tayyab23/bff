package io.bff.execution;

import io.bff.model.IngredientInput;

import java.util.*;
import java.util.stream.Collectors;

public class DagResolver {

    public static List<List<String>> resolve(List<IngredientInput> ingredients) {
        Map<String, Set<String>> deps = new HashMap<>();
        for (IngredientInput i : ingredients) {
            Set<String> d = new HashSet<>(FieldMapper.inferDependencies(i));
            if (i.dependsOn != null) d.addAll(i.dependsOn);
            deps.put(i.id, d);
        }

        detectCycle(deps);

        List<List<String>> levels = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        Set<String> remaining = new HashSet<>(deps.keySet());

        while (!remaining.isEmpty()) {
            List<String> level = remaining.stream()
                .filter(id -> resolved.containsAll(deps.get(id)))
                .sorted()
                .collect(Collectors.toList());
            if (level.isEmpty()) throw new IllegalStateException("Cycle detected");
            levels.add(level);
            resolved.addAll(level);
            remaining.removeAll(level);
        }
        return levels;
    }

    private static void detectCycle(Map<String, Set<String>> deps) {
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();
        for (String node : deps.keySet()) {
            if (dfs(node, deps, visited, stack, new ArrayList<>())) {
                throw new IllegalArgumentException("Circular dependency detected");
            }
        }
    }

    private static boolean dfs(String node, Map<String, Set<String>> deps,
                                Set<String> visited, Set<String> stack, List<String> path) {
        if (stack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        stack.add(node);
        for (String dep : deps.getOrDefault(node, Set.of())) {
            if (dfs(dep, deps, visited, stack, path)) return true;
        }
        stack.remove(node);
        return false;
    }
}
