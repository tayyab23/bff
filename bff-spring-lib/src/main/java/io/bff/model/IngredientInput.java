package io.bff.model;

import java.util.List;
import java.util.Map;

public class IngredientInput {
    public String id;
    public Map<String, Object> params;
    public Object body;
    public MapBlock map;
    public List<String> dependsOn;
    public IngredientHeaderConfig headers;

    public static class MapBlock {
        public Map<String, Object> path;
        public Map<String, Object> query;
        public Map<String, Object> body;
    }
}
