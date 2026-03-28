package io.bff.model;

import java.util.List;
import java.util.Map;

public class IngredientHeaderConfig {
    public Boolean forward;
    public List<String> forwardOnly;
    public Map<String, String> custom;
    public Map<String, String> mappings;
}
